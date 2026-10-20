package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.LogEntry;
import eu.apps4net.feedora.models.LogLevel;
import eu.apps4net.feedora.models.LogType;
import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.support.ApiTestBase;
import eu.apps4net.feedora.support.TestHttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedControllerTest extends ApiTestBase {

    private static final String OPML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="1.0">
              <head><title>Subscriptions</title></head>
              <body>
                <outline text="News" title="News">
                  <outline type="rss" text="BBC" title="BBC" xmlUrl="http://bbc.example/rss" htmlUrl="http://bbc.example"/>
                  <outline type="rss" text="CNN" title="CNN" xmlUrl="http://cnn.example/rss" htmlUrl="http://cnn.example"/>
                </outline>
                <outline text="Tech">
                  <outline type="rss" text="Verge" title="Verge" xmlUrl="http://verge.example/rss" htmlUrl="http://verge.example"/>
                  <outline type="atom" text="Ignored" title="Ignored" xmlUrl="http://ignored.example/atom"/>
                </outline>
              </body>
            </opml>
            """;

    private MockMultipartFile opmlFile(String name, String content) {
        return new MockMultipartFile("file", name, "text/xml", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void importOpmlReplacesTheUsersFeedsAndFolders() throws Exception {
        Folder oldFolder = createFolder("Old", user);
        Feed oldFeed = createFeed("Old feed", "http://old.example/rss", oldFolder, user);
        createPost("Old post", oldFeed, user, false, LocalDateTime.now());

        mockMvc.perform(auth(multipart("/api/feed/importOPML").file(opmlFile("feeds.opml", OPML))))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully replaced existing feeds. Imported 3 new feeds."));

        List<Feed> feeds = feedRepository.findByUser(user);
        assertThat(feeds).extracting(Feed::getTitle).containsExactlyInAnyOrder("BBC", "CNN", "Verge");
        assertThat(feeds).extracting(Feed::getFolderName).containsExactlyInAnyOrder("News", "News", "Tech");
        assertThat(folderRepository.findAllByUser(user)).extracting(Folder::getName).containsExactlyInAnyOrder("News", "Tech");
        assertThat(postRepository.findByUser(user)).isEmpty();
    }

    @Test
    void importOpmlKeepsOtherUsersFeeds() throws Exception {
        User other = createUser("other", "other@example.com", "secret123");
        createFeed("Other feed", "http://other.example/rss", null, other);

        mockMvc.perform(auth(multipart("/api/feed/importOPML").file(opmlFile("feeds.xml", OPML))))
                .andExpect(status().isOk());

        assertThat(feedRepository.findByUser(other)).hasSize(1);
    }

    @Test
    void importOpmlValidatesTheFile() throws Exception {
        mockMvc.perform(auth(multipart("/api/feed/importOPML").file(opmlFile("feeds.opml", ""))))
                .andExpect(content().string("Error: No file uploaded"));

        mockMvc.perform(auth(multipart("/api/feed/importOPML").file(opmlFile("feeds.txt", OPML))))
                .andExpect(content().string("Error: File must be an OPML (.opml) or XML (.xml) file"));

        mockMvc.perform(auth(multipart("/api/feed/importOPML").file(opmlFile("feeds.opml", "<opml><body>"))))
                .andExpect(content().string(startsWith("Error importing OPML")));
    }

    @Test
    void exportOpmlReturnsTheUsersFeedsGroupedByFolder() throws Exception {
        Folder news = createFolder("News & Politics", user);
        createFolder("Empty", user);
        createFeed("BBC", "http://bbc.example/rss?a=1&b=2", news, user);
        createFeed("Loose", "http://loose.example/rss", null, user);
        createFeed("Theirs", "http://theirs.example/rss", null, createUser("other", "other@example.com", "secret123"));

        String opml = mockMvc.perform(auth(get("/api/feed/exportOPML")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(opml).contains("<outline text=\"News &amp; Politics\"", "xmlUrl=\"http://bbc.example/rss?a=1&amp;b=2\"",
                "<outline text=\"Empty\"", "xmlUrl=\"http://loose.example/rss\"");
        assertThat(opml).doesNotContain("theirs.example");

        // The exported file can be imported back without losing anything
        mockMvc.perform(auth(multipart("/api/feed/importOPML").file(opmlFile("feeds.opml", opml))))
                .andExpect(content().string("Successfully replaced existing feeds. Imported 2 new feeds."));
        List<Feed> feeds = feedRepository.findByUser(user);
        assertThat(feeds).extracting(Feed::getXmlUrl).containsExactlyInAnyOrder("http://bbc.example/rss?a=1&b=2", "http://loose.example/rss");
        assertThat(feeds).extracting(Feed::getFolderName).containsExactlyInAnyOrder("News & Politics", null);
        assertThat(folderRepository.findAllByUser(user)).extracting(Folder::getName).containsExactlyInAnyOrder("News & Politics", "Empty");
    }

    @Test
    void getAllFeedsReturnsOnlyTheUsersFeeds() throws Exception {
        Folder folder = createFolder("News", user);
        createFeed("Mine", "http://mine.example/rss", folder, user);
        createFeed("Theirs", "http://theirs.example/rss", null, createUser("other", "other@example.com", "secret123"));

        mockMvc.perform(auth(get("/api/feed/getAllFeeds")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Mine")))
                .andExpect(jsonPath("$[?(@.title == 'Mine')].folderName", contains("News")));
    }

    @Test
    void getFeedsPutsFeedsWithProblemsFirstAndCountsTheirLogs() throws Exception {
        Feed alpha = createFeed("Alpha", "http://alpha.example/rss", null, user);
        Feed beta = createFeed("Beta", "http://beta.example/rss", null, user);
        Feed gamma = createFeed("Gamma", "http://gamma.example/rss", null, user);
        createLog(user, gamma.getId(), null, LogLevel.ERROR, LocalDateTime.now());
        createLog(user, gamma.getId(), UUID.randomUUID(), LogLevel.WARNING, LocalDateTime.now());
        // Only log entries of its posts, which don't make it a problematic feed
        Feed delta = createFeed("Delta", "http://delta.example/rss", null, user);
        createLog(user, delta.getId(), UUID.randomUUID(), LogLevel.ERROR, LocalDateTime.now());

        mockMvc.perform(auth(get("/api/feed/getFeeds").param("page", "1").param("pageSize", "10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", contains("Gamma", "Alpha", "Beta", "Delta")))
                .andExpect(jsonPath("$[0].logCount").value(1))
                .andExpect(jsonPath("$[1].logCount").value(0))
                .andExpect(jsonPath("$[1].id").value(alpha.getId().toString()))
                .andExpect(jsonPath("$[3].logCount").value(0));

        mockMvc.perform(auth(get("/api/feed/getFeeds").param("page", "2").param("pageSize", "2")))
                .andExpect(jsonPath("$[*].id", contains(beta.getId().toString(), delta.getId().toString())));
    }

    @Test
    void getFeedsSearchesByTitle() throws Exception {
        createFeed("Tech News", "http://a.example/rss", null, user);
        createFeed("Sports", "http://b.example/rss", null, user);
        createFeed("Other tech", "http://c.example/rss", null, createUser("other", "other@example.com", "secret123"));

        mockMvc.perform(auth(get("/api/feed/getFeeds").param("page", "1").param("pageSize", "10").param("search", " TECH ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", contains("Tech News")));
    }

    @Test
    void feedOperationStatusIsNotRunning() throws Exception {
        mockMvc.perform(auth(get("/api/feed/feedOperationStatus")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRunning").value(false))
                .andExpect(jsonPath("$.runCount").isNumber());
    }

    @Test
    void addFeedWithTitleSavesIt() throws Exception {
        Folder folder = createFolder("News", user);
        String body = "{\"url\":\"http://site.example/rss\",\"folderId\":\"" + folder.getId() + "\",\"title\":\"My feed\"}";

        mockMvc.perform(auth(post("/api/feed/addFeed")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("Feed added successfully"));

        Feed feed = feedRepository.findByXmlUrlAndUser("http://site.example/rss", user).orElseThrow();
        assertThat(feed.getTitle()).isEqualTo("My feed");
        assertThat(feed.getFolderId()).isEqualTo(folder.getId());
        assertThat(feed.getType()).isEqualTo("rss");
    }

    @Test
    void addFeedWithoutTitleReadsItFromTheFeed() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/rss", 200, "application/rss+xml", TestHttpServer.rss("Remote title"));
            String body = "{\"url\":\"" + server.url("/rss") + "\"}";

            mockMvc.perform(auth(post("/api/feed/addFeed")).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());

            Feed feed = feedRepository.findByXmlUrlAndUser(server.url("/rss"), user).orElseThrow();
            assertThat(feed.getTitle()).isEqualTo("Remote title");
            assertThat(feed.getFolder()).isNull();
        }
    }

    @Test
    void addFeedFallsBackToTheUrlAndLogsWhenTheTitleCantBeRead() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            String url = server.url("/missing");

            mockMvc.perform(auth(post("/api/feed/addFeed")).contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"" + url + "\"}"))
                    .andExpect(status().isOk());

            Feed feed = feedRepository.findByXmlUrlAndUser(url, user).orElseThrow();
            assertThat(feed.getTitle()).isEqualTo(url);
            assertThat(logEntryRepository.findAll()).singleElement().satisfies(log -> {
                assertThat(log.getType()).isEqualTo(LogType.FEED_INFO);
                assertThat(log.getLevel()).isEqualTo(LogLevel.WARNING);
                assertThat(log.getFeedId()).isEqualTo(feed.getId());
            });
        }
    }

    @Test
    void addFeedRejectsDuplicatesAndForeignFolders() throws Exception {
        createFeed("Existing", "http://site.example/rss", null, user);
        Folder foreign = createFolder("Foreign", createUser("other", "other@example.com", "secret123"));

        mockMvc.perform(auth(post("/api/feed/addFeed")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"http://site.example/rss\",\"title\":\"T\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Feed already exists")));

        mockMvc.perform(auth(post("/api/feed/addFeed")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"http://new.example/rss\",\"title\":\"T\",\"folderId\":\"" + foreign.getId() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Folder not found or doesn't belong to user")));
    }

    @Test
    void deleteFeedRemovesItsPostsAndLogs() throws Exception {
        Feed feed = createFeed("Feed", "http://site.example/rss", null, user);
        Feed kept = createFeed("Kept", "http://kept.example/rss", null, user);
        createPost("Post", feed, user, false, LocalDateTime.now());
        Post keptPost = createPost("Kept post", kept, user, false, LocalDateTime.now());
        createLog(user, feed.getId(), null, LogLevel.ERROR, LocalDateTime.now());
        LogEntry keptLog = createLog(user, kept.getId(), null, LogLevel.ERROR, LocalDateTime.now());

        mockMvc.perform(auth(post("/api/feed/deleteFeed").param("feedId", feed.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(content().string("Feed deleted successfully"));

        assertThat(feedRepository.findAll()).extracting(Feed::getId).containsExactly(kept.getId());
        assertThat(postRepository.findAll()).extracting(Post::getId).containsExactly(keptPost.getId());
        assertThat(logEntryRepository.findAll()).extracting(LogEntry::getId).containsExactly(keptLog.getId());
    }

    @Test
    void deleteFeedRejectsInvalidMissingAndForeignFeeds() throws Exception {
        Feed foreign = createFeed("Foreign", "http://f.example/rss", null, createUser("other", "other@example.com", "secret123"));

        mockMvc.perform(auth(post("/api/feed/deleteFeed").param("feedId", "nope")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid feed ID")));

        mockMvc.perform(auth(post("/api/feed/deleteFeed").param("feedId", UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Feed not found")));

        mockMvc.perform(auth(post("/api/feed/deleteFeed").param("feedId", foreign.getId().toString())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Feed doesn't belong to user")));

        assertThat(feedRepository.findById(foreign.getId())).isPresent();
    }

    @Test
    void fetchFeedInfoReadsRssTitle() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/rss", 200, "application/rss+xml", TestHttpServer.rss("RSS title"));

            mockMvc.perform(auth(get("/api/feed/fetchFeedInfo").param("url", server.url("/rss"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("RSS title"))
                    .andExpect(jsonPath("$.description").value("Feed description"))
                    .andExpect(jsonPath("$.url").value(server.url("/rss")));
        }
    }

    @Test
    void fetchFeedInfoReadsAtomTitle() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/atom", 200, "application/atom+xml", """
                    <?xml version="1.0" encoding="utf-8"?>
                    <feed xmlns="http://www.w3.org/2005/Atom">
                      <entry><title>Entry title</title></entry>
                      <title>Atom title</title>
                    </feed>
                    """);

            mockMvc.perform(auth(get("/api/feed/fetchFeedInfo").param("url", server.url("/atom"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Atom title"));
        }
    }

    @Test
    void fetchFeedInfoUsesAPlaceholderWhenTheFeedHasNoTitle() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/rss", 200, "application/rss+xml", "<rss><channel></channel></rss>");

            mockMvc.perform(auth(get("/api/feed/fetchFeedInfo").param("url", server.url("/rss"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Untitled Feed"));
        }
    }

    @Test
    void fetchFeedInfoReportsHttpErrors() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            mockMvc.perform(auth(get("/api/feed/fetchFeedInfo").param("url", server.url("/missing"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", containsString("HTTP 404")));
        }
    }

    @Test
    void updateFeedChangesTitleAndMovesItToANewFolder() throws Exception {
        Feed feed = createFeed("Old", "http://site.example/rss", createFolder("News", user), user);
        String body = "{\"feedId\":\"" + feed.getId() + "\",\"title\":\" New title \",\"folderName\":\" Tech \"}";

        mockMvc.perform(auth(post("/api/feed/updateFeed")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("Feed updated successfully"));

        Feed updated = feedRepository.findById(feed.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("New title");
        assertThat(updated.getFolderName()).isEqualTo("Tech");
        assertThat(folderRepository.findAllByUser(user)).extracting(Folder::getName).containsExactlyInAnyOrder("News", "Tech");
    }

    @Test
    void updateFeedReusesAnExistingFolderAndCanRemoveTheFolder() throws Exception {
        Folder news = createFolder("News", user);
        Feed feed = createFeed("Feed", "http://site.example/rss", null, user);

        mockMvc.perform(auth(post("/api/feed/updateFeed")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedId\":\"" + feed.getId() + "\",\"folderName\":\"News\"}"))
                .andExpect(status().isOk());
        Feed updated = feedRepository.findById(feed.getId()).orElseThrow();
        assertThat(updated.getFolderId()).isEqualTo(news.getId());
        assertThat(updated.getTitle()).isEqualTo("Feed");
        assertThat(folderRepository.findAllByUser(user)).hasSize(1);

        mockMvc.perform(auth(post("/api/feed/updateFeed")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedId\":\"" + feed.getId() + "\",\"folderName\":\"\"}"))
                .andExpect(status().isOk());
        assertThat(feedRepository.findById(feed.getId()).orElseThrow().getFolder()).isNull();
    }

    @Test
    void updateFeedSetsKeepsAndRemovesTheIconBackgroundColor() throws Exception {
        Feed feed = createFeed("Feed", "http://site.example/rss", null, user);

        mockMvc.perform(auth(post("/api/feed/updateFeed")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedId\":\"" + feed.getId() + "\",\"iconBackgroundColor\":\"#FFAA00\"}"))
                .andExpect(status().isOk());
        assertThat(feedRepository.findById(feed.getId()).orElseThrow().getIconBackgroundColor()).isEqualTo("#ffaa00");

        // Omitted: the color is kept
        mockMvc.perform(auth(post("/api/feed/updateFeed")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedId\":\"" + feed.getId() + "\",\"title\":\"Renamed\"}"))
                .andExpect(status().isOk());
        assertThat(feedRepository.findById(feed.getId()).orElseThrow().getIconBackgroundColor()).isEqualTo("#ffaa00");

        mockMvc.perform(auth(post("/api/feed/updateFeed")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedId\":\"" + feed.getId() + "\",\"iconBackgroundColor\":\"red\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid color")));

        mockMvc.perform(auth(post("/api/feed/updateFeed")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedId\":\"" + feed.getId() + "\",\"iconBackgroundColor\":\"\"}"))
                .andExpect(status().isOk());
        assertThat(feedRepository.findById(feed.getId()).orElseThrow().getIconBackgroundColor()).isNull();
    }

    @Test
    void updateFeedRejectsForeignFeeds() throws Exception {
        Feed foreign = createFeed("Foreign", "http://f.example/rss", null, createUser("other", "other@example.com", "secret123"));

        mockMvc.perform(auth(post("/api/feed/updateFeed")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedId\":\"" + foreign.getId() + "\",\"title\":\"Mine\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Feed doesn't belong to user")));

        mockMvc.perform(auth(post("/api/feed/updateFeed")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedId\":\"bad\",\"title\":\"Mine\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid feed ID")));
    }
}
