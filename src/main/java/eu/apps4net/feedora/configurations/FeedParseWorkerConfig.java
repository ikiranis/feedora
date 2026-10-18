package eu.apps4net.feedora.configurations;

import eu.apps4net.feedora.models.FeedStatistics;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.services.LogService;
import eu.apps4net.feedora.services.PostService;
import eu.apps4net.feedora.services.UserService;
import eu.apps4net.feedora.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
// Can be disabled (e.g. in tests) with feedora.feed-parser.enabled=false
@ConditionalOnProperty(name = "feedora.feed-parser.enabled", havingValue = "true", matchIfMissing = true)
public class FeedParseWorkerConfig {
    @Autowired
    private PostService postService;
    @Autowired
    private UserService userService;
    @Autowired
    private LogService logService;
    @Autowired
    private FeedRepository feedRepository;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void startFeedParseWorker() {
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Starting feed parsing task...");

            try {
                for (User user : userService.getAllUsers()) {
                    // Skip users without feeds silently
                    if (!feedRepository.existsByUser(user)) {
                        continue;
                    }

                    // Only the feeds that are due, depending on how often each one posts
                    // A failure for one user must not stop parsing for the rest
                    try {
                        postService.parseDueFeeds(user);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                logService.deleteExpiredLogs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        // Runs at the shortest check interval, each feed is checked only when its own interval has passed
        }, 0, FeedStatistics.VERY_BUSY_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }
}
