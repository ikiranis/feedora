package eu.apps4net.feedora.services;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.LogEntry;
import eu.apps4net.feedora.models.LogLevel;
import eu.apps4net.feedora.models.LogType;
import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.LogEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {
    @Mock
    private LogEntryRepository logEntryRepository;
    @InjectMocks
    private LogService logService;

    private User user;
    private Feed feed;

    @BeforeEach
    void setUp() {
        user = new User("tester", "tester@example.com", "hash");
        feed = new Feed("Feed title", "http://a.example/rss", "", "rss", null, user);
        feed.setId(UUID.randomUUID());
    }

    private LogEntry savedEntry() {
        ArgumentCaptor<LogEntry> entry = ArgumentCaptor.forClass(LogEntry.class);
        verify(logEntryRepository).save(entry.capture());
        return entry.getValue();
    }

    @Test
    void logFeedSavesAnEntryAboutTheFeed() {
        logService.logFeed(feed, LogLevel.ERROR, LogType.FEED_FETCH, "Failed", "details");

        LogEntry entry = savedEntry();
        assertThat(entry.getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(entry.getType()).isEqualTo(LogType.FEED_FETCH);
        assertThat(entry.getMessage()).isEqualTo("Failed");
        assertThat(entry.getDetails()).isEqualTo("details");
        assertThat(entry.getFeedId()).isEqualTo(feed.getId());
        assertThat(entry.getFeedTitle()).isEqualTo("Feed title");
        assertThat(entry.getUrl()).isEqualTo("http://a.example/rss");
        assertThat(entry.getUser()).isSameAs(user);
        assertThat(entry.getPostId()).isNull();
        assertThat(entry.getCreatedAt()).isCloseTo(LocalDateTime.now(), within(5, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void logPostSavesAnEntryAboutThePostAndItsFeed() {
        Post post = new Post("Post title", "http://a.example/post", null, null, null, false);
        post.setId(UUID.randomUUID());
        post.setFeed(feed);
        post.setUser(user);

        logService.logPost(post, LogLevel.WARNING, LogType.CONTENT_NOT_FOUND, "Nothing", null);

        LogEntry entry = savedEntry();
        assertThat(entry.getPostId()).isEqualTo(post.getId());
        assertThat(entry.getPostTitle()).isEqualTo("Post title");
        assertThat(entry.getFeedId()).isEqualTo(feed.getId());
        assertThat(entry.getFeedTitle()).isEqualTo("Feed title");
        assertThat(entry.getUrl()).isEqualTo("http://a.example/post");
        assertThat(entry.getUser()).isSameAs(user);
    }

    @Test
    void logPostWithoutFeedLeavesTheFeedEmpty() {
        Post post = new Post("Post", "http://a.example/post", null, null, null, false);
        post.setUser(user);

        logService.logPost(post, LogLevel.ERROR, LogType.CONTENT_EXTRACT, "Failed", null);

        assertThat(savedEntry().getFeedId()).isNull();
    }

    @Test
    void longTextIsTruncatedToTheColumnSizes() {
        Post post = new Post("t".repeat(1500), "http://a/" + "l".repeat(3000), null, null, null, false);
        feed.setTitle("f".repeat(1200));
        post.setFeed(feed);

        logService.logPost(post, LogLevel.ERROR, LogType.CONTENT_EXTRACT, "m".repeat(2500), null);

        LogEntry entry = savedEntry();
        assertThat(entry.getMessage()).hasSize(2000);
        assertThat(entry.getPostTitle()).hasSize(1000);
        assertThat(entry.getFeedTitle()).hasSize(1000);
        assertThat(entry.getUrl()).hasSize(2048);
    }

    @Test
    void savingErrorsAreSwallowed() {
        when(logEntryRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        assertThatCode(() -> logService.logFeed(feed, LogLevel.ERROR, LogType.FEED_FETCH, "Failed", null))
                .doesNotThrowAnyException();
    }

    @Test
    void countLogsByFeedMapsTheRows() {
        UUID feedA = UUID.randomUUID();
        UUID feedB = UUID.randomUUID();
        List<UUID> ids = List.of(feedA, feedB);
        when(logEntryRepository.countByFeedIds(user, ids)).thenReturn(List.<Object[]>of(new Object[]{feedA, 3L}));

        assertThat(logService.countLogsByFeed(user, ids)).containsExactly(Map.entry(feedA, 3L));
    }

    @Test
    void countLogsByFeedWithoutFeedsDoesntQuery() {
        assertThat(logService.countLogsByFeed(user, List.of())).isEmpty();
        verifyNoInteractions(logEntryRepository);
    }

    @Test
    void countLogsByPostQueriesTheRepository() {
        UUID postId = UUID.randomUUID();
        when(logEntryRepository.countByUserAndPostId(user, postId)).thenReturn(4L);

        assertThat(logService.countLogsByPost(user, postId)).isEqualTo(4L);
    }

    @Test
    void deleteLogsPrefersPostThenFeedThenAll() {
        UUID feedId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        when(logEntryRepository.deleteByUserAndPostId(user, postId)).thenReturn(1);
        when(logEntryRepository.deleteByUserAndFeedId(user, feedId)).thenReturn(2);
        when(logEntryRepository.deleteByUser(user)).thenReturn(3);

        assertThat(logService.deleteLogs(user, feedId, postId)).isEqualTo(1);
        assertThat(logService.deleteLogs(user, feedId, null)).isEqualTo(2);
        assertThat(logService.deleteLogs(user, null, null)).isEqualTo(3);
    }

    @Test
    void deleteExpiredLogsUsesTheRetentionPeriod() {
        ReflectionTestUtils.setField(logService, "retentionDays", 7);
        when(logEntryRepository.deleteOlderThan(any())).thenReturn(5);

        assertThat(logService.deleteExpiredLogs()).isEqualTo(5);

        ArgumentCaptor<LocalDateTime> before = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(logEntryRepository).deleteOlderThan(before.capture());
        assertThat(before.getValue()).isCloseTo(LocalDateTime.now().minusDays(7), within(5, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void describeListsTheCauses() {
        Exception e = new IllegalStateException("outer", new java.io.IOException("inner", new NullPointerException()));

        assertThat(LogService.describe(e)).isEqualTo(
                "java.lang.IllegalStateException: outer\n"
                        + "Caused by: java.io.IOException: inner\n"
                        + "Caused by: java.lang.NullPointerException");
    }
}
