package eu.apps4net.feedora.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Parsing statistics of a feed. They are updated on every check of the feed and decide when it's checked next:
 * busy feeds are checked often, quiet feeds rarely.
 */
@Entity
public class FeedStatistics {
    // Check intervals, from the posting frequency of the feed
    public static final int VERY_BUSY_INTERVAL_MINUTES = 1;     // more than VERY_BUSY_POSTS_PER_HOUR posts per hour
    public static final int BUSY_INTERVAL_MINUTES = 5;          // more than BUSY_POSTS_PER_HOUR posts per hour
    public static final int FREQUENT_INTERVAL_MINUTES = 10;     // more than 1 post per hour
    public static final int DAILY_INTERVAL_MINUTES = 60;        // more than 1 post per day
    public static final int RARE_INTERVAL_MINUTES = 6 * 60;     // up to 1 post per day
    public static final int VERY_BUSY_POSTS_PER_HOUR = 30;
    public static final int BUSY_POSTS_PER_HOUR = 10;
    // After this many failed checks in a row, the feed is checked as rarely as possible
    public static final int MAX_CONSECUTIVE_ERRORS = 3;
    // Only the posts of the last days count for the frequency
    public static final int FREQUENCY_WINDOW_DAYS = 7;
    // Only the posts of the last hours count for the posts per hour
    public static final int HOURLY_WINDOW_HOURS = 3;

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne
    @JoinColumn(name = "feed_id", unique = true, nullable = false)
    @JsonIgnore
    private Feed feed;

    // Last check of the feed, successful or not
    private LocalDateTime lastCheckedAt;
    // Last successful check
    private LocalDateTime lastSuccessAt;
    // Last check that found new posts
    private LocalDateTime lastNewPostAt;
    // Publication date of the newest post in the feed
    private LocalDateTime lastPostPubDate;
    // When the scheduled parser checks the feed next
    private LocalDateTime nextCheckAt;
    private int checkIntervalMinutes;

    private double postsPerDay;
    private double postsPerHour;

    private long totalChecks;
    private long totalErrors;
    private int consecutiveErrors;
    private LocalDateTime lastErrorAt;
    @Column(length = 1000)
    private String lastError;

    // Posts in the feed and new posts saved on the last successful check
    private int lastEntriesCount;
    private int lastNewPostsCount;
    private long totalNewPosts;
    private long lastFetchDurationMs;

    public FeedStatistics() {}

    public FeedStatistics(Feed feed) {
        this.feed = feed;
    }

    /**
     * Records a successful check and schedules the next one.
     *
     * @param checkedAt When the check started
     * @param durationMs How long the check took
     * @param pubDates Publication dates of the posts in the feed (undated posts excluded)
     * @param entriesCount Number of posts in the feed
     * @param newPostsCount Number of new posts saved
     */
    public void recordSuccess(LocalDateTime checkedAt, long durationMs, List<LocalDateTime> pubDates, int entriesCount, int newPostsCount) {
        lastCheckedAt = checkedAt;
        lastSuccessAt = checkedAt;
        lastFetchDurationMs = durationMs;
        totalChecks++;
        consecutiveErrors = 0;

        lastEntriesCount = entriesCount;
        lastNewPostsCount = newPostsCount;
        totalNewPosts += newPostsCount;
        if (newPostsCount > 0) {
            lastNewPostAt = checkedAt;
        }
        pubDates.stream().max(LocalDateTime::compareTo).ifPresent(newest -> lastPostPubDate = newest);

        postsPerDay = estimatePostsPerDay(pubDates, checkedAt);
        postsPerHour = estimatePostsPerHour(pubDates, checkedAt);

        scheduleNextCheck(checkedAt);
    }

    /**
     * Records a failed check and schedules the next one. The frequency is kept from the last successful check.
     *
     * @param checkedAt When the check started
     * @param durationMs How long the check took
     * @param error What went wrong
     */
    public void recordFailure(LocalDateTime checkedAt, long durationMs, String error) {
        lastCheckedAt = checkedAt;
        lastFetchDurationMs = durationMs;
        totalChecks++;
        totalErrors++;
        consecutiveErrors++;
        lastErrorAt = checkedAt;
        lastError = error != null && error.length() > 1000 ? error.substring(0, 1000) : error;

        scheduleNextCheck(checkedAt);
    }

    /**
     * Whether the scheduled parser should check the feed.
     *
     * @param now The current time
     * @return true if the feed was never checked or its next check is due
     */
    public boolean isDue(LocalDateTime now) {
        // Some tolerance, so a feed isn't skipped because the scheduler ran a bit earlier than its next check.
        // It's less than the scheduler's period of a minute, so the feeds aren't checked a run too early.
        return nextCheckAt == null || !nextCheckAt.isAfter(now.plusSeconds(30));
    }

