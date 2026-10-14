package eu.apps4net.feedora.services;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.LogEntry;
import eu.apps4net.feedora.models.LogLevel;
import eu.apps4net.feedora.models.LogType;
import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.LogEntryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LogService {
    @Autowired
    private LogEntryRepository logEntryRepository;
    @PersistenceContext
    private EntityManager entityManager;

    // Log entries older than this are deleted by the feed parse worker
    @Value("${feedora.logs.retention-days:30}")
    private int retentionDays;

    /**
     * Logs a problem with a feed.
     * @param feed The feed the entry is about
     * @param level The severity
     * @param type The kind of problem
     * @param message Short description, shown to the user
     * @param details Extra debugging information, or null
     */
    public void logFeed(Feed feed, LogLevel level, LogType type, String message, String details) {
        LogEntry entry = newEntry(level, type, message, details);
        setFeed(entry, feed);
        entry.setUrl(feed.getXmlUrl());
        entry.setUser(feed.getUser());
        save(entry);
    }

    /**
     * Logs a problem with a post. The entry is also related to the post's feed.
     * @param post The post the entry is about
     * @param level The severity
     * @param type The kind of problem
     * @param message Short description, shown to the user
     * @param details Extra debugging information, or null
     */
    public void logPost(Post post, LogLevel level, LogType type, String message, String details) {
        LogEntry entry = newEntry(level, type, message, details);
        if (post.getFeed() != null) setFeed(entry, post.getFeed());
        entry.setPostId(post.getId());
        entry.setPostTitle(truncate(post.getTitle(), 1000));
        entry.setUrl(truncate(post.getLink(), 2048));
        entry.setUser(post.getUser());
        save(entry);
    }

    /**
     * Returns the log entries of a user, newest first, optionally only those of a feed, a post or a level.
     * @param user The user
     * @param page The page number (1-indexed)
     * @param pageSize The number of entries per page
     * @param feedId Only entries of this feed (including its posts' entries), or null for all
     * @param postId Only entries of this post, or null for all
     * @param level Only entries of this level, or null for all
     * @return List of log entries
     */
    public List<LogEntry> getLogs(User user, int page, int pageSize, UUID feedId, UUID postId, LogLevel level) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LogEntry> query = cb.createQuery(LogEntry.class);
        Root<LogEntry> log = query.from(LogEntry.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(log.get("user"), user));
        if (feedId != null) predicates.add(cb.equal(log.get("feedId"), feedId));
        if (postId != null) predicates.add(cb.equal(log.get("postId"), postId));
        if (level != null) predicates.add(cb.equal(log.get("level"), level));

        query.select(log).where(predicates.toArray(new Predicate[0])).orderBy(cb.desc(log.get("createdAt")));
        return entityManager.createQuery(query)
                .setFirstResult(Math.max(page - 1, 0) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    /**
     * Returns the number of log entries of each of the given feeds.
     * @param user The user who owns the feeds
     * @param feedIds The feeds to count the entries of
     * @return Map of feed id to its number of entries. Feeds without entries are left out.
     */
    public Map<UUID, Long> countLogsByFeed(User user, Collection<UUID> feedIds) {
        Map<UUID, Long> counts = new HashMap<>();
        if (feedIds.isEmpty()) return counts;

        for (Object[] row : logEntryRepository.countByFeedIds(user, feedIds)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }

    /**
     * Returns the number of log entries of a post.
     * @param user The user who owns the post
     * @param postId The post
     * @return The number of entries
     */
    public long countLogsByPost(User user, UUID postId) {
        return logEntryRepository.countByUserAndPostId(user, postId);
    }

    /**
     * Deletes the log entries of a user, or only those of one of their posts or feeds.
     * @param user The user
     * @param feedId The feed whose entries to delete, or null
     * @param postId The post whose entries to delete, or null. Takes precedence over feedId.
     * @return The number of entries deleted
     */
    public int deleteLogs(User user, UUID feedId, UUID postId) {
        if (postId != null) return logEntryRepository.deleteByUserAndPostId(user, postId);
        if (feedId != null) return logEntryRepository.deleteByUserAndFeedId(user, feedId);
        return logEntryRepository.deleteByUser(user);
    }

    /**
     * Deletes the log entries older than the retention period (feedora.logs.retention-days, 30 by default).
     * @return The number of entries deleted
     */
    public int deleteExpiredLogs() {
        return logEntryRepository.deleteOlderThan(LocalDateTime.now().minusDays(retentionDays));
    }

    /**
     * Short description of an exception for the details of a log entry.
     */
    public static String describe(Throwable e) {
        StringBuilder text = new StringBuilder();
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (!text.isEmpty()) text.append("\nCaused by: ");
            text.append(t.getClass().getName());
            if (t.getMessage() != null) text.append(": ").append(t.getMessage());
        }
        return text.toString();
    }

    private LogEntry newEntry(LogLevel level, LogType type, String message, String details) {
        LogEntry entry = new LogEntry();
        entry.setCreatedAt(LocalDateTime.now());
        entry.setLevel(level);
        entry.setType(type);
        entry.setMessage(truncate(message, 2000));
        entry.setDetails(details);
        return entry;
    }

    private void setFeed(LogEntry entry, Feed feed) {
        entry.setFeedId(feed.getId());
        entry.setFeedTitle(truncate(feed.getTitle(), 1000));
    }

    /**
     * Saves the entry and prints it to the console. Never throws, so logging can't break the operation being logged.
     */
    private void save(LogEntry entry) {
        String subject = entry.getPostTitle() != null ? entry.getPostTitle() : entry.getFeedTitle();
        System.err.println("[Feedora] " + entry.getLevel() + " " + entry.getType() + " (" + subject + "): " + entry.getMessage());
        try {
            logEntryRepository.save(entry);
        } catch (Exception e) {
            System.err.println("[Feedora] Could not save log entry - " + e.getMessage());
        }
    }

    private static String truncate(String text, int maxLength) {
        return text != null && text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
