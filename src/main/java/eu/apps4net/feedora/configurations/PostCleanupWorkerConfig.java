package eu.apps4net.feedora.configurations;

import eu.apps4net.feedora.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
// Can be disabled (e.g. in tests) with feedora.post-cleanup.enabled=false
@ConditionalOnProperty(name = "feedora.post-cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class PostCleanupWorkerConfig {
    @Autowired
    private PostService postService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void startPostCleanupWorker() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int deleted = postService.deleteExpiredReadPosts();
                System.out.println("[Feedora] Post cleanup deleted " + deleted + " old read posts.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        // Waits a few minutes after startup, so it doesn't run together with the first feed parsing
        }, 5, TimeUnit.DAYS.toMinutes(1), TimeUnit.MINUTES);
    }

    @PreDestroy
    public void stopPostCleanupWorker() {
        scheduler.shutdownNow();
    }
}
