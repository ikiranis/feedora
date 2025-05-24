package eu.apps4net.feedora.services;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.PostRepository;
import eu.apps4net.feedora.utilities.RssFetcher;
import com.rometools.rome.feed.synd.SyndEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class PostService {
    @Autowired
    private FeedRepository feedRepository;
    @Autowired
    private PostRepository postRepository;

    /**
     * Iterates over the current user's RSS feeds, fetches recent unread posts, and adds them as Posts in the database.
     * (Stub: actual RSS fetching/parsing logic to be implemented)
     *
     * @param user The user whose feeds to parse
     */
    public void parseFeeds(User user) {
        long startTime = System.currentTimeMillis();
        List<Feed> feeds = feedRepository.findByUser(user);
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(feeds.size(), 8)); // up to 8 threads
        for (Feed feed : feeds) {
            executor.submit(() -> {
                int postsBefore = postRepository.findByFeedAndUser(feed, user).size();
                System.out.println("Parsing feed: " + feed.getTitle() + " (" + feed.getXmlUrl() + ")");
                try {
                    List<SyndEntry> entries = RssFetcher.fetch(feed.getXmlUrl());
                    for (SyndEntry entry : entries) {
                        if (postRepository.findByFeedAndUserAndLink(feed, user, entry.getLink()).isEmpty()) {
                            Post post = new Post(
                                entry.getTitle(),
                                entry.getLink(),
                                entry.getDescription() != null ? entry.getDescription().getValue() : null,
                                entry.getPublishedDate() != null ? ZonedDateTime.ofInstant(entry.getPublishedDate().toInstant(), ZoneId.systemDefault()) : null,
                                entry.getAuthor(),
                                false, // unread
                                feed,
                                user
                            );
                            postRepository.save(post);
                        }
                    }
                    int postsAfter = postRepository.findByFeedAndUser(feed, user).size();
                    System.out.println("Feed parsed: " + feed.getTitle() + " - Total posts: " + postsAfter + " (added " + (postsAfter - postsBefore) + ")");
                } catch (Exception e) {
                    System.err.println("Error parsing feed: " + feed.getTitle() + " (" + feed.getXmlUrl() + ") - " + e.getMessage());
                    e.printStackTrace();
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
        System.out.println("All feeds parsed in " + seconds + " seconds");
    }

    /**
     * Returns all posts for a given user.
     * @param user The user
     * @return List of posts
     */
    public List<Post> getAllPostsForUser(User user) {
        return postRepository.findByUser(user);
    }

    /**
     * Delete all posts from the database.
     */
    public void deleteAllPosts() {
        postRepository.deleteAll();
    }
}
