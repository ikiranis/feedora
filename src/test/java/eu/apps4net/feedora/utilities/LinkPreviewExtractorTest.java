package eu.apps4net.feedora.utilities;

import eu.apps4net.feedora.support.TestHttpServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinkPreviewExtractorTest {

    @Test
    void recognisesTweets() {
        assertThat(LinkPreviewExtractor.isTweet("https://x.com/AP/status/1")).isTrue();
        assertThat(LinkPreviewExtractor.isTweet("https://mobile.twitter.com/AP/status/1")).isTrue();
        assertThat(LinkPreviewExtractor.isTweet("https://example.com/x.com")).isFalse();
        assertThat(LinkPreviewExtractor.isTweet("not a url")).isFalse();
    }

    @Test
    void findsFirstLinkSkippingTwitterLinks() {
        String description = "<p>Quoted https://x.com/AP/status/1<br/>➡️https://shorturl.at/rE58o<br/><br/>#slgr https://b.example/</p>";

        assertThat(LinkPreviewExtractor.findLink(description)).isEqualTo(new LinkPreviewExtractor.FoundLink("https://shorturl.at/rE58o", null));
    }

    @Test
    void prefersTwitterLinkCard() {
        String description = "<p>Review https://a.example/</p><p><a href=\"https://x.com/CNET\">@CNET</a></p>"
                + "<p><a href=\"https://t.co/w8mAcJSEuv\">Lenovo Yoga Review - CNET</a></p>";

        assertThat(LinkPreviewExtractor.findLink(description))
                .isEqualTo(new LinkPreviewExtractor.FoundLink("https://t.co/w8mAcJSEuv", "Lenovo Yoga Review - CNET"));
    }

    @Test
    void skipsAnchorsWhoseTextIsALink() {
        assertThat(LinkPreviewExtractor.findLink("<p><a href=\"https://t.co/a\">https://b.example/page</a></p>"))
                .isEqualTo(new LinkPreviewExtractor.FoundLink("https://b.example/page", null));
    }

    @Test
    void removesEllipsisAndPunctuationAfterLink() {
        assertThat(LinkPreviewExtractor.findLink("<p>Text https://a.example/article/1032039/slug-cut…</p>").url())
                .isEqualTo("https://a.example/article/1032039/slug-cut");
        assertThat(LinkPreviewExtractor.findLink("Read (https://a.example/page).").url()).isEqualTo("https://a.example/page");
    }

    @Test
    void findsNoLinkInTextWithout() {
        assertThat(LinkPreviewExtractor.findLink("<p>No link <img src=\"https://pbs.twimg.com/media/a.jpg\"></p>")).isNull();
        assertThat(LinkPreviewExtractor.findLink(null)).isNull();
    }

    @Test
    void readsTitleAndImageOfPageAfterRedirect() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.redirect("/short", "/article");
            server.respond("/article", 200, "text/html; charset=utf-8",
                    "<html><head><title>Fallback</title><meta property=\"og:title\" content=\" Article title \">"
                            + "<meta property=\"og:image\" content=\"/img.jpg\"></head><body></body></html>");

            LinkPreviewExtractor.LinkPreview preview = LinkPreviewExtractor.fetch(server.url("/short"));

            assertThat(preview.url()).isEqualTo(server.url("/article"));
            assertThat(preview.title()).isEqualTo("Article title");
            assertThat(preview.image()).isEqualTo(server.url("/img.jpg"));
        }
    }

    @Test
    void followsMetaRefresh() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/t", 200, "text/html", "<html><head><meta http-equiv=\"refresh\" content=\"0;URL=/article\"><title>/article</title></head></html>");
            server.respond("/article", 200, "text/html", "<html><head><meta property=\"og:title\" content=\"Article title\"></head></html>");

            LinkPreviewExtractor.LinkPreview preview = LinkPreviewExtractor.fetch(server.url("/t"));

            assertThat(preview.url()).isEqualTo(server.url("/article"));
            assertThat(preview.title()).isEqualTo("Article title");
        }
    }

    @Test
    void fallsBackToTitleTagAndReturnsNullForEmptyPage() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/titled", 200, "text/html", "<html><head><title>Page title</title></head></html>");
            server.respond("/empty", 200, "text/html", "<html><body>Text</body></html>");

            LinkPreviewExtractor.LinkPreview preview = LinkPreviewExtractor.fetch(server.url("/titled"));
            assertThat(preview.title()).isEqualTo("Page title");
            assertThat(preview.image()).isNull();
            assertThat(LinkPreviewExtractor.fetch(server.url("/empty"))).isNull();
        }
    }

    @Test
    void throwsForMissingPage() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/missing", 404, "text/html", "Not found");

            assertThatThrownBy(() -> LinkPreviewExtractor.fetch(server.url("/missing"))).isInstanceOf(Exception.class);
        }
    }
}
