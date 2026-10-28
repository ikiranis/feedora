package eu.apps4net.feedora.configurations;

import eu.apps4net.feedora.services.ParseService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
// Can be disabled (e.g. in tests) with feedora.page-parser.enabled=false
@ConditionalOnProperty(name = "feedora.page-parser.enabled", havingValue = "true", matchIfMissing = true)
public class PageParseWorkerConfig {
    @Autowired
    private ParseService parseService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void startPageParseWorker() {
        // Checks every minute, each page is parsed only when its own frequency has passed
        scheduler.scheduleAtFixedRate(() -> {
            try {
                parseService.parseDuePages();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.MINUTES);
    }
}
