package eu.apps4net.feedora.services;

import eu.apps4net.feedora.exceptions.NotFoundException;
import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.FeedStatistics;
import eu.apps4net.feedora.models.LogLevel;
import eu.apps4net.feedora.models.LogType;
import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.FeedStatisticsRepository;
import eu.apps4net.feedora.repositories.PostRepository;
import eu.apps4net.feedora.support.TestHttpServer;
import eu.apps4net.feedora.utilities.ArticleExtractor;
import eu.apps4net.feedora.utilities.LinkPreviewExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    @Mock
    private FeedRepository feedRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private FeedStatisticsRepository feedStatisticsRepository;
    @Mock
    private FeedService feedService;
    @Mock
    private OperationLockService operationLockService;
    @Mock
    private LogService logService;
    @Mock
    private SettingService settingService;
    @Mock
    private PlatformTransactionManager transactionManager;
    @InjectMocks
    private PostService postService;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        user = new User("tester", "tester@example.com", "hash");
        user.setId(UUID.randomUUID());
        otherUser = new User("other", "other@example.com", "hash");
        otherUser.setId(UUID.randomUUID());
        ReflectionTestUtils.setField(postService, "transactionTemplate", new TransactionTemplate(transactionManager));
    }

    // The posts saved together by the last saveAll call
    @SuppressWarnings("unchecked")
    private List<Post> savedPosts() {
        ArgumentCaptor<List<Post>> saved = ArgumentCaptor.forClass(List.class);
        verify(postRepository).saveAll(saved.capture());
        return saved.getValue();
    }

    private Post post(String title, User owner, LocalDateTime pubDate) {
        Post post = new Post(title, "http://example.com/" + title, "Short description", pubDate, "Author", false);
        post.setId(UUID.randomUUID());
        post.setUser(owner);
        return post;
    }

    @Test
    void parseFeedsIsSkippedWhileAnotherOperationRuns() {
        Feed feed = new Feed("Feed", "http://example.com/rss", "", "rss", null, user);
        when(feedRepository.findByUser(user)).thenReturn(List.of(feed));
        when(operationLockService.tryLockFeedOperation()).thenReturn(false);

        postService.parseFeeds(user);

        verifyNoInteractions(postRepository);
        verify(operationLockService, never()).startProgress(anyInt());
        verify(operationLockService, never()).unlockFeedOperation();
    }

    @Test
    void parseFeedsDoesNotTakeTheLockWhenThereAreNoFeeds() {
        when(feedRepository.findByUser(user)).thenReturn(List.of());

        postService.parseFeeds(user);

        verify(operationLockService, never()).tryLockFeedOperation();
        verify(postRepository, never()).save(any());
        verify(postRepository, never()).saveAll(any());
    }

    @Test
    void parseFeedsSavesNewEntriesWithFeedContent() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/rss", 200, "application/rss+xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/"><channel><title>Feed</title>
                    <item><title>Post</title><link>http://example.com/post</link><description>Summary</description>
                    <content:encoded><![CDATA[<p>Full content<script>alert(1)</script></p>]]></content:encoded></item>
                    </channel></rss>
                    """);
            Feed feed = new Feed("Feed", server.url("/rss"), "", "rss", null, user);
            when(operationLockService.tryLockFeedOperation()).thenReturn(true);
            when(feedRepository.findByUser(user)).thenReturn(List.of(feed));

            postService.parseFeeds(user);

            List<Post> saved = savedPosts();
            assertThat(saved).hasSize(1);
            Post post = saved.get(0);
            assertThat(post.getTitle()).isEqualTo("Post");
            assertThat(post.getDescription()).isEqualTo("Summary");
            assertThat(post.getContent()).isEqualTo(ArticleExtractor.VERSION_MARKER + "<p>Full content</p>");
            assertThat(post.getFeed()).isSameAs(feed);
            assertThat(post.getUser()).isSameAs(user);
            assertThat(post.isRead()).isFalse();
            assertThat(post.getPubDate()).isNotNull();
            verify(operationLockService).unlockFeedOperation();
        }
    }

    @Test
    void parseFeedsLogsInvalidFeeds() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/rss", 200, "application/xml", "<?xml version=\"1.0\"?><notafeed/>");
            Feed feed = new Feed("Feed", server.url("/rss"), "", "rss", null, user);
            when(operationLockService.tryLockFeedOperation()).thenReturn(true);
            when(feedRepository.findByUser(user)).thenReturn(List.of(feed));

            postService.parseFeeds(user);

            verify(logService).logFeed(eq(feed), eq(LogLevel.ERROR), eq(LogType.FEED_PARSE), anyString(), anyString());
            verify(postRepository, never()).save(any());
            verify(postRepository, never()).saveAll(any());
        }
    }

    @Test
    void parseFeedsRecordsTheStatisticsOfEachFeed() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/rss", 200, "application/rss+xml", TestHttpServer.rss("Feed",
                    TestHttpServer.rssItem("First", "http://example.com/1", "First"),
                    TestHttpServer.rssItem("Second", "http://example.com/2", "Second")));
            Feed feed = new Feed("Feed", server.url("/rss"), "", "rss", null, user);
            Feed broken = new Feed("Broken", server.url("/missing"), "", "rss", null, user);
            when(operationLockService.tryLockFeedOperation()).thenReturn(true);
            when(feedRepository.findByUser(user)).thenReturn(List.of(feed, broken));
            when(postRepository.findLinksByFeedAndUser(feed, user)).thenReturn(List.of("http://example.com/1"));
            when(feedStatisticsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            postService.parseFeeds(user);

            FeedStatistics stats = feed.getStatistics();
            assertThat(stats.getFeed()).isSameAs(feed);
            assertThat(stats.getLastEntriesCount()).isEqualTo(2);
            assertThat(stats.getLastNewPostsCount()).isEqualTo(1);
            assertThat(stats.getConsecutiveErrors()).isZero();
            assertThat(stats.getNextCheckAt()).isNotNull();

            FeedStatistics brokenStats = broken.getStatistics();
            assertThat(brokenStats.getTotalErrors()).isEqualTo(1);
            assertThat(brokenStats.getLastError()).contains("HTTP 404");
            assertThat(brokenStats.getLastSuccessAt()).isNull();
        }
    }

    @Test
    void parseFeedsSavesTheNewPostsOfAFeedTogetherAndSkipsRepeatedEntries() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/rss", 200, "application/rss+xml", TestHttpServer.rss("Feed",
                    TestHttpServer.rssItem("Known", "http://example.com/known", "Known"),
                    TestHttpServer.rssItem("First", "http://example.com/1", "First"),
                    TestHttpServer.rssItem("Repeated", "http://example.com/1", "Repeated"),
                    TestHttpServer.rssItem("Second", "http://example.com/2", "Second")));
            Feed feed = new Feed("Feed", server.url("/rss"), "", "rss", null, user);
            when(operationLockService.tryLockFeedOperation()).thenReturn(true);
            when(feedRepository.findByUser(user)).thenReturn(List.of(feed));
            when(postRepository.findLinksByFeedAndUser(feed, user)).thenReturn(List.of("http://example.com/known"));

            postService.parseFeeds(user);

            assertThat(savedPosts()).extracting(Post::getTitle).containsExactly("First", "Second");
            verify(postRepository, never()).save(any());
            verify(transactionManager).commit(any());
        }
    }

    @Test
    void parseFeedsSavesThePostsOneByOneWhenTheBatchFails() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/rss", 200, "application/rss+xml", TestHttpServer.rss("Feed",
                    TestHttpServer.rssItem("Good", "http://example.com/good", "Good"),
                    TestHttpServer.rssItem("Bad", "http://example.com/bad", "Bad")));
            Feed feed = new Feed("Feed", server.url("/rss"), "", "rss", null, user);
            when(operationLockService.tryLockFeedOperation()).thenReturn(true);
            when(feedRepository.findByUser(user)).thenReturn(List.of(feed));
            when(postRepository.saveAll(any())).thenThrow(new RuntimeException("Batch failed"));
            when(postRepository.save(any())).thenAnswer(invocation -> {
                Post post = invocation.getArgument(0);
                if (post.getTitle().equals("Bad")) throw new RuntimeException("Bad post");
                return post;
            });
            when(feedStatisticsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            postService.parseFeeds(user);

            verify(transactionManager).rollback(any());
            verify(postRepository, times(2)).save(any());
            verify(logService).logFeed(eq(feed), eq(LogLevel.ERROR), eq(LogType.POST_SAVE), contains("Bad"), anyString());
            assertThat(feed.getStatistics().getLastNewPostsCount()).isEqualTo(1);
        }
    }

    @Test
    void parseFeedsSkipsEntriesOlderThanTheReadRetentionOnceTheFeedWasChecked() throws Exception {
        ReflectionTestUtils.setField(postService, "readRetentionDays", 30);
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/rss", 200, "application/rss+xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <rss version="2.0"><channel><title>Feed</title>
                    <item><title>Old</title><link>http://example.com/old</link><pubDate>Mon, 01 Jan 2024 10:00:00 GMT</pubDate></item>
                    <item><title>Undated</title><link>http://example.com/undated</link></item>
                    </channel></rss>
                    """);
            Feed checked = new Feed("Checked", server.url("/rss"), "", "rss", null, user);
            FeedStatistics stats = new FeedStatistics(checked);
            stats.recordSuccess(LocalDateTime.now().minusDays(1), 10, List.of(), 2, 2);
            checked.setStatistics(stats);
            when(operationLockService.tryLockFeedOperation()).thenReturn(true);
            when(feedRepository.findByUser(user)).thenReturn(List.of(checked));

            postService.parseFeeds(user);

            // The old entry may be a read post deleted by the cleanup
            assertThat(savedPosts()).extracting(Post::getTitle).containsExactly("Undated");
        }
    }

    @Test
    void parseFeedsAddsOldEntriesOnTheFeedsFirstCheck() throws Exception {
        ReflectionTestUtils.setField(postService, "readRetentionDays", 30);
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/rss", 200, "application/rss+xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <rss version="2.0"><channel><title>Feed</title>
                    <item><title>Old</title><link>http://example.com/old</link><pubDate>Mon, 01 Jan 2024 10:00:00 GMT</pubDate></item>
                    </channel></rss>
                    """);
            Feed feed = new Feed("Feed", server.url("/rss"), "", "rss", null, user);
            when(operationLockService.tryLockFeedOperation()).thenReturn(true);
            when(feedRepository.findByUser(user)).thenReturn(List.of(feed));

            postService.parseFeeds(user);

            assertThat(savedPosts()).extracting(Post::getTitle).containsExactly("Old");
        }
    }

    @Test
    void parseDueFeedsChecksOnlyTheFeedsThatAreDue() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/rss", 200, "application/rss+xml", TestHttpServer.rss("Feed",
                    TestHttpServer.rssItem("Post", "http://example.com/post", "Post")));
            Feed newFeed = new Feed("New", server.url("/rss"), "", "rss", null, user);
            Feed dueFeed = new Feed("Due", server.url("/rss"), "", "rss", null, user);
            FeedStatistics dueStats = new FeedStatistics(dueFeed);
            dueStats.setNextCheckAt(LocalDateTime.now().minusMinutes(5));
            dueFeed.setStatistics(dueStats);
            Feed notDueFeed = new Feed("Not due", server.url("/rss"), "", "rss", null, user);
            FeedStatistics notDueStats = new FeedStatistics(notDueFeed);
            notDueStats.setNextCheckAt(LocalDateTime.now().plusHours(1));
            notDueFeed.setStatistics(notDueStats);
            when(operationLockService.tryLockFeedOperation()).thenReturn(true);
            when(feedRepository.findByUser(user)).thenReturn(List.of(newFeed, dueFeed, notDueFeed));
            when(feedStatisticsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            postService.parseDueFeeds(user);

            // One batch per checked feed
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Post>> saved = ArgumentCaptor.forClass(List.class);
            verify(postRepository, times(2)).saveAll(saved.capture());
            assertThat(saved.getAllValues()).flatExtracting(posts -> posts).extracting(Post::getFeed)
                    .containsExactlyInAnyOrder(newFeed, dueFeed);
            assertThat(newFeed.getStatistics()).isNotNull();
            assertThat(notDueStats.getTotalChecks()).isZero();
            verify(operationLockService).unlockFeedOperation();
        }
    }

    @Test
    void parseDueFeedsDoesNothingWhenNoFeedIsDue() {
        Feed feed = new Feed("Feed", "http://example.com/rss", "", "rss", null, user);
        FeedStatistics stats = new FeedStatistics(feed);
        stats.setNextCheckAt(LocalDateTime.now().plusHours(1));
        feed.setStatistics(stats);
        when(feedRepository.findByUser(user)).thenReturn(List.of(feed));

        postService.parseDueFeeds(user);

        verifyNoInteractions(postRepository, feedStatisticsRepository);
        // Nothing is due, so no scan is shown in the frontend
        verify(operationLockService, never()).tryLockFeedOperation();
    }

    @Test
    void getAllPostsForUserSortsNewestFirstWithUndatedLast() {
        Post undated = post("undated", user, null);
        Post old = post("old", user, LocalDateTime.now().minusDays(1));
        Post recent = post("recent", user, LocalDateTime.now());
        when(postRepository.findByUserAndReadFalse(user)).thenReturn(new ArrayList<>(List.of(undated, old, recent)));

        assertThat(postService.getAllPostsForUser(user)).containsExactly(recent, old, undated);
    }

    @Test
    void getPostsForUserPagesByNewestFirst() {
        postService.getPostsForUser(user, 2, 15);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findByUser(eq(user), eq(false), eq(false), any(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(15);
        assertThat(pageable.getValue().getSort().getOrderFor("pubDate").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getPostsForUserPicksTheQueryByFilter() {
        UUID folderId = UUID.randomUUID();
        UUID feedId = UUID.randomUUID();

        postService.getPostsForUser(user, 1, 10, " term ");
        verify(postRepository).findByUserAndTitleOrDescriptionContaining(eq(user), eq(false), eq("term"), eq(false), any(), any());

        postService.getPostsForUser(user, 1, 10, null, folderId, null, false, true);
        verify(postRepository).findByUserAndFolder(eq(user), eq(true), eq(folderId), eq(false), any(), any());

        postService.getPostsForUser(user, 1, 10, "x", folderId, null, false, false);
        verify(postRepository).findByUserAndFolderAndTitleOrDescriptionContaining(eq(user), eq(false), eq(folderId), eq("x"), eq(false), any(), any());

        postService.getPostsForUser(user, 1, 10, null, folderId, feedId, false, false);
        verify(postRepository).findByUserAndFeed(eq(user), eq(false), eq(feedId), eq(false), any(), any());

        postService.getPostsForUser(user, 1, 10, "x", null, feedId, false, false);
        verify(postRepository).findByUserAndFeedAndTitleOrDescriptionContaining(eq(user), eq(false), eq(feedId), eq("x"), eq(false), any(), any());

        postService.getPostsForUser(user, 1, 10, null, folderId, feedId, true, false);
        verify(postRepository).findByUserAndSavedTrue(eq(user), any());

        postService.getPostsForUser(user, 1, 10, "x", folderId, feedId, true, false);
        verify(postRepository).findByUserAndSavedTrueAndTitleOrDescriptionContaining(eq(user), eq("x"), any());

        postService.getPostsForUser(user, 1, 10, null, folderId, feedId, true, true, false, null);
        verify(postRepository).findByUserAndFavoriteTrue(eq(user), any());

        postService.getPostsForUser(user, 1, 10, "x", folderId, feedId, true, true, false, null);
        verify(postRepository).findByUserAndFavoriteTrueAndTitleOrDescriptionContaining(eq(user), eq("x"), any());
    }

    @Test
    void getPostsForUserPassesTheFoldersWhoseShortsAreHidden() {
        UUID folderId = UUID.randomUUID();

        postService.getPostsForUser(user, 1, 10, null, null, null, false, false, List.of(folderId));
        verify(postRepository).findByUser(eq(user), eq(false), eq(true), eq(List.of(folderId)), any());

        postService.getPostsForUser(user, 1, 10, null, folderId, null, false, false, List.of());
        verify(postRepository).findByUserAndFolder(eq(user), eq(false), eq(folderId), eq(false), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUnreadCountsSumsPerFolder() {
        UUID folderId = UUID.randomUUID();
        UUID feedA = UUID.randomUUID();
        UUID feedB = UUID.randomUUID();
        UUID feedC = UUID.randomUUID();
        when(postRepository.countUnreadByFeed(eq(user), eq(false), any())).thenReturn(List.of(
                new Object[]{feedA, "A", "http://a", folderId, 2L},
                new Object[]{feedB, "B", "http://b", folderId, 3L},
                new Object[]{feedC, "C", "http://c", null, 4L}));
        when(postRepository.countByUserAndSavedTrue(user)).thenReturn(5L);
        when(postRepository.countByUserAndFavoriteTrue(user)).thenReturn(2L);

        Map<String, Object> counts = postService.getUnreadCounts(user);

        assertThat(counts.get("total")).isEqualTo(9L);
        assertThat(counts.get("saved")).isEqualTo(5L);
        assertThat(counts.get("favorites")).isEqualTo(2L);
        assertThat((Map<String, Long>) counts.get("folders")).containsExactly(Map.entry(folderId.toString(), 5L));
        List<Map<String, Object>> feeds = (List<Map<String, Object>>) counts.get("feeds");
        assertThat(feeds).hasSize(3);
        assertThat(feeds.get(2)).containsEntry("id", feedC.toString()).containsEntry("folderId", null).containsEntry("count", 4L);
    }

    @Test
    void deleteAllPostsForUserKeepsSavedAndFavoritePosts() {
        List<Post> unsaved = List.of(post("a", user, null));
        when(postRepository.findByUserAndSavedFalseAndFavoriteFalse(user)).thenReturn(unsaved);

        postService.deleteAllPostsForUser(user);

        verify(postRepository).deleteAll(unsaved);
    }

    @Test
    void deleteAllPostsDeletesEverything() {
        postService.deleteAllPosts();
        verify(postRepository).deleteAll();
    }

    @Test
    void markPostAsReadOnlyMarksTheUsersPosts() {
        Post mine = post("mine", user, null);
        Post theirs = post("theirs", otherUser, null);
        when(postRepository.findById(mine.getId())).thenReturn(Optional.of(mine));
        when(postRepository.findById(theirs.getId())).thenReturn(Optional.of(theirs));
        UUID missing = UUID.randomUUID();
        when(postRepository.findById(missing)).thenReturn(Optional.empty());

        assertThat(postService.markPostAsRead(mine.getId(), user)).isTrue();
        assertThat(mine.isRead()).isTrue();
        assertThat(mine.getReadAt()).isCloseTo(LocalDateTime.now(), within(5, java.time.temporal.ChronoUnit.SECONDS));
        verify(postRepository).save(mine);

        assertThat(postService.markPostAsRead(theirs.getId(), user)).isFalse();
        assertThat(theirs.isRead()).isFalse();
        assertThat(theirs.getReadAt()).isNull();
        assertThat(postService.markPostAsRead(missing, user)).isFalse();
    }

    @Test
    void markPostAsReadKeepsTheFirstReadDate() {
        Post post = post("post", user, null);
        LocalDateTime firstRead = LocalDateTime.now().minusDays(3);
        post.setRead(true);
        post.setReadAt(firstRead);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        postService.markPostAsRead(post.getId(), user);

        assertThat(post.getReadAt()).isEqualTo(firstRead);
    }

    @Test
    void deleteExpiredReadPostsUsesTheRetentionPeriod() {
        ReflectionTestUtils.setField(postService, "readRetentionDays", 30);
        when(postRepository.deleteReadBefore(any())).thenReturn(4);

        assertThat(postService.deleteExpiredReadPosts()).isEqualTo(4);

        ArgumentCaptor<LocalDateTime> readAt = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> before = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(postRepository).setMissingReadAt(readAt.capture());
        verify(postRepository).deleteReadBefore(before.capture());
        assertThat(readAt.getValue()).isCloseTo(LocalDateTime.now(), within(5, java.time.temporal.ChronoUnit.SECONDS));
        assertThat(before.getValue()).isEqualTo(readAt.getValue().minusDays(30));
    }

    @Test
    void deleteExpiredReadPostsDoesNothingWithoutARetentionPeriod() {
        ReflectionTestUtils.setField(postService, "readRetentionDays", 0);

        assertThat(postService.deleteExpiredReadPosts()).isZero();
        verifyNoInteractions(postRepository);
    }

    @Test
    void setPostSavedRejectsOtherUsersPosts() {
        Post theirs = post("theirs", otherUser, null);
        when(postRepository.findById(theirs.getId())).thenReturn(Optional.of(theirs));

        assertThatThrownBy(() -> postService.setPostSaved(theirs.getId(), user, true))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Post not found");
        assertThat(theirs.isSaved()).isFalse();
    }

    @Test
    void setPostFavoriteRejectsOtherUsersPosts() {
        Post theirs = post("theirs", otherUser, null);
        when(postRepository.findById(theirs.getId())).thenReturn(Optional.of(theirs));

        assertThatThrownBy(() -> postService.setPostFavorite(theirs.getId(), user, true))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Post not found");
        assertThat(theirs.isFavorite()).isFalse();
    }

    @Test
    void getPostContentReturnsCurrentStoredContentWithoutDownloading() {
        Post post = post("post", user, null);
        post.setLink("http://127.0.0.1:1/never-downloaded");
        String stored = ArticleExtractor.VERSION_MARKER + "<p>A stored article that is much longer than the description.</p>";
        post.setContent(stored);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThat(postService.getPostContent(post.getId(), user)).isEqualTo(stored);
        verifyNoInteractions(logService);
        verify(postRepository, never()).save(any());
    }

    @Test
    void getPostContentReturnsDescriptionOfTweetsWithoutDownloading() {
        Post post = post("tweet", user, null);
        post.setLink("https://x.com/AP/status/1");
        post.setDescription("<p>The tweet text</p>");
        post.setContent(ArticleExtractor.VERSION_MARKER + "<h2>Log in or sign up for X</h2><p>See what's happening and join the conversation</p>");
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThat(postService.getPostContent(post.getId(), user)).isEqualTo("<p>The tweet text</p>");
        verifyNoInteractions(logService);
        verify(postRepository, never()).save(any());
    }

    @Test
    void getPostContentReturnsStoredTweetContent() {
        Post post = post("tweet", user, null);
        post.setLink("https://x.com/AP/status/1");
        post.setContent(LinkPreviewExtractor.TWEET_CONTENT_MARKER + "<p>Text</p><div class=\"tweet-photos\"><img src=\"https://pbs.twimg.com/media/a.jpg\" alt=\"\"></div>");
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThat(postService.getPostContent(post.getId(), user))
                .isEqualTo("<p>Text</p><div class=\"tweet-photos\"><img src=\"https://pbs.twimg.com/media/a.jpg\" alt=\"\"></div>");
    }

    @Test
    void getPostContentAddsThumbnailToOlderTweets() {
        Post post = post("tweet", user, null);
        post.setLink("https://x.com/AP/status/1");
        post.setDescription("<p>Legend.</p>");
        post.setThumbnail("https://pbs.twimg.com/media/a.jpg");
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThat(postService.getPostContent(post.getId(), user))
                .isEqualTo("<p>Legend.</p><div class=\"tweet-photos\"><img src=\"https://pbs.twimg.com/media/a.jpg\" alt=\"\"></div>");
    }

    @Test
    void getPostContentReusesOlderStoredContentWhenThePageCantBeRead() {
        Post post = post("post", user, null);
        post.setLink("http://127.0.0.1:1/unreachable");
        post.setContent("<p>Content stored by an older version, longer than the description.</p>");
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        String content = postService.getPostContent(post.getId(), user);

        assertThat(content).isEqualTo(ArticleExtractor.VERSION_MARKER + "<p>Content stored by an older version, longer than the description.</p>");
        verify(postRepository).save(post);
        verify(logService).logPost(eq(post), eq(LogLevel.ERROR), eq(LogType.CONTENT_EXTRACT), anyString(), anyString());
    }

    @Test
    void getPostContentLogsWhenThePageHasNoArticle() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/empty", 200, "text/html", "<html><body></body></html>");
            Post post = post("post", user, null);
            post.setLink(server.url("/empty"));
            when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

            assertThat(postService.getPostContent(post.getId(), user)).isEqualTo("<p>Short description</p>");

            verify(logService).logPost(eq(post), eq(LogLevel.WARNING), eq(LogType.CONTENT_NOT_FOUND), anyString(), isNull());
            verify(postRepository, never()).save(any());
        }
    }

    @Test
    void getPostContentOfAnotherUsersPostIsNotFound() {
        Post theirs = post("theirs", otherUser, null);
        when(postRepository.findById(theirs.getId())).thenReturn(Optional.of(theirs));

        assertThatThrownBy(() -> postService.getPostContent(theirs.getId(), user)).isInstanceOf(NotFoundException.class);
    }
}
