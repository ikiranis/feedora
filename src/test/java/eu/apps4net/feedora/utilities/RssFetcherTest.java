package eu.apps4net.feedora.utilities;

import com.rometools.rome.feed.synd.SyndEntry;
import eu.apps4net.feedora.exceptions.FeedFetchException;
import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.LogType;
import eu.apps4net.feedora.services.FeedService;
import eu.apps4net.feedora.support.TestHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RssFetcherTest {
    private TestHttpServer server;
    private FeedService feedService;

    @BeforeEach
    void setUp() throws Exception {
        server = new TestHttpServer();
        feedService = mock(FeedService.class);
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private Feed feed(String path) {
        return new Feed("Feed", server.url(path), "", "rss", null, null);
    }

    @Test
    void fetchesRssEntries() throws Exception {
        server.respond("/rss", 200, "application/rss+xml", TestHttpServer.rss("Feed",
                TestHttpServer.rssItem("First", "http://example.com/1", "One"),
                TestHttpServer.rssItem("Second", "http://example.com/2", "Two")));

        List<SyndEntry> entries = RssFetcher.fetch(feed("/rss"), feedService);

        assertThat(entries).extracting(SyndEntry::getTitle).containsExactly("First", "Second");
        assertThat(entries.get(0).getLink()).isEqualTo("http://example.com/1");
        assertThat(entries.get(0).getDescription().getValue()).isEqualTo("One");
        assertThat(entries.get(0).getPublishedDate()).isNotNull();
        verify(feedService, never()).removeInvalidFeed(any(), anyString());
    }

    @Test
    void fetchesAtomEntries() throws Exception {
        server.respond("/atom", 200, "application/atom+xml", """
                <?xml version="1.0" encoding="utf-8"?>
                <feed xmlns="http://www.w3.org/2005/Atom"><title>Atom</title><id>urn:feed</id><updated>2025-01-01T00:00:00Z</updated>
                  <entry><title>Entry</title><id>urn:1</id><link href="http://example.com/entry"/><updated>2025-01-01T00:00:00Z</updated></entry>
                </feed>
                """);

        List<SyndEntry> entries = RssFetcher.fetch(feed("/atom"), feedService);

        assertThat(entries).singleElement().satisfies(entry -> {
            assertThat(entry.getTitle()).isEqualTo("Entry");
            assertThat(entry.getLink()).isEqualTo("http://example.com/entry");
        });
    }

    @Test
    void removesControlCharactersAndAddsTheXmlDeclaration() throws Exception {
        server.respond("/rss", 200, "application/rss+xml",
                "<rss version=\"2.0\"><channel><title>Feed</title>"
                        + "<item><title>Bad\u0001Title</title><link>http://example.com/1</link></item></channel></rss>");

        assertThat(RssFetcher.fetch(feed("/rss"), feedService)).extracting(SyndEntry::getTitle).containsExactly("BadTitle");
    }

    @Test
    void followsRedirects() throws Exception {
        server.redirect("/old", server.url("/rss"));
        server.respond("/rss", 200, "application/rss+xml", TestHttpServer.rss("Feed",
                TestHttpServer.rssItem("Moved", "http://example.com/1", "One")));

        assertThat(RssFetcher.fetch(feed("/old"), feedService)).extracting(SyndEntry::getTitle).containsExactly("Moved");
    }

    @Test
    void httpErrorsAreFetchErrors() {
        assertThatThrownBy(() -> RssFetcher.fetch(feed("/missing"), feedService))
                .isInstanceOfSatisfying(FeedFetchException.class, e -> {
                    assertThat(e.getType()).isEqualTo(LogType.FEED_FETCH);
                    assertThat(e.getMessage()).startsWith("HTTP 404");
                });
    }

    @Test
    void invalidXmlIsAParseErrorWithASnippet() {
        server.respond("/rss", 200, "application/xml", "<?xml version=\"1.0\"?><notafeed><item/></notafeed>");

        assertThatThrownBy(() -> RssFetcher.fetch(feed("/rss"), feedService))
                .isInstanceOfSatisfying(FeedFetchException.class, e -> {
                    assertThat(e.getType()).isEqualTo(LogType.FEED_PARSE);
                    assertThat(e.getMessage()).startsWith("Invalid feed");
                    assertThat(e.getDetails()).startsWith("XML snippet:").contains("notafeed");
                    assertThat(e.getCause()).isNotNull();
                });
    }

    @Test
    void htmlPagesRemoveTheFeed() throws Exception {
        server.respond("/page", 200, "text/html; charset=utf-8", "<!DOCTYPE html><html><body>Not a feed</body></html>");
        Feed feed = feed("/page");

        assertThat(RssFetcher.fetch(feed, feedService)).isEmpty();
        verify(feedService).removeInvalidFeed(feed, "The feed URL returned an HTML page instead of a feed");
    }

    @Test
    void htmlContentIsDetectedEvenWithAnXmlContentType() throws Exception {
        server.respond("/page", 200, "application/xml", "<html><body>Not a feed</body></html>");

        assertThat(RssFetcher.fetch(feed("/page"), null)).isEmpty();
    }
}
