package eu.apps4net.feedora.utilities;

import eu.apps4net.feedora.support.TestHttpServer;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void replacesCutLinksWithTheirFullUrl() {
        String description = "<p>Title<br/>https://amna.gr/macedonia/article/1034549/dimografiko-problimarn… and https://x.com/AP/status/1</p>";
        String html = LinkPreviewExtractor.withFullLinks(description,
                List.of("https://x.com/AP/status/1", "https://www.amna.gr/macedonia/article/1034549/dimografiko-problima"));

        assertThat(html).isEqualTo("<p>Title<br><a href=\"https://www.amna.gr/macedonia/article/1034549/dimografiko-problima\" target=\"_blank\" "
                + "rel=\"noopener noreferrer\">https://www.amna.gr/macedonia/article/1034549/dimografiko-problima</a> and https://x.com/AP/status/1</p>");
        assertThat(LinkPreviewExtractor.hasCutLink(html)).isFalse();
        assertThat(LinkPreviewExtractor.findLink(html).url()).isEqualTo("https://www.amna.gr/macedonia/article/1034549/dimografiko-problima");
    }

    @Test
    void keepsCutLinksWithoutAFullUrlToTheSameSite() {
        String description = "<p>Text https://a.example/article/slug-cu…</p>";

        assertThat(LinkPreviewExtractor.hasCutLink(description)).isTrue();
        assertThat(LinkPreviewExtractor.withFullLinks(description, List.of("https://b.example/article/slug-cut"))).isEqualTo(description);
        assertThat(LinkPreviewExtractor.hasCutLink("<p>Text https://a.example/article</p>")).isFalse();
    }

    @Test
    void findsNoLinkInTextWithout() {
        assertThat(LinkPreviewExtractor.findLink("<p>No link <img src=\"https://pbs.twimg.com/media/a.jpg\"></p>")).isNull();
        assertThat(LinkPreviewExtractor.findLink(null)).isNull();
    }

    @Test
    void tweetContentAddsPhotosTheDescriptionDoesntShow() {
        String content = LinkPreviewExtractor.tweetContent("<p>Text <img src=\"https://pbs.twimg.com/media/a.jpg?format=jpg&amp;name=900x900\"></p>",
                List.of("https://pbs.twimg.com/media/a.jpg?format=jpg&name=900x900", "https://pbs.twimg.com/media/b.jpg",
                        "https://pbs.twimg.com/card_img/1/c?format=jpg"),
                "https://x.com/AP/status/1");

        assertThat(content).isEqualTo("<p>Text <img src=\"https://pbs.twimg.com/media/a.jpg?format=jpg&amp;name=900x900\"></p>"
                + "<div class=\"tweet-photos\"><img src=\"https://pbs.twimg.com/media/b.jpg\" alt=\"\"></div>");
        assertThat(LinkPreviewExtractor.tweetContent("<p>Text</p>", List.of(), null)).isEqualTo("<p>Text</p>");
        assertThat(LinkPreviewExtractor.tweetContent(null, List.of(), null)).isNull();
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
