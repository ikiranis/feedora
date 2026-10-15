package eu.apps4net.feedora.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FeedStatisticsTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 15, 12, 0);

    // Posts published every given number of minutes, going back from now
    private List<LocalDateTime> postsEvery(int minutes, int count) {
        return IntStream.range(0, count).mapToObj(i -> now.minusMinutes((long) i * minutes)).toList();
    }

    @Test
    void estimatesPostsPerDayFromTheOldestRecentPostUntilNow() {
        // 4 posts over the last 12 hours
        List<LocalDateTime> dates = List.of(now.minusHours(12), now.minusHours(8), now.minusHours(4), now);

        assertThat(FeedStatistics.estimatePostsPerDay(dates, now)).isCloseTo(8, within(0.001));
    }

    @Test
    void ignoresPostsOlderThanTheWindowAndCountsFuturePostsAsNow() {
        List<LocalDateTime> dates = List.of(now.minusDays(30), now.plusDays(2));

        // Only the future post counts, over the minimum span of an hour
        assertThat(FeedStatistics.estimatePostsPerDay(dates, now)).isCloseTo(24, within(0.001));
        assertThat(FeedStatistics.estimatePostsPerDay(List.of(now.minusDays(30)), now)).isZero();
        assertThat(FeedStatistics.estimatePostsPerDay(List.of(), now)).isZero();
    }

    @Test
    void busyFeedsAreCheckedEveryTenMinutes() {
        FeedStatistics stats = new FeedStatistics();

        stats.recordSuccess(now, 100, postsEvery(20, 10), 10, 3);

        assertThat(stats.getPostsPerHour()).isGreaterThan(1);
        assertThat(stats.getPostsPerDay()).isCloseTo(stats.getPostsPerHour() * 24, within(0.001));
        assertThat(stats.getCheckIntervalMinutes()).isEqualTo(FeedStatistics.FREQUENT_INTERVAL_MINUTES);
        assertThat(stats.getNextCheckAt()).isEqualTo(now.plusMinutes(10));
    }

    @Test
    void feedsWithSomePostsPerDayAreCheckedEveryHour() {
        FeedStatistics stats = new FeedStatistics();

        stats.recordSuccess(now, 100, postsEvery(6 * 60, 8), 8, 0);

        assertThat(stats.getPostsPerHour()).isLessThanOrEqualTo(1);
        assertThat(stats.getPostsPerDay()).isGreaterThan(1);
        assertThat(stats.getNextCheckAt()).isEqualTo(now.plusHours(1));
    }

    @Test
    void quietFeedsAreCheckedEverySixHours() {
        FeedStatistics stats = new FeedStatistics();

        stats.recordSuccess(now, 100, postsEvery(3 * 24 * 60, 3), 3, 0);

        assertThat(stats.getPostsPerDay()).isLessThanOrEqualTo(1);
        assertThat(stats.getNextCheckAt()).isEqualTo(now.plusHours(6));
    }

    @Test
    void recordsTheCheckResults() {
        FeedStatistics stats = new FeedStatistics();

        stats.recordSuccess(now.minusHours(1), 100, List.of(now.minusDays(2)), 5, 2);
        stats.recordSuccess(now, 250, List.of(now.minusDays(2), now.minusMinutes(5)), 6, 0);

        assertThat(stats.getLastCheckedAt()).isEqualTo(now);
        assertThat(stats.getLastSuccessAt()).isEqualTo(now);
        assertThat(stats.getLastNewPostAt()).isEqualTo(now.minusHours(1));
        assertThat(stats.getLastPostPubDate()).isEqualTo(now.minusMinutes(5));
        assertThat(stats.getLastFetchDurationMs()).isEqualTo(250);
        assertThat(stats.getLastEntriesCount()).isEqualTo(6);
        assertThat(stats.getLastNewPostsCount()).isZero();
        assertThat(stats.getTotalNewPosts()).isEqualTo(2);
        assertThat(stats.getTotalChecks()).isEqualTo(2);
        assertThat(stats.getTotalErrors()).isZero();
    }

    @Test
    void failuresKeepTheFrequencyUntilTheyRepeat() {
        FeedStatistics stats = new FeedStatistics();
        stats.recordSuccess(now, 100, postsEvery(20, 10), 10, 0);
        double postsPerDay = stats.getPostsPerDay();

        stats.recordFailure(now.plusMinutes(10), 50, "HTTP 500");
        stats.recordFailure(now.plusMinutes(20), 50, "HTTP 500");

        assertThat(stats.getPostsPerDay()).isEqualTo(postsPerDay);
        assertThat(stats.getConsecutiveErrors()).isEqualTo(2);
        assertThat(stats.getNextCheckAt()).isEqualTo(now.plusMinutes(30));

        stats.recordFailure(now.plusMinutes(30), 50, "x".repeat(2000));

        assertThat(stats.getTotalErrors()).isEqualTo(3);
        assertThat(stats.getLastErrorAt()).isEqualTo(now.plusMinutes(30));
        assertThat(stats.getLastError()).hasSize(1000);
        assertThat(stats.getLastSuccessAt()).isEqualTo(now);
        assertThat(stats.getNextCheckAt()).isEqualTo(now.plusMinutes(30).plusHours(6));

        // A successful check resets the errors
        stats.recordSuccess(now.plusHours(7), 100, postsEvery(20, 10).stream().map(d -> d.plusHours(7)).toList(), 10, 0);

        assertThat(stats.getConsecutiveErrors()).isZero();
        assertThat(stats.getTotalErrors()).isEqualTo(3);
        assertThat(stats.getCheckIntervalMinutes()).isEqualTo(FeedStatistics.FREQUENT_INTERVAL_MINUTES);
    }

    @Test
    void isDueWhenNeverCheckedOrTheNextCheckHasCome() {
        FeedStatistics stats = new FeedStatistics();
        assertThat(stats.isDue(now)).isTrue();

        stats.setNextCheckAt(now.plusMinutes(30));
        assertThat(stats.isDue(now)).isFalse();

        // A scheduler run slightly earlier than the next check still counts
        stats.setNextCheckAt(now.plusSeconds(30));
        assertThat(stats.isDue(now)).isTrue();
    }
}
