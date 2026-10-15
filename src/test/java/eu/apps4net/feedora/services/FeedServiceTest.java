package eu.apps4net.feedora.services;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.LogLevel;
import eu.apps4net.feedora.models.LogType;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.FolderRepository;
import eu.apps4net.feedora.repositories.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {
    @Mock
    private FeedRepository feedRepository;
    @Mock
    private FolderRepository folderRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private OperationLockService operationLockService;
    @Mock
    private LogService logService;
    @InjectMocks
    private FeedService feedService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("tester", "tester@example.com", "hash");
        user.setId(UUID.randomUUID());
    }

    @Test
    void importOpmlFailsWithoutTouchingFeedsWhileAnotherOperationRuns() {
        when(operationLockService.tryLockFeedOperation()).thenReturn(false);

        assertThatThrownBy(() -> feedService.importOPML(new ByteArrayInputStream(new byte[0]), user))
                .hasMessageContaining("Feed parsing operation is currently in progress");

        verifyNoInteractions(feedRepository, folderRepository, postRepository);
        verify(operationLockService, never()).unlockFeedOperation();
    }

    @Test
    void importOpmlReleasesTheLockWhenTheFileIsInvalid() {
        when(operationLockService.tryLockFeedOperation()).thenReturn(true);
        ByteArrayInputStream invalid = new ByteArrayInputStream("<opml>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> feedService.importOPML(invalid, user)).isInstanceOf(Exception.class);

        verify(operationLockService).unlockFeedOperation();
    }

    @Test
    void importOpmlUsesTheTextAttributeWhenTheFolderHasNoTitle() throws Exception {
        when(operationLockService.tryLockFeedOperation()).thenReturn(true);
        when(folderRepository.findByNameAndUser(anyString(), eq(user))).thenReturn(java.util.Optional.empty());
        when(folderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        String opml = "<opml><body><outline text=\"Text name\">"
                + "<outline type=\"rss\" title=\"Feed\" xmlUrl=\"http://a.example/rss\" htmlUrl=\"http://a.example\"/>"
                + "</outline></body></opml>";

        int added = feedService.importOPML(new ByteArrayInputStream(opml.getBytes(StandardCharsets.UTF_8)), user);

        assertThat(added).isEqualTo(1);
        ArgumentCaptor<Feed> feed = ArgumentCaptor.forClass(Feed.class);
        verify(feedRepository).save(feed.capture());
        assertThat(feed.getValue().getFolderName()).isEqualTo("Text name");
        assertThat(feed.getValue().getXmlUrl()).isEqualTo("http://a.example/rss");
        assertThat(feed.getValue().getHtmlUrl()).isEqualTo("http://a.example");
        assertThat(feed.getValue().getUser()).isSameAs(user);
        verify(postRepository).deleteByUser(user);
        verify(feedRepository).deleteByUser(user);
        verify(folderRepository).deleteByUser(user);
    }

    @Test
    void getFeedsForUserSortsByTitle() {
        feedService.getFeedsForUser(user, 3, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(feedRepository).findByUser(eq(user), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().getOrderFor("title").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getFeedsForUserWithSearchUsesTheTrimmedSearchTerm() {
        feedService.getFeedsForUser(user, 1, 10, "  tech ");
        verify(feedRepository).findByUserAndTitleContainingLogsFirst(eq(user), eq("tech"), any(Pageable.class));

        feedService.getFeedsForUser(user, 1, 10, "   ");
        feedService.getFeedsForUser(user, 1, 10, null);
        verify(feedRepository, org.mockito.Mockito.times(2)).findByUserLogsFirst(eq(user), any(Pageable.class));
    }

    @Test
    void removeInvalidFeedDeletesAndLogsIt() {
        Feed feed = new Feed("Feed", "http://a.example/rss", "", "rss", null, user);
        feed.setId(UUID.randomUUID());

        feedService.removeInvalidFeed(feed, "Not a feed");

        verify(feedRepository).deleteById(feed.getId());
        verify(logService).logFeed(feed, LogLevel.WARNING, LogType.FEED_REMOVED, "Not a feed. The feed was removed.", null);
    }

    @Test
    void removeInvalidFeedLogsAnErrorWhenTheFeedCantBeDeleted() {
        Feed feed = new Feed("Feed", "http://a.example/rss", "", "rss", null, user);
        feed.setId(UUID.randomUUID());
        doThrow(new RuntimeException("DB down")).when(feedRepository).deleteById(feed.getId());

        feedService.removeInvalidFeed(feed, "Not a feed");

        verify(logService).logFeed(eq(feed), eq(LogLevel.ERROR), eq(LogType.FEED_REMOVED),
                eq("Not a feed. The feed couldn't be removed."), contains("DB down"));
    }

    @Test
    void addSingleFeedRejectsAMissingFolder() {
        UUID folderId = UUID.randomUUID();
        when(feedRepository.findByXmlUrlAndUser("http://a.example/rss", user)).thenReturn(java.util.Optional.empty());
        when(folderRepository.findById(folderId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> feedService.addSingleFeed("http://a.example/rss", folderId.toString(), "Title", user))
                .hasMessage("Folder not found or doesn't belong to user");
        verify(feedRepository, never()).save(any());
    }

    @Test
    void fetchFeedInfoWrapsConnectionErrors() {
        assertThatThrownBy(() -> feedService.fetchFeedInfo("http://127.0.0.1:1/rss"))
                .hasMessageStartingWith("Failed to fetch feed info");
    }

    @Test
    void getAllFeedsReturnsEveryFeed() {
        List<Feed> feeds = List.of(new Feed());
        when(feedRepository.findAll()).thenReturn(feeds);

        assertThat(feedService.getAllFeeds()).isSameAs(feeds);
    }
}
