package eu.apps4net.feedora.services;

import eu.apps4net.feedora.exceptions.FeedFetchException;
import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.FeedStatistics;
import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.LiveStatus;
import eu.apps4net.feedora.models.LogLevel;
import eu.apps4net.feedora.models.LogType;
import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.FeedStatisticsRepository;
import eu.apps4net.feedora.repositories.PostRepository;
import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.exceptions.NotFoundException;
import eu.apps4net.feedora.utilities.ArticleExtractor;
import eu.apps4net.feedora.utilities.LinkPreviewExtractor;
import eu.apps4net.feedora.utilities.RssFetcher;
import eu.apps4net.feedora.utilities.ThumbnailExtractor;
import eu.apps4net.feedora.utilities.YouTubeLiveChecker;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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
    private FeedStatisticsRepository feedStatisticsRepository;
    @Autowired
    private FeedService feedService;
    @Autowired
    private OperationLockService operationLockService;
    @Autowired
    private LogService logService;
    @Autowired
    private SettingService settingService;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private TransactionTemplate transactionTemplate;

    // Read posts are deleted this many days after they were read, by the post cleanup worker
    @Value("${feedora.posts.read-retention-days:30}")
    private int readRetentionDays;

    // New YouTube videos published within this many days are checked for being scheduled or live streams.
    // Older ones are skipped, so a feed's first check doesn't read the page of every video in it.
    private static final int LIVE_CHECK_MAX_AGE_DAYS = 7;

    // New tweets published within this many days get a preview of their link, read by up to this many threads per feed
    private static final int LINK_PREVIEW_MAX_AGE_DAYS = 7;
    private static final int LINK_PREVIEW_THREADS = 4;

    /**
     * Checks all the feeds of a user for new posts, and adds them as Posts in the database.
     *
     * @param user The user whose feeds to parse
     */
    public void parseFeeds(User user) {
        parseFeeds(user, null, null, false);
    }

    /**
     * Checks the feeds of a user in a folder, or a single feed, for new posts, and adds them as Posts in the database.
     * The feed takes precedence over the folder. With neither, all the feeds of the user are checked.
     *
     * @param user The user whose feeds to parse
     * @param folderId The folder whose feeds to parse, or null
     * @param feedId The feed to parse, or null
     */
    public void parseFeeds(User user, UUID folderId, UUID feedId) {
        parseFeeds(user, folderId, feedId, false);
    }

    /**
     * Checks the feeds of a user that are due, according to how often each feed posts (see FeedStatistics).
     * Used by the scheduled parser.
     *
     * @param user The user whose feeds to parse
     */
    public void parseDueFeeds(User user) {
        parseFeeds(user, null, null, true);
    }

    private void parseFeeds(User user, UUID folderId, UUID feedId, boolean onlyDue) {
        long startTime = System.currentTimeMillis();
        List<Feed> feeds = feedId != null ? feedRepository.findByIdAndUser(feedId, user)
                : folderId != null ? feedRepository.findByUserAndFolder_Id(user, folderId)
                : feedRepository.findByUser(user);
        if (feeds.isEmpty()) {
            System.out.println("[Feedora] No feeds found in database. Skipping feed parsing task.");
            return;
        }
        if (onlyDue) {
            LocalDateTime now = LocalDateTime.now();
            feeds = feeds.stream()
                    .filter(feed -> feed.getStatistics() == null || feed.getStatistics().isDue(now))
                    .toList();
            // Nothing is due, so the lock isn't taken and the frontend doesn't show a scan
            if (feeds.isEmpty()) {
                return;
            }
        }

        // Try to acquire lock to prevent conflicts with OPML import
        if (!operationLockService.tryLockFeedOperation()) {
            System.out.println("[Feedora] Skipping feed parsing - another feed operation is in progress (likely OPML import).");
            return;
        }

        try {
            operationLockService.startProgress(feeds.size());
            feedService.freezeLastParsedAt(user);
            // Read posts older than this may have been deleted by the cleanup, so they must not be added again
            LocalDateTime deletedBefore = readRetentionDays > 0 ? LocalDateTime.now().minusDays(readRetentionDays) : null;
            int threadCount = Math.min(feeds.size(), 8);
            if (threadCount == 0) threadCount = 1;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount); // up to 8 threads, at least 1
            String youtubeApiKey = settingService.getYoutubeApiKey();

            // Use countByUser to avoid CLOB issues
            long postsBefore = postRepository.countByUser(user);
        final int[] failedFeeds = {0};
        final int[] succeededFeeds = {0};

        for (Feed feed : feeds) {
            executor.submit(() -> {
                LocalDateTime checkedAt = LocalDateTime.now();
                long feedStartTime = System.currentTimeMillis();
                boolean success = false;
                List<SyndEntry> entries = null;
                String error = null;

                try {
                    entries = RssFetcher.fetch(feed, feedService);
                    success = true;
                } catch (FeedFetchException e) {
                    synchronized (failedFeeds) { failedFeeds[0]++; }
                    error = e.getMessage();
                    String details = e.getDetails() != null ? e.getDetails() : LogService.describe(e);
                    logService.logFeed(feed, LogLevel.ERROR, e.getType(), e.getMessage(), details);
                } catch (Exception e) {
                    synchronized (failedFeeds) { failedFeeds[0]++; }
                    error = "Couldn't download the feed: " + e.getMessage();
                    logService.logFeed(feed, LogLevel.ERROR, LogType.FEED_FETCH, error, LogService.describe(e));
                }

                int newPosts = 0;
                List<LocalDateTime> pubDates = new ArrayList<>();
                if (success && entries != null) {
                    // On a feed's first check all its entries are added, however old
                    boolean checkedBefore = feed.getStatistics() != null && feed.getStatistics().getLastSuccessAt() != null;
                    // Links already added, including those of this check, so an entry repeated in the feed is added once
                    Set<String> knownLinks = new HashSet<>(postRepository.findLinksByFeedAndUser(feed, user));
                    List<Post> postsToSave = new ArrayList<>();
                    for (SyndEntry entry : entries) {
                        if (entry.getPublishedDate() != null) {
                            pubDates.add(toLocalDateTime(entry.getPublishedDate()));
                        }

                        if (checkedBefore && deletedBefore != null && entry.getPublishedDate() != null
                                && toLocalDateTime(entry.getPublishedDate()).isBefore(deletedBefore)) {
                            continue;
                        }

                        if (entry.getLink() != null && !knownLinks.add(entry.getLink())) {
                            continue;
                        }

                        try {
                            Post post = new Post(
                                entry.getTitle(),
                                entry.getLink(),
                                entry.getDescription() != null ? entry.getDescription().getValue() : null,
                                entry.getPublishedDate() != null
                                    ? toLocalDateTime(entry.getPublishedDate())
                                    : LocalDateTime.now(),
                                entry.getAuthor(),
                                false
                            );
                            post.setContent(getEntryContent(entry));
                            post.setThumbnail(ThumbnailExtractor.find(entry));
                            post.setFeed(feed);
                            post.setUser(user);
                            postsToSave.add(post);
                        } catch (Exception e) {
                            logService.logFeed(feed, LogLevel.ERROR, LogType.POST_SAVE,
                                    "Couldn't save the post \"" + entry.getTitle() + "\" (" + entry.getLink() + ")", LogService.describe(e));
                        }
                    }
                    List<String> liveCheckErrors = checkLiveStates(feed, user, postsToSave, youtubeApiKey);
                    addLinkPreviews(postsToSave);
                    newPosts = savePosts(feed, postsToSave);
                    if (!liveCheckErrors.isEmpty()) {
                        logService.logFeed(feed, LogLevel.WARNING, LogType.YOUTUBE_LIVE_CHECK,
                                "Couldn't check the YouTube videos for scheduled or live streams",
                                String.join("\n", liveCheckErrors.stream().distinct().toList()));
                    }
                    synchronized (succeededFeeds) { succeededFeeds[0]++; }
                }

                long duration = System.currentTimeMillis() - feedStartTime;
                if (success && entries != null) {
                    int entriesCount = entries.size();
                    int savedPosts = newPosts;
                    updateStatistics(feed, stats -> stats.recordSuccess(checkedAt, duration, pubDates, entriesCount, savedPosts));
                } else {
                    String failure = error;
                    updateStatistics(feed, stats -> stats.recordFailure(checkedAt, duration, failure));
                }
                operationLockService.incrementProgress();
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

        logService.logUser(user, failedFeeds[0] > 0 ? LogLevel.WARNING : LogLevel.INFO, LogType.FEED_PARSING,
                "Parsed " + totalFeedsParsed + " feeds in " + seconds + " seconds. Succeeded: " + succeededFeeds[0] + ", Failed: " + failedFeeds[0] + ". Total posts: " + postsAfter + ". New posts added: " + newPosts, null);
        } finally {
            feedService.unfreezeLastParsedAt(user);
            // Always release the lock
            operationLockService.unlockFeedOperation();
        }
    }

    /**
     * Saves the new posts of a feed in a single transaction, with batched inserts. A commit per post is very slow
     * on an HDD (the database syncs its log to disk on every commit), e.g. on a feed's first check or after an OPML import.
     * If the batch fails, the posts are saved one by one, so a single bad post doesn't lose the rest.
     *
     * @param feed The feed of the posts
     * @param posts The posts to save
     * @return The number of posts saved
     */
    private int savePosts(Feed feed, List<Post> posts) {
        if (posts.isEmpty()) {
            return 0;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> postRepository.saveAll(posts));
            return posts.size();
        } catch (Exception batchError) {
            int saved = 0;
            for (Post post : posts) {
                post.setId(null); // The id given in the failed batch was never committed
                try {
                    postRepository.save(post);
                    saved++;
                } catch (Exception e) {
                    logService.logFeed(feed, LogLevel.ERROR, LogType.POST_SAVE,
                            "Couldn't save the post \"" + post.getTitle() + "\" (" + post.getLink() + ")", LogService.describe(e));
                }
            }
            return saved;
        }
    }

    /**
     * Adds a preview (page URL, title and image) of the first link in the text of new tweets published within
     * LINK_PREVIEW_MAX_AGE_DAYS, before they're saved. Each link is read once, even if several tweets share it.
     * Tweets whose link couldn't be read get no preview; it's not worth a log entry.
     *
     * @param newPosts The feed's new posts, not saved yet
     */
    private void addLinkPreviews(List<Post> newPosts) {
        LocalDateTime oldest = LocalDateTime.now().minusDays(LINK_PREVIEW_MAX_AGE_DAYS);
        Map<String, List<Post>> postsByLink = new LinkedHashMap<>();
        for (Post post : newPosts) {
            if (!LinkPreviewExtractor.isTweet(post.getLink()) || post.getPubDate().isBefore(oldest)) continue;
            String link = LinkPreviewExtractor.findLink(post.getDescription());
            if (link != null) postsByLink.computeIfAbsent(link, l -> new ArrayList<>()).add(post);
        }
        if (postsByLink.isEmpty()) return;

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(postsByLink.size(), LINK_PREVIEW_THREADS));
        try {
            List<Future<?>> tasks = new ArrayList<>();
            postsByLink.forEach((link, posts) -> tasks.add(executor.submit(() -> {
                try {
                    LinkPreviewExtractor.LinkPreview preview = LinkPreviewExtractor.fetch(link);
                    if (preview == null) return;
                    for (Post post : posts) {
                        post.setLinkUrl(preview.url());
                        post.setLinkTitle(preview.title());
                        post.setLinkImage(preview.image());
                    }
                } catch (Exception ignored) {
                    // The tweet is shown without a preview
                }
            })));
            for (Future<?> task : tasks) {
                task.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // The tasks catch their own errors
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Checks the live state (scheduled live stream/premiere, or live now) and length of a feed's YouTube videos, in one batch:
     * - the new posts published within LIVE_CHECK_MAX_AGE_DAYS, before they're saved
     * - stored videos that are live, or scheduled to start by now, since their state changes over time
     *   (scheduled → live → ended); videos scheduled for later are only checked once their start time comes
     * - stored recent videos that were never checked, e.g. added before live states existed, or have no length
     *   (once a day, e.g. checked before lengths existed),
     *   and saved videos once a day, since they're kept for long and may be scheduled later
     * Videos that couldn't be checked are left unchanged, and checked again on the feed's next parse.
     *
     * @param feed The feed being parsed
     * @param user The owner of the feed
     * @param newPosts The feed's new posts, not saved yet
     * @param apiKey The YouTube Data API key, or null to read the videos' pages
     * @return The errors of the failed checks
     */
    private List<String> checkLiveStates(Feed feed, User user, List<Post> newPosts, String apiKey) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusMinutes(5);
        Set<String> videoIds = new HashSet<>();

        List<Object[]> storedVideos = new ArrayList<>();
        for (Object[] video : postRepository.findLiveVideosByFeedAndUser(feed, user,
                now.minusDays(LIVE_CHECK_MAX_AGE_DAYS), now.minusDays(1))) {
            LiveStatus status = (LiveStatus) video[2];
            LocalDateTime startAt = (LocalDateTime) video[3];
            if (status == LiveStatus.UPCOMING && startAt != null && startAt.isAfter(soon)) {
                continue;
            }
            String videoId = YouTubeLiveChecker.videoId((String) video[1]);
            if (videoId == null) {
                postRepository.setLiveState((UUID) video[0], null, null, (Integer) video[4], now);
                continue;
            }
            storedVideos.add(video);
            videoIds.add(videoId);
        }

        List<Post> newVideos = newPosts.stream()
                .filter(post -> post.getPubDate().isAfter(now.minusDays(LIVE_CHECK_MAX_AGE_DAYS)))
                .filter(post -> YouTubeLiveChecker.videoId(post.getLink()) != null)
                .toList();
        newVideos.forEach(post -> videoIds.add(YouTubeLiveChecker.videoId(post.getLink())));

        if (videoIds.isEmpty()) {
            return List.of();
        }
        YouTubeLiveChecker.CheckResult result = YouTubeLiveChecker.checkAll(videoIds, apiKey);

        for (Object[] video : storedVideos) {
            YouTubeLiveChecker.LiveInfo info = result.results().get(YouTubeLiveChecker.videoId((String) video[1]));
            if (info != null) {
                // Ended streams keep their start time, and no longer have a live status
                LocalDateTime startAt = info.startAt() != null ? info.startAt() : (LocalDateTime) video[3];
                Integer duration = info.duration() != null ? info.duration() : (Integer) video[4];
                postRepository.setLiveState((UUID) video[0], info.status(), startAt, duration, now);
            }
        }
        for (Post post : newVideos) {
            YouTubeLiveChecker.LiveInfo info = result.results().get(YouTubeLiveChecker.videoId(post.getLink()));
            if (info != null) {
                post.setLiveStatus(info.status());
                post.setLiveStartAt(info.startAt());
                post.setDuration(info.duration());
                post.setLiveCheckedAt(now);
            }
        }
        return result.errors();
    }

    private static LocalDateTime toLocalDateTime(java.util.Date date) {
        return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Applies a change to the statistics of a feed and saves them, creating them on the feed's first check.
     *
     * @param feed The checked feed
     * @param change What to record
     */
    private void updateStatistics(Feed feed, Consumer<FeedStatistics> change) {
        try {
            // The feed may have been removed during the check (e.g. its URL returns an HTML page)
            if (feed.getId() != null && !feedRepository.existsById(feed.getId())) {
                return;
            }
            FeedStatistics stats = feed.getStatistics() != null ? feed.getStatistics() : new FeedStatistics(feed);
            change.accept(stats);
            feed.setStatistics(feedStatisticsRepository.save(stats));
        } catch (Exception e) {
            logService.logFeed(feed, LogLevel.ERROR, LogType.FEED_STATISTICS, "Couldn't save the feed statistics", LogService.describe(e));
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
        boolean pageRead = false;
        try {
            extracted = ArticleExtractor.extractFromUrl(post.getLink());
            pageRead = true;
        } catch (Exception e) {
            logService.logPost(post, LogLevel.ERROR, LogType.CONTENT_EXTRACT,
                    "Couldn't read the post's page: " + e.getMessage(), LogService.describe(e));
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

        // Only log when the page was read, a failed download is already logged above
        if (pageRead) {
            logService.logPost(post, LogLevel.WARNING, LogType.CONTENT_NOT_FOUND,
                    "No article content was found on the post's page, the description is shown instead", null);
        }
        return description;
    }

    /**
     * Returns paginated unread posts for a given user.
     * @param user The user
     * @param page The page number (1-indexed)
     * @param pageSize The number of posts per page
     * @return List of unread posts
     */
    public List<Post> getPostsForUser(User user, int page, int pageSize) {
        return getPostsForUser(user, page, pageSize, null);
    }

    /**
     * Returns paginated unread posts for a given user with search functionality.
     * @param user The user
     * @param page The page number (1-indexed)
     * @param pageSize The number of posts per page
     * @param searchTerm The search term to filter posts by title or description (case-insensitive)
     * @return List of unread posts
     */
    public List<Post> getPostsForUser(User user, int page, int pageSize, String searchTerm) {
        return getPostsForUser(user, page, pageSize, searchTerm, null, null, false, false);
    }

    /**
     * Returns paginated posts for a given user, optionally filtered by search term and by feed or folder.
     * @param user The user
     * @param page The page number (1-indexed)
     * @param pageSize The number of posts per page
     * @param searchTerm The search term to filter posts by title or description (case-insensitive)
     * @param folderId The folder whose feeds' posts to return, or null for all folders
     * @param feedId The feed whose posts to return, or null for all feeds. Takes precedence over folderId.
     * @param saved If true, returns the saved posts (read or unread) instead. Takes precedence over feedId and folderId.
     * @param includeRead If true, returns the read posts too, otherwise only the unread ones
     * @return List of posts, or of saved posts
     */
    public List<Post> getPostsForUser(User user, int page, int pageSize, String searchTerm, UUID folderId, UUID feedId,
                                      boolean saved, boolean includeRead) {
        return getPostsForUser(user, page, pageSize, searchTerm, folderId, feedId, saved, includeRead, null);
    }

    /**
     * Returns paginated posts for a given user, optionally filtered by search term and by feed or folder.
     * @param user The user
     * @param page The page number (1-indexed)
     * @param pageSize The number of posts per page
     * @param searchTerm The search term to filter posts by title or description (case-insensitive)
     * @param folderId The folder whose feeds' posts to return, or null for all folders
     * @param feedId The feed whose posts to return, or null for all feeds. Takes precedence over folderId.
     * @param saved If true, returns the saved posts (read or unread) instead. Takes precedence over feedId and folderId.
     * @param includeRead If true, returns the read posts too, otherwise only the unread ones
     * @param hideShortsFolderIds Folders whose YouTube Shorts are left out (null or empty for none). Not applied to the saved posts.
     * @return List of posts, or of saved posts
     */
    public List<Post> getPostsForUser(User user, int page, int pageSize, String searchTerm, UUID folderId, UUID feedId,
                                      boolean saved, boolean includeRead, List<UUID> hideShortsFolderIds) {
        boolean hideShorts = hideShortsFolderIds != null && !hideShortsFolderIds.isEmpty();
        List<UUID> shortsFolderIds = shortsFolderIdsParam(hideShortsFolderIds);
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "pubDate"));
        boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();

        if (saved) {
            return hasSearch
                    ? postRepository.findByUserAndSavedTrueAndTitleOrDescriptionContaining(user, searchTerm.trim(), pageable)
                    : postRepository.findByUserAndSavedTrue(user, pageable);
        }

        if (feedId != null) {
            return hasSearch
                    ? postRepository.findByUserAndFeedAndTitleOrDescriptionContaining(user, includeRead, feedId, searchTerm.trim(), hideShorts, shortsFolderIds, pageable)
                    : postRepository.findByUserAndFeed(user, includeRead, feedId, hideShorts, shortsFolderIds, pageable);
        }

        if (folderId != null) {
            return hasSearch
                    ? postRepository.findByUserAndFolderAndTitleOrDescriptionContaining(user, includeRead, folderId, searchTerm.trim(), hideShorts, shortsFolderIds, pageable)
                    : postRepository.findByUserAndFolder(user, includeRead, folderId, hideShorts, shortsFolderIds, pageable);
        }

        return hasSearch
                ? postRepository.findByUserAndTitleOrDescriptionContaining(user, includeRead, searchTerm.trim(), hideShorts, shortsFolderIds, pageable)
                : postRepository.findByUser(user, includeRead, hideShorts, shortsFolderIds, pageable);
    }

    /**
     * Value of the hideShortsFolderIds query parameter: the given folders, or a placeholder when there are none,
     * since an empty list can't be bound to NOT IN (the queries ignore it when hideShorts is false).
     */
    private static List<UUID> shortsFolderIdsParam(List<UUID> hideShortsFolderIds) {
        return hideShortsFolderIds != null && !hideShortsFolderIds.isEmpty() ? hideShortsFolderIds : List.of(new UUID(0, 0));
    }

    /**
     * Returns the unread post counts for a given user, in total, per folder and per feed.
     * All the user's feeds are included (with count 0 if they have no unread posts), but only folders with unread posts.
     * @param user The user
     * @return Map with "total" (long), "saved" (long, number of saved posts read or unread),
     *         "folders" (folder id → unread count) and "feeds" (list of {id, title, htmlUrl, folderId, count})
     */
    public Map<String, Object> getUnreadCounts(User user) {
        return getUnreadCounts(user, null);
    }

    /**
     * Returns the unread post counts for a given user, in total, per folder and per feed.
     * All the user's feeds are included (with count 0 if they have no unread posts), but only folders with unread posts.
     * @param user The user
     * @param hideShortsFolderIds Folders whose YouTube Shorts aren't counted (null or empty for none)
     * @return Map with "total" (long), "saved" (long, number of saved posts read or unread),
     *         "folders" (folder id → unread count) and "feeds" (list of {id, title, htmlUrl, folderId, count})
     */
    public Map<String, Object> getUnreadCounts(User user, List<UUID> hideShortsFolderIds) {
        long total = 0;
        Map<String, Long> folderCounts = new HashMap<>();
        List<Map<String, Object>> feedCounts = new ArrayList<>();

        for (Object[] row : postRepository.countUnreadByFeed(user,
                hideShortsFolderIds != null && !hideShortsFolderIds.isEmpty(), shortsFolderIdsParam(hideShortsFolderIds))) {
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
     * Counts the unread posts of a user added after the given time, e.g. parsed since the posts list was loaded.
     * @param user The user
     * @param folderId The folder whose feeds' posts to count, or null for all folders
     * @param feedId The feed whose posts to count, or null for all feeds. Takes precedence over folderId.
     * @param addedAfter Only count the posts added after this time
     * @param hideShortsFolderIds Folders whose YouTube Shorts aren't counted (null or empty for none)
     * @return The number of new unread posts
     */
    public long countNewPosts(User user, UUID folderId, UUID feedId, LocalDateTime addedAfter, List<UUID> hideShortsFolderIds) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Post> post = query.from(Post.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(post.get("user"), user));
        predicates.add(cb.isFalse(post.get("read")));
        predicates.add(cb.greaterThan(post.get("createdAt"), addedAfter));

        if (feedId != null) {
            predicates.add(cb.equal(post.get("feed").get("id"), feedId));
        } else if (folderId != null) {
            predicates.add(cb.equal(post.get("feed").get("folder").get("id"), folderId));
        }

        // Same rule as NOT_HIDDEN_SHORT of the posts list
        if (hideShortsFolderIds != null && !hideShortsFolderIds.isEmpty()) {
            Join<Post, Feed> feed = post.join("feed");
            Join<Feed, Folder> folder = feed.join("folder", JoinType.LEFT);
            predicates.add(cb.or(
                    cb.isNull(post.get("link")),
                    cb.notLike(post.get("link"), "%youtube.com/shorts/%"),
                    cb.isNull(folder.get("id")),
                    cb.not(folder.get("id").in(hideShortsFolderIds))));
        }

        query.select(cb.count(post)).where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(query).getSingleResult();
    }

    /**
     * Returns the posts of a user added after the given time, read or unread, for the live newsfeed.
     * @param user The user
     * @param addedAfter Only return the posts added after this time
     * @param limit The maximum number of posts; the most recently added are kept
     * @return The posts, most recently added first
     */
    public List<Post> getPostsAddedAfter(User user, LocalDateTime addedAfter, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Post> query = cb.createQuery(Post.class);
        Root<Post> post = query.from(Post.class);

        query.select(post)
                .where(cb.equal(post.get("user"), user), cb.greaterThan(post.get("createdAt"), addedAfter))
                .orderBy(cb.desc(post.get("createdAt")));
        return entityManager.createQuery(query).setMaxResults(limit).getResultList();
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
                // Keep the first read date if the post is marked again
                if (!post.isRead() || post.getReadAt() == null) {
                    post.setReadAt(LocalDateTime.now());
                }
                post.setRead(true);
                postRepository.save(post);
                return true;
            }
        }
        return false;
    }

    /**
     * Mark a post as unread for a specific user.
     * Clears its read date too, so the cleanup of old read posts doesn't delete it.
     * @param postId The UUID of the post to mark as unread
     * @param user The user who is marking the post as unread
     * @return true if the post was found and updated, false otherwise
     */
    public boolean markPostAsUnread(UUID postId, User user) {
        Optional<Post> postOptional = postRepository.findById(postId);
        if (postOptional.isPresent()) {
            Post post = postOptional.get();
            // Verify that the post belongs to the specified user
            if (post.getUser().getId().equals(user.getId())) {
                post.setRead(false);
                post.setReadAt(null);
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
     * @param addedBefore Only mark the posts added up to this time (when the list was loaded), so posts parsed
     *                    in the meantime stay unread, or null for all posts
     * @return The number of posts marked as read
     */
    @Transactional
    public int markAllPostsAsRead(User user, UUID folderId, UUID feedId, Integer olderThanDays, LocalDateTime addedBefore) {
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

        if (addedBefore != null) {
            predicates.add(cb.or(cb.isNull(post.get("createdAt")), cb.lessThanOrEqualTo(post.get("createdAt"), addedBefore)));
        }

        update.set(post.<Boolean>get("read"), true)
                .set(post.<LocalDateTime>get("readAt"), LocalDateTime.now())
                .where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(update).executeUpdate();
    }

    /**
     * Deletes the read posts that were read more than the retention period ago (feedora.posts.read-retention-days).
     * Saved posts are kept. Read posts without a read date (read before it was kept) get the current date,
     * so they are deleted a full retention period from now. Runs daily from the post cleanup worker.
     * @return The number of deleted posts
     */
    public int deleteExpiredReadPosts() {
        if (readRetentionDays <= 0) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        postRepository.setMissingReadAt(now);
        return postRepository.deleteReadBefore(now.minusDays(readRetentionDays));
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
