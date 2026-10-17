package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.LogLevel;
import eu.apps4net.feedora.models.LogType;
import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.services.PostService;
import eu.apps4net.feedora.support.ApiTestBase;
import eu.apps4net.feedora.support.TestHttpServer;
import eu.apps4net.feedora.utilities.ArticleExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostControllerTest extends ApiTestBase {
    @Autowired
    private PostService postService;

    private final LocalDateTime now = LocalDateTime.now();

    private Folder news;
    private Feed bbc;
    private Feed cnn;
    private Feed verge;

    private void createFeeds() {
        news = createFolder("News", user);
        bbc = createFeed("BBC", "http://bbc.example/rss", news, user);
        cnn = createFeed("CNN", "http://cnn.example/rss", news, user);
        verge = createFeed("Verge", "http://verge.example/rss", null, user);
    }

    @Test
    void getAllPostsReturnsUnreadPostsNewestFirst() throws Exception {
        createFeeds();
        createPost("Old", bbc, user, false, now.minusDays(2));
        createPost("New", bbc, user, false, now);
        createPost("Read", bbc, user, true, now.minusDays(1));

        mockMvc.perform(auth(get("/api/posts/getAllPosts")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", contains("New", "Old")))
                .andExpect(jsonPath("$[0].feed.title").value("BBC"))
                .andExpect(jsonPath("$[0].user").doesNotExist())
                .andExpect(jsonPath("$[0].content").doesNotExist());
    }

    @Test
    void getPostsReturnsTheUnreadPostsPaginated() throws Exception {
        createFeeds();
        createPost("One", bbc, user, false, now.minusDays(3));
        createPost("Two", cnn, user, false, now.minusDays(2));
        createPost("Three", verge, user, false, now.minusDays(1));
        createPost("Read", verge, user, true, now);
        Feed foreignFeed = createFeed("Foreign", "http://f.example/rss", null, createUser("other", "other@example.com", "secret123"));
        createPost("Foreign", foreignFeed, foreignFeed.getUser(), false, now);

        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", contains("Three", "Two")));

        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "2").param("pageSize", "2")))
                .andExpect(jsonPath("$[*].title", contains("One")));

        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10").param("includeRead", "true")))
                .andExpect(jsonPath("$[*].title", contains("Read", "Three", "Two", "One")));
    }

    @Test
    void getPostsFiltersByFolderFeedAndSearch() throws Exception {
        createFeeds();
        createPost("Election results", bbc, user, false, now.minusDays(3));
        createPost("Weather", cnn, user, false, now.minusDays(2));
        createPost("Gadgets", verge, user, false, now.minusDays(1));
        createPost("Read election", bbc, user, true, now);

        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10").param("folderId", news.getId().toString())))
                .andExpect(jsonPath("$[*].title", contains("Weather", "Election results")));

        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10").param("feedId", verge.getId().toString())))
                .andExpect(jsonPath("$[*].title", contains("Gadgets")));

        // feedId takes precedence over folderId
        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10")
                        .param("folderId", news.getId().toString()).param("feedId", verge.getId().toString())))
                .andExpect(jsonPath("$[*].title", contains("Gadgets")));

        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10").param("search", " ELECTION ")))
                .andExpect(jsonPath("$[*].title", contains("Election results")));

        // The search also matches the description ("Description of Weather")
        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10").param("search", "of weather")))
                .andExpect(jsonPath("$[*].title", contains("Weather")));

        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10")
                        .param("search", "election").param("includeRead", "true")))
                .andExpect(jsonPath("$[*].title", contains("Read election", "Election results")));

        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10")
                        .param("search", "weather").param("folderId", news.getId().toString())))
                .andExpect(jsonPath("$[*].title", contains("Weather")));

        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10")
                        .param("search", "weather").param("feedId", bbc.getId().toString())))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void youtubeShortsCanBeHiddenByFolder() throws Exception {
        createFeeds();
        Folder videos = createFolder("Videos", user);
        Feed channel = createFeed("Channel", "http://youtube.example/rss", videos, user);
        createPost("Video", channel, user, false, now.minusDays(4), "https://www.youtube.com/watch?v=aaaaaaaaaaa");
        createPost("Short", channel, user, false, now.minusDays(3), "https://www.youtube.com/shorts/bbbbbbbbbbb");
        createPost("News short", bbc, user, false, now.minusDays(2), "https://www.youtube.com/shorts/ccccccccccc");
        createPost("Orphan short", verge, user, false, now.minusDays(1), "https://www.youtube.com/shorts/ddddddddddd");
        String hidden = videos.getId().toString();

        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10")))
                .andExpect(jsonPath("$[*].title", contains("Orphan short", "News short", "Short", "Video")));

        // Only the Shorts of the given folders are left out, in every view
        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10").param("hideShortsFolderIds", hidden)))
                .andExpect(jsonPath("$[*].title", contains("Orphan short", "News short", "Video")));
        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10")
                        .param("folderId", hidden).param("hideShortsFolderIds", hidden)))
                .andExpect(jsonPath("$[*].title", contains("Video")));
        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10")
                        .param("feedId", channel.getId().toString()).param("search", "o").param("hideShortsFolderIds", hidden)))
                .andExpect(jsonPath("$[*].title", contains("Video")));

        mockMvc.perform(auth(get("/api/posts/unreadCounts").param("hideShortsFolderIds", hidden)))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.folders." + hidden).value(1))
                .andExpect(jsonPath("$.feeds[?(@.title == 'Channel')].count", contains(1)))
                .andExpect(jsonPath("$.feeds[?(@.title == 'Verge')].count", contains(1)));
    }

    @Test
    void getPostsReturnsSavedPostsReadOrUnread() throws Exception {
        createFeeds();
        Post savedUnread = createPost("Saved unread", bbc, user, false, now.minusDays(1));
        Post savedRead = createPost("Saved read", cnn, user, true, now);
        createPost("Not saved", bbc, user, false, now);
        savedUnread.setSaved(true);
        savedRead.setSaved(true);
        postRepository.save(savedUnread);
        postRepository.save(savedRead);

        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10").param("saved", "true")))
                .andExpect(jsonPath("$[*].title", contains("Saved read", "Saved unread")))
                .andExpect(jsonPath("$[0].saved").value(true));

        mockMvc.perform(auth(get("/api/posts/getPosts").param("page", "1").param("pageSize", "10")
                        .param("saved", "true").param("search", "unread")))
                .andExpect(jsonPath("$[*].title", contains("Saved unread")));
    }

    @Test
    void getPostsRequiresPaging() throws Exception {
        mockMvc.perform(auth(get("/api/posts/getPosts"))).andExpect(status().isBadRequest());
    }

    @Test
    void unreadCountsAreGroupedByFeedAndFolder() throws Exception {
        createFeeds();
        createPost("A", bbc, user, false, now);
        createPost("B", bbc, user, false, now);
        createPost("C", cnn, user, false, now);
        createPost("D", cnn, user, true, now);
        Post saved = createPost("E", verge, user, true, now);
        saved.setSaved(true);
        postRepository.save(saved);

        mockMvc.perform(auth(get("/api/posts/unreadCounts")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.saved").value(1))
                .andExpect(jsonPath("$.folders." + news.getId()).value(3))
                .andExpect(jsonPath("$.feeds", hasSize(3)))
                .andExpect(jsonPath("$.feeds[?(@.title == 'BBC')].count", contains(2)))
                .andExpect(jsonPath("$.feeds[?(@.title == 'CNN')].count", contains(1)))
                .andExpect(jsonPath("$.feeds[?(@.title == 'Verge')].count", contains(0)))
                .andExpect(jsonPath("$.feeds[?(@.title == 'Verge')].folderId", contains((Object) null)))
                .andExpect(jsonPath("$.feeds[?(@.title == 'BBC')].folderId", contains(news.getId().toString())))
                .andExpect(jsonPath("$.feeds[?(@.title == 'BBC')].htmlUrl", contains("http://example.com/BBC")));
    }

    @Test
    void markAsReadMarksTheUsersPost() throws Exception {
        createFeeds();
        Post post = createPost("Post", bbc, user, false, now);

        mockMvc.perform(auth(put("/api/posts/markAsRead/" + post.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string("Post marked as read"));

        assertThat(postRepository.findById(post.getId()).orElseThrow().isRead()).isTrue();
    }

    @Test
    void markAsUnreadMarksTheUsersPost() throws Exception {
        createFeeds();
        Post post = createPost("Post", bbc, user, true, now);

        mockMvc.perform(auth(put("/api/posts/markAsUnread/" + post.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string("Post marked as unread"));

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updated.isRead()).isFalse();
        assertThat(updated.getReadAt()).isNull();
    }

    @Test
    void markAsUnreadFailsForForeignPosts() throws Exception {
        User other = createUser("other", "other@example.com", "secret123");
        Feed foreignFeed = createFeed("Foreign", "http://f.example/rss", null, other);
        Post foreign = createPost("Foreign", foreignFeed, other, true, now);

        mockMvc.perform(auth(put("/api/posts/markAsUnread/" + foreign.getId())))
                .andExpect(status().isBadRequest());

        assertThat(postRepository.findById(foreign.getId()).orElseThrow().isRead()).isTrue();
    }

    @Test
    void markAsReadFailsForMissingOrForeignPosts() throws Exception {
        User other = createUser("other", "other@example.com", "secret123");
        Feed foreignFeed = createFeed("Foreign", "http://f.example/rss", null, other);
        Post foreign = createPost("Foreign", foreignFeed, other, false, now);

        mockMvc.perform(auth(put("/api/posts/markAsRead/" + foreign.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to mark post as read"));

        mockMvc.perform(auth(put("/api/posts/markAsRead/" + UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        assertThat(postRepository.findById(foreign.getId()).orElseThrow().isRead()).isFalse();
    }

    @Test
    void markAllAsReadMarksEveryUnreadPost() throws Exception {
        createFeeds();
        createPost("A", bbc, user, false, now);
        createPost("B", verge, user, false, now);
        createPost("C", verge, user, true, now);
        User other = createUser("other", "other@example.com", "secret123");
        Feed foreignFeed = createFeed("Foreign", "http://f.example/rss", null, other);
        createPost("Foreign", foreignFeed, other, false, now);

        mockMvc.perform(auth(put("/api/posts/markAllAsRead")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        assertThat(postRepository.findByUserAndReadFalse(user)).isEmpty();
        assertThat(postRepository.findByUserAndReadFalse(other)).hasSize(1);
        // Only the posts marked now get a read date
        assertThat(postRepository.findAll()).filteredOn(p -> p.getReadAt() != null)
                .extracting(Post::getTitle).containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void deleteExpiredReadPostsDeletesOnlyPostsReadBeforeTheRetentionPeriod() {
        createFeeds();
        Post expired = createPost("Expired", bbc, user, true, now.minusDays(60));
        expired.setReadAt(now.minusDays(31));
        postRepository.save(expired);
        Post recent = createPost("Recent", bbc, user, true, now.minusDays(60));
        recent.setReadAt(now.minusDays(29));
        postRepository.save(recent);
        Post saved = createPost("Saved", bbc, user, true, now.minusDays(60));
        saved.setReadAt(now.minusDays(31));
        saved.setSaved(true);
        postRepository.save(saved);
        createPost("Unread", bbc, user, false, now.minusDays(60));
        createPost("No read date", bbc, user, true, now.minusDays(60));

        assertThat(postService.deleteExpiredReadPosts()).isEqualTo(1);

        assertThat(postRepository.findAll()).extracting(Post::getTitle)
                .containsExactlyInAnyOrder("Recent", "Saved", "Unread", "No read date");
        // Read before the date was kept, so it's deleted a full retention period from now
        assertThat(postRepository.findAll()).filteredOn(p -> p.getTitle().equals("No read date"))
                .allSatisfy(p -> assertThat(p.getReadAt()).isAfter(now.minusMinutes(1)));
    }

    @Test
    void markAllAsReadCanBeLimitedToAFolderFeedOrAge() throws Exception {
        createFeeds();
        Post bbcOld = createPost("BBC old", bbc, user, false, now.minusDays(10));
        Post bbcNew = createPost("BBC new", bbc, user, false, now);
        Post cnnNew = createPost("CNN new", cnn, user, false, now);
        Post vergeNew = createPost("Verge new", verge, user, false, now);

        mockMvc.perform(auth(put("/api/posts/markAllAsRead").param("feedId", bbc.getId().toString()).param("olderThanDays", "5")))
                .andExpect(jsonPath("$.count").value(1));
        assertThat(postRepository.findById(bbcOld.getId()).orElseThrow().isRead()).isTrue();
        assertThat(postRepository.findById(bbcNew.getId()).orElseThrow().isRead()).isFalse();

        mockMvc.perform(auth(put("/api/posts/markAllAsRead").param("folderId", news.getId().toString())))
                .andExpect(jsonPath("$.count").value(2));
        assertThat(postRepository.findById(cnnNew.getId()).orElseThrow().isRead()).isTrue();
        assertThat(postRepository.findById(vergeNew.getId()).orElseThrow().isRead()).isFalse();
    }

    @Test
    void savedFlagCanBeSetAndCleared() throws Exception {
        createFeeds();
        Post post = createPost("Post", bbc, user, false, now);

        mockMvc.perform(auth(put("/api/posts/saved/" + post.getId()).param("saved", "true")))
                .andExpect(status().isOk());
        assertThat(postRepository.findById(post.getId()).orElseThrow().isSaved()).isTrue();

        mockMvc.perform(auth(put("/api/posts/saved/" + post.getId()).param("saved", "false")))
                .andExpect(status().isOk());
        assertThat(postRepository.findById(post.getId()).orElseThrow().isSaved()).isFalse();
    }

    @Test
    void savingAMissingPostIsNotFound() throws Exception {
        mockMvc.perform(auth(put("/api/posts/saved/" + UUID.randomUUID()).param("saved", "true")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Post not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteAllKeepsSavedPostsAndOtherUsersPosts() throws Exception {
        createFeeds();
        createPost("A", bbc, user, false, now);
        createPost("B", bbc, user, true, now);
        Post saved = createPost("Saved", bbc, user, false, now);
        saved.setSaved(true);
        postRepository.save(saved);
        User other = createUser("other", "other@example.com", "secret123");
        Feed foreignFeed = createFeed("Foreign", "http://f.example/rss", null, other);
        createPost("Foreign", foreignFeed, other, false, now);

        mockMvc.perform(auth(delete("/api/posts/deleteAll")))
                .andExpect(status().isOk())
                .andExpect(content().string("All posts deleted."));

        assertThat(postRepository.findByUser(user)).extracting(Post::getTitle).containsExactly("Saved");
        assertThat(postRepository.findByUser(other)).hasSize(1);
    }

    @Test
    void contentReturnsStoredContentAndLogCount() throws Exception {
        createFeeds();
        Post post = createPost("Post", bbc, user, false, now);
        post.setContent(ArticleExtractor.VERSION_MARKER + "<p>A full article that is longer than the description of the post.</p>");
        postRepository.save(post);
        createLog(user, bbc.getId(), post.getId(), eu.apps4net.feedora.models.LogLevel.WARNING, now);

        mockMvc.perform(auth(get("/api/posts/content/" + post.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", containsString("A full article")))
                .andExpect(jsonPath("$.logCount").value(1));
    }

    @Test
    void contentIsExtractedFromThePostsPage() throws Exception {
        createFeeds();
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/article", 200, "text/html", articlePage());
            Post post = createPost("Post", bbc, user, false, now);
            post.setLink(server.url("/article"));
            postRepository.save(post);

            mockMvc.perform(auth(get("/api/posts/content/" + post.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", containsString("Readability needs a long enough article")))
                    .andExpect(jsonPath("$.logCount").value(0));

            assertThat(postRepository.findById(post.getId()).orElseThrow().getContent()).startsWith(ArticleExtractor.VERSION_MARKER);
        }
    }

    @Test
    void contentFallsBackToTheDescriptionAndLogsWhenThePageCantBeRead() throws Exception {
        createFeeds();
        try (TestHttpServer server = new TestHttpServer()) {
            Post post = createPost("Post", bbc, user, false, now);
            post.setLink(server.url("/missing"));
            postRepository.save(post);

            mockMvc.perform(auth(get("/api/posts/content/" + post.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("<p>Description of Post</p>"))
                    .andExpect(jsonPath("$.logCount").value(1));

            assertThat(logEntryRepository.findAll()).singleElement()
                    .satisfies(log -> assertThat(log.getType()).isEqualTo(LogType.CONTENT_EXTRACT));
        }
    }

    @Test
    void contentOfAForeignPostIsNotFound() throws Exception {
        User other = createUser("other", "other@example.com", "secret123");
        Feed foreignFeed = createFeed("Foreign", "http://f.example/rss", null, other);
        Post foreign = createPost("Foreign", foreignFeed, other, false, now);

        mockMvc.perform(auth(get("/api/posts/content/" + foreign.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void parseFeedsDownloadsNewPosts() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/rss", 200, "application/rss+xml", TestHttpServer.rss("Feed",
                    TestHttpServer.rssItem("First", "http://example.com/1", "First description"),
                    TestHttpServer.rssItem("Second", "http://example.com/2", "Second description")));
            Feed feed = createFeed("Feed", server.url("/rss"), null, user);
            Post existing = createPost("Existing", feed, user, true, now);
            existing.setLink("http://example.com/1");
            postRepository.save(existing);

            mockMvc.perform(auth(post("/api/posts/parseFeeds")))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Feeds parsed and posts updated."));

            assertThat(postRepository.findByUser(user)).extracting(Post::getTitle).containsExactlyInAnyOrder("Existing", "Second");
        }
    }

    @Test
    void parseFeedsRecordsStatisticsThatAreRemovedWithTheFeed() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/rss", 200, "application/rss+xml", TestHttpServer.rss("Feed",
                    TestHttpServer.rssItem("First", "http://example.com/1", "First description")));
            Feed feed = createFeed("Feed", server.url("/rss"), null, user);

            mockMvc.perform(auth(post("/api/posts/parseFeeds"))).andExpect(status().isOk());
            // Checked again: no new posts this time
            mockMvc.perform(auth(post("/api/posts/parseFeeds"))).andExpect(status().isOk());

            assertThat(feedStatisticsRepository.findAll()).singleElement().satisfies(stats -> {
                assertThat(stats.getFeed().getId()).isEqualTo(feed.getId());
                assertThat(stats.getTotalChecks()).isEqualTo(2);
                assertThat(stats.getTotalNewPosts()).isEqualTo(1);
                assertThat(stats.getLastNewPostsCount()).isZero();
                assertThat(stats.getNextCheckAt()).isNotNull();
            });

            mockMvc.perform(auth(post("/api/feed/deleteFeed").param("feedId", feed.getId().toString())))
                    .andExpect(status().isOk());

            assertThat(feedRepository.findAll()).isEmpty();
            assertThat(feedStatisticsRepository.findAll()).isEmpty();
        }
    }

    @Test
    void parseFeedsLogsFeedsThatCantBeDownloaded() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            Feed feed = createFeed("Broken", server.url("/missing"), null, user);

            mockMvc.perform(auth(post("/api/posts/parseFeeds"))).andExpect(status().isOk());

            assertThat(logEntryRepository.findAll()).filteredOn(log -> log.getType() != LogType.FEED_PARSING).singleElement().satisfies(log -> {
                assertThat(log.getFeedId()).isEqualTo(feed.getId());
                assertThat(log.getType()).isEqualTo(LogType.FEED_FETCH);
                assertThat(log.getMessage()).contains("HTTP 404");
            });
            // The summary of the parsing is logged too
            assertThat(logEntryRepository.findAll()).filteredOn(log -> log.getType() == LogType.FEED_PARSING).singleElement().satisfies(log -> {
                assertThat(log.getFeedId()).isNull();
                assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
                assertThat(log.getMessage()).contains("Parsed 1 feeds").contains("Failed: 1");
            });
        }
    }

    @Test
    void parseFeedsWithoutFeedsDoesNothing() throws Exception {
        mockMvc.perform(auth(post("/api/posts/parseFeeds")))
                .andExpect(status().isOk())
                .andExpect(content().string("Feeds parsed and posts updated."));
        mockMvc.perform(auth(get("/api/posts/getAllPosts"))).andExpect(jsonPath("$", hasSize(0)));
    }

    static String articlePage() {
        String paragraph = "<p>Readability needs a long enough article to recognise it as the main content of the page. "
                + "This paragraph is repeated, so the article is long enough, with commas, sentences, and words.</p>";
        return "<html><head><title>Article</title></head><body><nav>Menu</nav><article><h1>Article</h1>"
                + paragraph.repeat(6) + "</article><footer>Footer</footer></body></html>";
    }
}
