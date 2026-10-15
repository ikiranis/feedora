package eu.apps4net.feedora.support;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.LogEntry;
import eu.apps4net.feedora.models.LogLevel;
import eu.apps4net.feedora.models.LogType;
import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.Role;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.FeedStatisticsRepository;
import eu.apps4net.feedora.repositories.FolderRepository;
import eu.apps4net.feedora.repositories.LogEntryRepository;
import eu.apps4net.feedora.repositories.PostRepository;
import eu.apps4net.feedora.repositories.UserRepository;
import eu.apps4net.feedora.utilities.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base for the API tests: runs the whole application against an in-memory database,
 * starts every test with an empty database and a logged-in admin user.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiTestBase {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected FolderRepository folderRepository;
    @Autowired
    protected FeedRepository feedRepository;
    @Autowired
    protected PostRepository postRepository;
    @Autowired
    protected LogEntryRepository logEntryRepository;
    @Autowired
    protected FeedStatisticsRepository feedStatisticsRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected JwtUtil jwtUtil;

    protected User user;
    protected String token;

    @BeforeEach
    void resetDatabase() {
        logEntryRepository.deleteAll();
        postRepository.deleteAll();
        feedRepository.deleteAll();
        folderRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User("tester", "tester@example.com", passwordEncoder.encode("secret123"), Role.ADMIN));
        token = jwtUtil.generateToken(user.getEmail(), user.getId());
    }

    /** Creates a simple, non admin, user. */
    protected User createUser(String username, String email, String password) {
        return userRepository.save(new User(username, email, passwordEncoder.encode(password)));
    }

    /** Adds the logged-in user's token to a request. The logged-in user is the admin. */
    protected MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token);
    }

    /** Adds the token of another user to a request. */
    protected MockHttpServletRequestBuilder authAs(User other, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + jwtUtil.generateToken(other.getEmail(), other.getId()));
    }

    protected Folder createFolder(String name, User owner) {
        return folderRepository.save(new Folder(name, owner));
    }

    protected Feed createFeed(String title, String xmlUrl, Folder folder, User owner) {
        return feedRepository.save(new Feed(title, xmlUrl, "http://example.com/" + title, "rss", folder, owner));
    }

    protected Post createPost(String title, Feed feed, User owner, boolean read, LocalDateTime pubDate) {
        Post post = new Post(title, "http://example.com/post/" + UUID.randomUUID(), "Description of " + title, pubDate, "Author", read);
        post.setFeed(feed);
        post.setUser(owner);
        return postRepository.save(post);
    }

    protected LogEntry createLog(User owner, UUID feedId, UUID postId, LogLevel level, LocalDateTime createdAt) {
        LogEntry entry = new LogEntry();
        entry.setUser(owner);
        entry.setFeedId(feedId);
        entry.setPostId(postId);
        entry.setLevel(level);
        entry.setType(LogType.FEED_FETCH);
        entry.setMessage("Something happened");
        entry.setCreatedAt(createdAt);
        return logEntryRepository.save(entry);
    }
}
