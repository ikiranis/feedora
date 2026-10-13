package eu.apps4net.feedora.services;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.PostRepository;
import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.exceptions.NotFoundException;
import eu.apps4net.feedora.utilities.ArticleExtractor;
import eu.apps4net.feedora.utilities.RssFetcher;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PostService {
    @Autowired
    private FeedRepository feedRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private FeedService feedService;
    @Autowired
    private OperationLockService operationLockService;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Iterates over the current user's RSS feeds, fetches recent unread posts, and adds them as Posts in the database.
     * (Stub: actual RSS fetching/parsing logic to be implemented)
     *
     * @param user The user whose feeds to parse
     */
    public void parseFeeds(User user) {
        // Try to acquire lock to prevent conflicts with OPML import
        if (!operationLockService.tryLockFeedOperation()) {
            System.out.println("[Feedora] Skipping feed parsing - another feed operation is in progress (likely OPML import).");
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            List<Feed> feeds = feedRepository.findByUser(user);
            if (feeds.isEmpty()) {
                System.out.println("[Feedora] No feeds found in database. Skipping feed parsing task.");
                return;
            }
            int threadCount = Math.min(feeds.size(), 8);
            if (threadCount == 0) threadCount = 1;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount); // up to 8 threads, at least 1

            // Use countByUser to avoid CLOB issues
            long postsBefore = postRepository.countByUser(user);
        final int[] failedFeeds = {0};
        final int[] succeededFeeds = {0};

        for (Feed feed : feeds) {
            executor.submit(() -> {
                boolean success = false;
                List<SyndEntry> entries = null;

                try {
                    entries = RssFetcher.fetch(feed, feedService);
                    success = true;
                } catch (Exception e) {
                    synchronized (failedFeeds) { failedFeeds[0]++; }
                    System.err.println("Error parsing feed: " + (feed.getTitle() != null ? feed.getTitle() : feed.getId()) + " - " + e.getMessage());
                }

                if (success && entries != null) {
                    for (SyndEntry entry : entries) {
                        // Modify the query to fetch only necessary fields to avoid CLOB issues
                        if (postRepository.existsByFeedAndUserAndLink(feed, user, entry.getLink())) {
                            continue;
                        }

                        Post post = new Post(
                            entry.getTitle(),
                            entry.getLink(),
                            entry.getDescription() != null ? entry.getDescription().getValue() : null,
                            entry.getPublishedDate() != null
                                ? ZonedDateTime.ofInstant(entry.getPublishedDate().toInstant(), ZoneId.systemDefault()).toLocalDateTime()
                                : LocalDateTime.now(),
                            entry.getAuthor(),
                            false
                        );
                        post.setContent(getEntryContent(entry));
                        post.setFeed(feed);
                        post.setUser(user);
                        postRepository.save(post);
                    }
                    synchronized (succeededFeeds) { succeededFeeds[0]++; }
                }
            });
        }

        executor.shutdown();

        try {
            executor.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Feed parsing interrupted");
        }

        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000.0;
        long postsAfter = postRepository.countByUser(user);
        long newPosts = postsAfter - postsBefore;
        int totalFeedsParsed = succeededFeeds[0] + failedFeeds[0];

        System.out.println("[Feedora] Parsed " + totalFeedsParsed + " feeds in " + seconds + " seconds. Succeeded: " + succeededFeeds[0] + ", Failed: " + failedFeeds[0] + ". Total posts: " + postsAfter + ". New posts added: " + newPosts);
        } finally {
            // Always release the lock
            operationLockService.unlockFeedOperation();
        }
    }

    /**
     * Returns all unread posts for a given user, sorted by pubDate descending.
     * @param user The user
     * @return List of unread posts
     */
    public List<Post> getAllPostsForUser(User user) {
        List<Post> posts = postRepository.findByUserAndReadFalse(user);
        posts.sort((a, b) -> {
            if (a.getPubDate() == null && b.getPubDate() == null) return 0;
            if (a.getPubDate() == null) return 1;
            if (b.getPubDate() == null) return -1;
            return b.getPubDate().compareTo(a.getPubDate());
        });
        return posts;
    }

    /**
     * Returns the full content of a feed entry (e.g. RSS content:encoded or Atom content), sanitized.
     * @param entry The feed entry
     * @return Sanitized content HTML, or null if the entry has no full content
     */
    private String getEntryContent(SyndEntry entry) {
        if (entry.getContents() == null || entry.getContents().isEmpty()) return null;

        StringBuilder html = new StringBuilder();
        for (SyndContent content : entry.getContents()) {
            if (content.getValue() != null) html.append(content.getValue());
        }
        String content = ArticleExtractor.sanitize(html.toString(), entry.getLink());
        return content != null ? ArticleExtractor.VERSION_MARKER + content : null;
    }

    /**
     * Returns the full content of a post. Uses the content stored from the feed if it's longer than the description,
     * otherwise downloads the post's page, extracts the article and stores it for next time.
     * Falls back to the description if nothing better can be found.
     * @param postId The UUID of the post
     * @param user The user who owns the post
     * @return Sanitized content HTML, or null if the post has no content at all
     * @throws NotFoundException if the post doesn't exist or belongs to another user
     */
    public String getPostContent(UUID postId, User user) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException(Language.getActionString("Post not found")));

        String description = ArticleExtractor.sanitize(post.getDescription(), post.getLink());
        int descriptionLength = ArticleExtractor.textLength(description);

        String stored = post.getContent();
        boolean storedIsCurrent = stored != null && stored.startsWith(ArticleExtractor.VERSION_MARKER);
        if (storedIsCurrent && ArticleExtractor.textLength(stored) > descriptionLength) {
            return stored;
        }

        String extracted = null;
        try {
            extracted = ArticleExtractor.extractFromUrl(post.getLink());
        } catch (Exception e) {
            System.err.println("[Feedora] Could not extract content from " + post.getLink() + " - " + e.getMessage());
        }

        // Prefer the newly extracted content, then content stored by an older version, if longer than the description
        String best = ArticleExtractor.textLength(extracted) > descriptionLength ? extracted
                : ArticleExtractor.textLength(stored) > descriptionLength ? stored.replace(ArticleExtractor.VERSION_MARKER, "")
                : null;
        if (best != null) {
            post.setContent(ArticleExtractor.VERSION_MARKER + best);
            postRepository.save(post);
            return post.getContent();
        }

        return description;
    }

    /**
     * Returns paginated unread posts for a given user.
     * @param user The user
     * @param page The page number (0-indexed)
     * @param pageSize The number of posts per page
     * @return List of unread posts
     */
    public List<Post> getPostsForUser(User user, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "pubDate")); // Sorting included in Pageable
        return postRepository.findByUserAndReadFalse(user, pageable);
    }

    /**
     * Returns paginated unread posts for a given user with search functionality.
     * @param user The user
     * @param page The page number (0-indexed)
     * @param pageSize The number of posts per page
     * @param searchTerm The search term to filter posts by title or description (case-insensitive)
     * @return List of unread posts
     */
    public List<Post> getPostsForUser(User user, int page, int pageSize, String searchTerm) {
        return getPostsForUser(user, page, pageSize, searchTerm, null, null, false);
    }

    /**
     * Returns paginated unread posts for a given user, optionally filtered by search term and by feed or folder.
     * @param user The user
     * @param page The page number (1-indexed)
     * @param pageSize The number of posts per page
     * @param searchTerm The search term to filter posts by title or description (case-insensitive)
     * @param folderId The folder whose feeds' posts to return, or null for all folders
     * @param feedId The feed whose posts to return, or null for all feeds. Takes precedence over folderId.
     * @param saved If true, returns the saved posts (read or unread) instead. Takes precedence over feedId and folderId.
     * @return List of unread posts, or of saved posts
     */
    public List<Post> getPostsForUser(User user, int page, int pageSize, String searchTerm, UUID folderId, UUID feedId, boolean saved) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "pubDate"));
        boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();

        if (saved) {
            return hasSearch
                    ? postRepository.findByUserAndSavedTrueAndTitleOrDescriptionContaining(user, searchTerm.trim(), pageable)
                    : postRepository.findByUserAndSavedTrue(user, pageable);
        }

        if (feedId != null) {
            return hasSearch
                    ? postRepository.findByUserAndReadFalseAndFeedAndTitleOrDescriptionContaining(user, feedId, searchTerm.trim(), pageable)
                    : postRepository.findByUserAndReadFalseAndFeed(user, feedId, pageable);
        }

        if (folderId != null) {
            return hasSearch
                    ? postRepository.findByUserAndReadFalseAndFolderAndTitleOrDescriptionContaining(user, folderId, searchTerm.trim(), pageable)
                    : postRepository.findByUserAndReadFalseAndFolder(user, folderId, pageable);
        }

        return hasSearch
                ? postRepository.findByUserAndReadFalseAndTitleOrDescriptionContaining(user, searchTerm.trim(), pageable)
                : postRepository.findByUserAndReadFalse(user, pageable);
    }

    /**
     * Returns the unread post counts for a given user, in total, per folder and per feed.
     * All the user's feeds are included (with count 0 if they have no unread posts), but only folders with unread posts.
     * @param user The user
     * @return Map with "total" (long), "saved" (long, number of saved posts read or unread),
     *         "folders" (folder id → unread count) and "feeds" (list of {id, title, htmlUrl, folderId, count})
     */
    public Map<String, Object> getUnreadCounts(User user) {
        long total = 0;
        Map<String, Long> folderCounts = new HashMap<>();
        List<Map<String, Object>> feedCounts = new ArrayList<>();

        for (Object[] row : postRepository.countUnreadByFeed(user)) {
            String folderId = row[3] != null ? row[3].toString() : null;
            long count = (Long) row[4];
            total += count;

            if (folderId != null) {
                folderCounts.merge(folderId, count, Long::sum);
            }

            Map<String, Object> feed = new HashMap<>();
            feed.put("id", row[0].toString());
            feed.put("title", row[1]);
            feed.put("htmlUrl", row[2]);
            feed.put("folderId", folderId);
            feed.put("count", count);
            feedCounts.add(feed);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("saved", postRepository.countByUserAndSavedTrue(user));
        result.put("folders", folderCounts);
        result.put("feeds", feedCounts);
        return result;
    }

    /**
     * Delete all posts from the database.
     */
    public void deleteAllPosts() {
        postRepository.deleteAll();
    }

    /**
     * Delete all posts for a specific user, except the saved ones.
     * @param user The user whose posts should be deleted
     */
    public void deleteAllPostsForUser(User user) {
        List<Post> userPosts = postRepository.findByUserAndSavedFalse(user);
        postRepository.deleteAll(userPosts);
    }

    /**
     * Mark a post as read for a specific user.
     * @param postId The UUID of the post to mark as read
     * @param user The user who is marking the post as read
     * @return true if the post was found and updated, false otherwise
     */
    public boolean markPostAsRead(UUID postId, User user) {
        Optional<Post> postOptional = postRepository.findById(postId);
        if (postOptional.isPresent()) {
            Post post = postOptional.get();
            // Verify that the post belongs to the specified user
            if (post.getUser().getId().equals(user.getId())) {
                post.setRead(true);
                postRepository.save(post);
                return true;
            }
        }
        return false;
    }

    /**
     * Mark all the unread posts of a user as read, optionally only those of a feed or folder, or older than some days.
     * Runs as a single bulk update, so it doesn't load the posts.
     * @param user The user whose posts to mark as read
     * @param folderId The folder whose feeds' posts to mark, or null for all folders
     * @param feedId The feed whose posts to mark, or null for all feeds. Takes precedence over folderId.
     * @param olderThanDays Only mark the posts published more than this many days ago, or null for all posts
     * @return The number of posts marked as read
     */
    @Transactional
    public int markAllPostsAsRead(User user, UUID folderId, UUID feedId, Integer olderThanDays) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<Post> update = cb.createCriteriaUpdate(Post.class);
        Root<Post> post = update.from(Post.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(post.get("user"), user));
        predicates.add(cb.isFalse(post.get("read")));

        if (feedId != null) {
            predicates.add(cb.equal(post.get("feed").get("id"), feedId));
        } else if (folderId != null) {
            Subquery<UUID> folderFeeds = update.subquery(UUID.class);
            Root<Feed> feed = folderFeeds.from(Feed.class);
            folderFeeds.select(feed.get("id")).where(cb.equal(feed.get("folder").get("id"), folderId));
            predicates.add(post.get("feed").get("id").in(folderFeeds));
        }

        if (olderThanDays != null && olderThanDays > 0) {
            predicates.add(cb.lessThan(post.get("pubDate"), LocalDateTime.now().minusDays(olderThanDays)));
        }

        update.set(post.<Boolean>get("read"), true).where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(update).executeUpdate();
    }

    /**
     * Save a post for later, or remove it from the saved posts, for a specific user.
     * @param postId The UUID of the post
     * @param user The user who owns the post
     * @param saved true to save the post, false to remove it from the saved posts
     * @throws NotFoundException if the post doesn't exist or belongs to another user
     */
    public void setPostSaved(UUID postId, User user, boolean saved) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException(Language.getActionString("Post not found")));
        post.setSaved(saved);
        postRepository.save(post);
    }
}
