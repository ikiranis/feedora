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
        List<Feed> feeds = feedRepository.findByUser(user);
        for (Feed feed : feeds) {
            int postsBefore = postRepository.findByFeedAndUser(feed, user).size();
            System.out.println("Parsing feed: " + feed.getTitle() + " (" + feed.getXmlUrl() + ")");
            
            try {
                List<SyndEntry> entries = RssFetcher.fetch(feed.getXmlUrl());

                for (SyndEntry entry : entries) {
                    // Check if post already exists for this feed/user/link
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
                // Log or handle error for this feed
                System.err.println("Error parsing feed: " + feed.getTitle() + " (" + feed.getXmlUrl() + ") - " + e.getMessage());
                e.printStackTrace();
            }
        }
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
