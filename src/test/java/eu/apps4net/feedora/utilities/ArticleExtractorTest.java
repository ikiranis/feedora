package eu.apps4net.feedora.utilities;

import eu.apps4net.feedora.support.TestHttpServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleExtractorTest {

    private static final String PARAGRAPH = "<p>Readability needs a long enough article to recognise it as the main content of the page. "
            + "This paragraph is repeated, so the article is long enough, with commas, sentences, and words.</p>";

    // sanitize() pretty-prints block elements, remove the formatting whitespace to compare the markup
    private static String compact(String html) {
        return html.replaceAll("\\s*\n\\s*", "");
    }

    @Test
    void sanitizeReturnsNullForEmptyContent() {
        assertThat(ArticleExtractor.sanitize(null, null)).isNull();
        assertThat(ArticleExtractor.sanitize("  ", null)).isNull();
        assertThat(ArticleExtractor.sanitize("<div><span> </span></div>", null)).isNull();
    }

    @Test
    void sanitizeTurnsPlainTextIntoParagraphs() {
        String html = ArticleExtractor.sanitize("First line\nsecond line\n\nSecond & more > less", null);

        assertThat(compact(html)).isEqualTo("<p>First line<br>second line</p><p>Second &amp; more &gt; less</p>");
    }

    @Test
    void sanitizeRemovesScriptsStylesAndEventHandlers() {
        String html = ArticleExtractor.sanitize(
                "<p onclick=\"evil()\" style=\"color:red\">Text</p><script>alert(1)</script><style>p{}</style><iframe src=\"x\"></iframe>",
                "http://example.com/");

        assertThat(html).isEqualTo("<p>Text</p>");
    }

    @Test
    void sanitizeMakesLinksAbsoluteAndOpensThemInANewTab() {
        String html = ArticleExtractor.sanitize("<p><a href=\"/page\">Link</a> <img src=\"img.png\"></p>", "http://example.com/dir/post");

        assertThat(html).contains("href=\"http://example.com/page\"")
                .contains("target=\"_blank\"")
                .contains("rel=\"noopener noreferrer\"")
                .contains("src=\"http://example.com/dir/img.png\"");
    }

    @Test
    void sanitizeKeepsImageOnlyContent() {
        assertThat(ArticleExtractor.sanitize("<img src=\"http://example.com/a.png\">", null))
                .isEqualTo("<img src=\"http://example.com/a.png\">");
    }

    @Test
    void sanitizeKeepsFigures() {
        String html = ArticleExtractor.sanitize("<figure><img src=\"http://e.com/a.png\"><figcaption>Cap</figcaption></figure><hr>", null);

        assertThat(compact(html))
                .isEqualTo("<figure><img src=\"http://e.com/a.png\"><figcaption>Cap</figcaption></figure><hr>");
    }

    @Test
    void textLengthCountsTheVisibleText() {
        assertThat(ArticleExtractor.textLength(null)).isZero();
        assertThat(ArticleExtractor.textLength("<p>Hello <b>world</b></p>")).isEqualTo("Hello world".length());
    }

    @Test
    void extractFromUrlReturnsTheArticleAndAddsTheLeadImage() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/article", 200, "text/html", "<html><head><title>T</title>"
                    + "<meta property=\"og:image\" content=\"/lead.jpg\"></head><body><nav>Menu links</nav>"
                    + "<article><h1>Title</h1>" + PARAGRAPH.repeat(6) + "</article><footer>Footer</footer></body></html>");

            String content = ArticleExtractor.extractFromUrl(server.url("/article"));

            assertThat(content).startsWith("<figure><img src=\"" + server.url("/lead.jpg") + "\" alt=\"\"></figure>");
            assertThat(content).contains("Readability needs a long enough article").doesNotContain("Menu links");
        }
    }

    @Test
    void extractFromUrlFixesLazyLoadedImages() throws Exception {
        // Absolute image URLs, because Readability4J drops the port of the test server when resolving relative ones
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/article", 200, "text/html", "<html><head><title>T</title>"
                    + "<meta property=\"og:image\" content=\"http://example.com/lead.jpg\"></head><body><article><h1>Title</h1>"
                    + PARAGRAPH.repeat(3)
                    + "<p><img src=\"data:image/gif;base64,R0lGOD\" data-src=\"http://images.example/real.jpg\" alt=\"Photo\"></p>"
                    + "<p><img srcset=\"http://images.example/small.jpg 300w, http://images.example/big.jpg 600w\" alt=\"Other\"></p>"
                    + PARAGRAPH.repeat(3) + "</article></body></html>");

            String content = ArticleExtractor.extractFromUrl(server.url("/article"));

            assertThat(content).contains("src=\"http://images.example/real.jpg\"")
                    .contains("src=\"http://images.example/small.jpg\"")
                    .doesNotContain("big.jpg")
                    .doesNotContain("data:image")
                    // The page already has images, so the lead image isn't added
                    .doesNotContain("lead.jpg");
        }
    }

    @Test
    void sanitizeKeepsYoutubePlayersMadeFromTheVideoIdOnly() {
        String html = ArticleExtractor.sanitize("<p>Text</p>"
                + "<iframe src=\"https://www.youtube.com/embed/dQw4w9WgXcQ?si=abc\" onload=\"evil()\" width=\"560\"></iframe>"
                + "<iframe data-src=\"//www.youtube-nocookie.com/embed/aBcDeFgHiJk\"></iframe>"
                + "<iframe src=\"https://evil.example/embed/dQw4w9WgXcQ\"></iframe>", "http://example.com/");

        assertThat(html).contains("<div class=\"embedded-video\"><iframe src=\"https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ\"")
                .contains("src=\"https://www.youtube-nocookie.com/embed/aBcDeFgHiJk\"")
                .doesNotContain("evil")
                .doesNotContain("si=abc")
                .doesNotContain("width");
        assertThat(html.split("<iframe", -1)).hasSize(3);
    }

    @Test
    void sanitizeKeepsVideoOnlyContentAndLinksToEmbedUrls() {
        assertThat(ArticleExtractor.sanitize("<iframe src=\"https://www.youtube.com/embed/dQw4w9WgXcQ\"></iframe>", null))
                .contains("<iframe");
        // A link with text stays a link
        assertThat(ArticleExtractor.sanitize("<a href=\"https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ\">Video</a>", null))
                .doesNotContain("<iframe");
    }

    @Test
    void extractFromUrlKeepsEmbeddedYoutubeVideos() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/article", 200, "text/html", "<html><head><title>T</title></head><body><article><h1>Title</h1>"
                    + PARAGRAPH.repeat(3)
                    + "<p><iframe src=\"https://www.youtube.com/embed/dQw4w9WgXcQ\" allowfullscreen></iframe></p>"
                    + PARAGRAPH.repeat(3) + "</article></body></html>");

            String content = ArticleExtractor.extractFromUrl(server.url("/article"));

            assertThat(content).contains("<iframe src=\"https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ\"");
        }
    }

    @Test
    void extractFromUrlFailsWhenThePageCantBeDownloaded() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            assertThatThrownBy(() -> ArticleExtractor.extractFromUrl(server.url("/missing"))).isInstanceOf(Exception.class);
        }
    }
}
