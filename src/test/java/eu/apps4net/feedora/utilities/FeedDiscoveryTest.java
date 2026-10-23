package eu.apps4net.feedora.utilities;

import eu.apps4net.feedora.support.TestHttpServer;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeedDiscoveryTest {

    @Test
    void findsTheFeedsThePageLinksTo() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/blog", 200, "text/html", "<html><head>"
                    + "<link rel=\"alternate\" type=\"application/rss+xml\" title=\"Posts\" href=\"/blog/feed\">"
                    + "<link rel=\"alternate\" type=\"application/atom+xml\" href=\"https://b.example/comments.atom\">"
                    + "<link rel=\"alternate\" hreflang=\"el\" href=\"/el/\">"
                    + "<link rel=\"stylesheet\" type=\"text/css\" href=\"/style.css\">"
                    + "</head><body></body></html>");

            assertThat(FeedDiscovery.discover(server.url("/blog"))).containsExactly(
                    new FeedDiscovery.DiscoveredFeed(server.url("/blog/feed"), "Posts"),
                    new FeedDiscovery.DiscoveredFeed("https://b.example/comments.atom", null));
        }
    }

    @Test
    void skipsDuplicateLinks() {
        var page = Jsoup.parse("<link rel=\"Alternate\" type=\"application/RSS+xml\" href=\"/feed\">"
                + "<link rel=\"alternate\" type=\"application/rss+xml\" href=\"https://a.example/feed\">", "https://a.example/");

        assertThat(FeedDiscovery.linkedFeeds(page))
                .containsExactly(new FeedDiscovery.DiscoveredFeed("https://a.example/feed", null));
    }

    @Test
    void triesTheUsualPathsWhenThePageHasNoFeedLinks() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/about", 200, "text/html", "<html><head><title>About</title></head></html>")
                    .respond("/feed", 200, "text/html", "<html><body>Not a feed</body></html>")
                    .redirect("/rss", "/rss.xml")
                    .respond("/rss.xml", 200, "application/xml", TestHttpServer.rss("Site"))
                    .respond("/atom.xml", 200, "application/atom+xml",
                            "<?xml version=\"1.0\"?><feed xmlns=\"http://www.w3.org/2005/Atom\"><title>Site</title></feed>");

            assertThat(FeedDiscovery.discover(server.url("/about"))).containsExactly(
                    new FeedDiscovery.DiscoveredFeed(server.url("/rss.xml"), null),
                    new FeedDiscovery.DiscoveredFeed(server.url("/atom.xml"), null));
        }
    }

    @Test
    void findsNothingOnAPageWithoutFeeds() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/", 200, "text/html", "<html><body>Hello</body></html>");

            assertThat(FeedDiscovery.discover(server.url("/"))).isEqualTo(List.of());
        }
    }
}
