package eu.apps4net.feedora.utilities;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import eu.apps4net.feedora.exceptions.PageBlockedException;
import eu.apps4net.feedora.support.TestHttpServer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageParserTest {
    private static final String NEWS_PAGE = "<html><head><title>News</title></head><body>"
            + "<nav><ul><li><a href=\"/a\">Home page link</a></li><li><a href=\"/b\">About us page</a></li>"
            + "<li><a href=\"/c\">Contact us page</a></li></ul></nav>"
            + "<div id=\"main\">"
            + "<article class=\"card post\"><h2><a href=\"/news/1\">First headline of the day</a></h2>"
            + "<img data-src=\"/img/1.jpg\" src=\"data:image/gif;base64,R0lGOD\"><p>The first story is about something.</p>"
            + "<time datetime=\"2026-10-01T10:00:00Z\">1 Oct</time></article>"
            + "<article class=\"card post\"><h2><a href=\"/news/2\">Second headline of the day</a></h2><p>The second story is about something.</p></article>"
            + "<article class=\"card post featured\"><a href=\"/news/3\"><h2>Third headline of the day</h2></a></article>"
            + "</div><footer><a href=\"/privacy\">Privacy policy page</a></footer></body></html>";

    private static Document page(String html) {
        return Jsoup.parse(html, "https://news.example/");
    }

    @Test
    void findsTheRepeatedPostsOutsideTheNavigation() {
        List<PageParser.Candidate> candidates = PageParser.findCandidates(page(NEWS_PAGE));

        assertThat(candidates).isNotEmpty();
        PageParser.Candidate best = candidates.get(0);
        assertThat(best.selector()).isEqualTo("article.card.post");
        assertThat(best.count()).isEqualTo(3);
        assertThat(best.samples()).extracting(PageParser.PageItem::link)
                .containsExactly("https://news.example/news/1", "https://news.example/news/2", "https://news.example/news/3");
        assertThat(candidates).noneMatch(candidate -> candidate.selector().contains("li"));
    }

    @Test
    void readsTitleLinkDescriptionImageAndDate() {
        List<PageParser.PageItem> items = PageParser.extractItems(page(NEWS_PAGE), "article.post");

        PageParser.PageItem first = items.get(0);
        assertThat(first.title()).isEqualTo("First headline of the day");
        assertThat(first.description()).isEqualTo("The first story is about something.");
        assertThat(first.image()).isEqualTo("https://news.example/img/1.jpg");
        assertThat(first.published()).isEqualTo(Date.from(java.time.Instant.parse("2026-10-01T10:00:00Z")));
        // The heading inside the link
        assertThat(items.get(2).link()).isEqualTo("https://news.example/news/3");
        assertThat(items.get(2).title()).isEqualTo("Third headline of the day");
    }

    @Test
    void readsTitlesRepeatedForDesktopAndMobileOnce() {
        String html = "<html><body><div>"
                + "<article><h2><a title=\"First headline of the day\" href=\"/news/1\">"
                + "<span class=\"desktop_title\">First headline of the day</span>"
                + "<span class=\"mobile_title\">First headline of the day</span></a></h2></article>"
                + "<article><h2><a href=\"/news/2\"><span>Second headline of the day</span>"
                + "<span>Second headline of the day</span></a></h2></article>"
                + "<article><h2><a title=\"Third headline\" href=\"/news/3\">Breaking: <span>Third headline</span></a></h2></article>"
                + "</div></body></html>";

        List<PageParser.PageItem> items = PageParser.extractItems(page(html), "article");

        assertThat(items).extracting(PageParser.PageItem::title)
                .containsExactly("First headline of the day", "Second headline of the day", "Third headline");
    }

    @Test
    void scopesTheSelectorToTheParentWhenTheTagIsUsedElsewhere() {
        Document doc = page("<html><body><div class=\"list\">"
                + "<div><a href=\"/p/1\">A long enough post title one</a></div>"
                + "<div><a href=\"/p/2\">A long enough post title two</a></div>"
                + "<div><a href=\"/p/3\">A long enough post title three</a></div>"
                + "</div><div>Something else</div></body></html>");

        List<PageParser.Candidate> candidates = PageParser.findCandidates(doc);

        assertThat(candidates.get(0).selector()).isEqualTo("div.list > div");
        assertThat(candidates.get(0).count()).isEqualTo(3);
    }

    @Test
    void detectsCloudflareChallenges() {
        Document challenge = page("<html><head><title>Just a moment...</title></head><body>"
                + "<script>window._cf_chl_opt={}</script></body></html>");

        assertThat(PageParser.blockReason(403, "cloudflare", challenge)).isNotNull();
        assertThat(PageParser.blockReason(200, null, page(NEWS_PAGE))).isNull();
    }

    @Test
    void detectsCaptchaOnlyPagesButNotFormsWithACaptcha() {
        Document captcha = page("<html><head><title>Check</title></head><body><div class=\"g-recaptcha\"></div></body></html>");
        Document contactForm = page("<html><body>" + "<p>Long text of the page. </p>".repeat(60)
                + "<form><div class=\"g-recaptcha\"></div></form></body></html>");

        assertThat(PageParser.blockReason(200, null, captcha)).isNotNull();
        assertThat(PageParser.blockReason(200, null, contactForm)).isNull();
    }

    @Test
    void fetchFailsForBlockedPages() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.respond("/blocked", 403, "text/html", "<html><body>Forbidden</body></html>")
                    .respond("/news", 200, "text/html", NEWS_PAGE);

            assertThatThrownBy(() -> PageParser.fetch(server.url("/blocked"))).isInstanceOf(PageBlockedException.class);
            assertThat(PageParser.fetch(server.url("/news")).title()).isEqualTo("News");
        }
    }

    @Test
    void buildsAnRssFeedKeepingTheKnownDates() throws Exception {
        List<PageParser.PageItem> items = PageParser.extractItems(page(NEWS_PAGE), "article.post");
        Date known = new Date(1_000_000_000_000L);

        String rss = PageParser.toRss("News", "https://news.example/", items, Map.of("https://news.example/news/2", known));
        SyndFeed feed = new SyndFeedInput().build(new StringReader(rss));

        assertThat(feed.getTitle()).isEqualTo("News");
        assertThat(feed.getEntries()).hasSize(3);
        assertThat(feed.getEntries().get(0).getDescription().getValue()).contains("https://news.example/img/1.jpg");
        assertThat(feed.getEntries().get(1).getPublishedDate()).isEqualTo(known);
        assertThat(feed.getEntries().get(2).getPublishedDate()).isNotNull();
    }
}