    private void scheduleNextCheck(LocalDateTime checkedAt) {
        if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
            checkIntervalMinutes = RARE_INTERVAL_MINUTES;
        } else if (postsPerHour > VERY_BUSY_POSTS_PER_HOUR) {
            checkIntervalMinutes = VERY_BUSY_INTERVAL_MINUTES;
        } else if (postsPerHour > BUSY_POSTS_PER_HOUR) {
            checkIntervalMinutes = BUSY_INTERVAL_MINUTES;
        } else if (postsPerHour > 1) {
            checkIntervalMinutes = FREQUENT_INTERVAL_MINUTES;
        } else if (postsPerDay > 1) {
            checkIntervalMinutes = DAILY_INTERVAL_MINUTES;
        } else {
            checkIntervalMinutes = RARE_INTERVAL_MINUTES;
        }
        nextCheckAt = checkedAt.plusMinutes(checkIntervalMinutes);
    }

    /**
     * Estimates the posts per day of a feed, from the posts published in the last days.
     * The rate is measured from the oldest of these posts until now, so a feed that stops posting slows down over time.
     *
     * @param pubDates Publication dates of the posts in the feed
     * @param now The current time
     * @return The estimated posts per day, 0 if nothing was published lately
     */
    public static double estimatePostsPerDay(List<LocalDateTime> pubDates, LocalDateTime now) {
        LocalDateTime windowStart = now.minusDays(FREQUENCY_WINDOW_DAYS);
        // Posts dated in the future count as published now
        List<LocalDateTime> recent = pubDates.stream()
                .map(date -> date.isAfter(now) ? now : date)
                .filter(date -> date.isAfter(windowStart))
                .toList();
        if (recent.isEmpty()) {
            return 0;
        }

        LocalDateTime oldest = recent.stream().min(LocalDateTime::compareTo).get();
        // At least an hour, so a single fresh post doesn't look like a flood
        double hours = Math.max(1, Duration.between(oldest, now).toMinutes() / 60.0);
        return recent.size() / hours * 24;
    }

    /**
     * Estimates the posts per hour of a feed, from the posts published in the last hours.
     * The rate is measured over the whole window, unless the feed's oldest post is newer than it
     * (feeds keep only their latest posts), in which case it's measured from that post until now.
     *
     * @param pubDates Publication dates of the posts in the feed
     * @param now The current time
     * @return The estimated posts per hour, 0 if nothing was published lately
     */
    public static double estimatePostsPerHour(List<LocalDateTime> pubDates, LocalDateTime now) {
        LocalDateTime windowStart = now.minusHours(HOURLY_WINDOW_HOURS);
        // Posts dated in the future count as published now
        List<LocalDateTime> dates = pubDates.stream()
                .map(date -> date.isAfter(now) ? now : date)
                .toList();
        long recentCount = dates.stream().filter(date -> date.isAfter(windowStart)).count();
        if (recentCount == 0) {
            return 0;
        }

        LocalDateTime oldest = dates.stream().min(LocalDateTime::compareTo).get();
        LocalDateTime spanStart = oldest.isAfter(windowStart) ? oldest : windowStart;
        // At least an hour, so a single fresh post doesn't look like a flood
        double hours = Math.max(1, Duration.between(spanStart, now).toMinutes() / 60.0);
        return recentCount / hours;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Feed getFeed() { return feed; }
    public void setFeed(Feed feed) { this.feed = feed; }

    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
    public LocalDateTime getLastSuccessAt() { return lastSuccessAt; }
    public LocalDateTime getLastNewPostAt() { return lastNewPostAt; }
    public LocalDateTime getLastPostPubDate() { return lastPostPubDate; }
    public LocalDateTime getNextCheckAt() { return nextCheckAt; }
    public void setNextCheckAt(LocalDateTime nextCheckAt) { this.nextCheckAt = nextCheckAt; }
    public int getCheckIntervalMinutes() { return checkIntervalMinutes; }

    public double getPostsPerDay() { return postsPerDay; }
    public double getPostsPerHour() { return postsPerHour; }

    public long getTotalChecks() { return totalChecks; }
    public long getTotalErrors() { return totalErrors; }
    public int getConsecutiveErrors() { return consecutiveErrors; }
    public LocalDateTime getLastErrorAt() { return lastErrorAt; }
    public String getLastError() { return lastError; }

    public int getLastEntriesCount() { return lastEntriesCount; }
    public int getLastNewPostsCount() { return lastNewPostsCount; }
    public long getTotalNewPosts() { return totalNewPosts; }
    public long getLastFetchDurationMs() { return lastFetchDurationMs; }
}
