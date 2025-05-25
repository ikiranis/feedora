package eu.apps4net.feedora.services;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.PostRepository;
import eu.apps4net.feedora.utilities.RssFetcher;
import com.rometools.rome.feed.synd.SyndEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;

@Service
public class PostService {
    @Autowired
    private FeedRepository feedRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private FeedService feedService;

    /**
     * Iterates over the current user's RSS feeds, fetches recent unread posts, and adds them as Posts in the database.
     * (Stub: actual RSS fetching/parsing logic to be implemented)
     *
     * @param user The user whose feeds to parse
     */
    public void parseFeeds(User user) {
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
    }

    /**
     * Returns all posts for a given user, sorted by pubDate descending.
     * @param user The user
     * @return List of posts
     */
    public List<Post> getAllPostsForUser(User user) {
        List<Post> posts = postRepository.findByUser(user);
        posts.sort((a, b) -> {
            if (a.getPubDate() == null && b.getPubDate() == null) return 0;
            if (a.getPubDate() == null) return 1;
            if (b.getPubDate() == null) return -1;
            return b.getPubDate().compareTo(a.getPubDate());
        });
        return posts;
    }

    /**
     * Returns paginated posts for a given user.
     * @param user The user
     * @param page The page number (0-indexed)
     * @param pageSize The number of posts per page
     * @return List of posts
     */
    public List<Post> getPostsForUser(User user, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "pubDate")); // Sorting included in Pageable
        return postRepository.findByUser(user, pageable);
    }

    /**
     * Delete all posts from the database.
     */
    public void deleteAllPosts() {
        postRepository.deleteAll();
    }
}
