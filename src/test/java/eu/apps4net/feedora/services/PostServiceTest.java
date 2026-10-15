package eu.apps4net.feedora.services;

import eu.apps4net.feedora.exceptions.NotFoundException;
import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.LogLevel;
import eu.apps4net.feedora.models.LogType;
import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.PostRepository;
import eu.apps4net.feedora.support.TestHttpServer;
import eu.apps4net.feedora.utilities.ArticleExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
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
    private FeedService feedService;
    @Mock
    private OperationLockService operationLockService;
    @Mock
    private LogService logService;
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
    }

    private Post post(String title, User owner, LocalDateTime pubDate) {
        Post post = new Post(title, "http://example.com/" + title, "Short description", pubDate, "Author", false);
        post.setId(UUID.randomUUID());
        post.setUser(owner);
        return post;
    }

    @Test
    void parseFeedsIsSkippedWhileAnotherOperationRuns() {
        when(operationLockService.tryLockFeedOperation()).thenReturn(false);

        postService.parseFeeds(user);

        verifyNoInteractions(feedRepository, postRepository);
        verify(operationLockService, never()).unlockFeedOperation();
    }

    @Test
    void parseFeedsReleasesTheLockWhenThereAreNoFeeds() {
        when(operationLockService.tryLockFeedOperation()).thenReturn(true);
        when(feedRepository.findByUser(user)).thenReturn(List.of());

        postService.parseFeeds(user);

        verify(operationLockService).unlockFeedOperation();
        verify(postRepository, never()).save(any());
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

            ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(saved.capture());
            Post post = saved.getValue();
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
        }
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
        verify(postRepository).findByUser(eq(user), eq(false), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(15);
        assertThat(pageable.getValue().getSort().getOrderFor("pubDate").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getPostsForUserPicksTheQueryByFilter() {
        UUID folderId = UUID.randomUUID();
        UUID feedId = UUID.randomUUID();

        postService.getPostsForUser(user, 1, 10, " term ");
        verify(postRepository).findByUserAndTitleOrDescriptionContaining(eq(user), eq(false), eq("term"), any());

        postService.getPostsForUser(user, 1, 10, null, folderId, null, false, true);
        verify(postRepository).findByUserAndFolder(eq(user), eq(true), eq(folderId), any());

        postService.getPostsForUser(user, 1, 10, "x", folderId, null, false, false);
        verify(postRepository).findByUserAndFolderAndTitleOrDescriptionContaining(eq(user), eq(false), eq(folderId), eq("x"), any());

        postService.getPostsForUser(user, 1, 10, null, folderId, feedId, false, false);
        verify(postRepository).findByUserAndFeed(eq(user), eq(false), eq(feedId), any());

        postService.getPostsForUser(user, 1, 10, "x", null, feedId, false, false);
        verify(postRepository).findByUserAndFeedAndTitleOrDescriptionContaining(eq(user), eq(false), eq(feedId), eq("x"), any());

        postService.getPostsForUser(user, 1, 10, null, folderId, feedId, true, false);
        verify(postRepository).findByUserAndSavedTrue(eq(user), any());

        postService.getPostsForUser(user, 1, 10, "x", folderId, feedId, true, false);
        verify(postRepository).findByUserAndSavedTrueAndTitleOrDescriptionContaining(eq(user), eq("x"), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUnreadCountsSumsPerFolder() {
        UUID folderId = UUID.randomUUID();
        UUID feedA = UUID.randomUUID();
        UUID feedB = UUID.randomUUID();
        UUID feedC = UUID.randomUUID();
        when(postRepository.countUnreadByFeed(user)).thenReturn(List.of(
                new Object[]{feedA, "A", "http://a", folderId, 2L},
                new Object[]{feedB, "B", "http://b", folderId, 3L},
                new Object[]{feedC, "C", "http://c", null, 4L}));
        when(postRepository.countByUserAndSavedTrue(user)).thenReturn(5L);

        Map<String, Object> counts = postService.getUnreadCounts(user);

        assertThat(counts.get("total")).isEqualTo(9L);
        assertThat(counts.get("saved")).isEqualTo(5L);
        assertThat((Map<String, Long>) counts.get("folders")).containsExactly(Map.entry(folderId.toString(), 5L));
        List<Map<String, Object>> feeds = (List<Map<String, Object>>) counts.get("feeds");
        assertThat(feeds).hasSize(3);
        assertThat(feeds.get(2)).containsEntry("id", feedC.toString()).containsEntry("folderId", null).containsEntry("count", 4L);
    }

    @Test
    void deleteAllPostsForUserKeepsSavedPosts() {
        List<Post> unsaved = List.of(post("a", user, null));
        when(postRepository.findByUserAndSavedFalse(user)).thenReturn(unsaved);

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
        verify(postRepository).save(mine);

        assertThat(postService.markPostAsRead(theirs.getId(), user)).isFalse();
        assertThat(theirs.isRead()).isFalse();
        assertThat(postService.markPostAsRead(missing, user)).isFalse();
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
