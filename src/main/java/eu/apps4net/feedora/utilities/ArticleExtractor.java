package eu.apps4net.feedora.utilities;

import net.dankito.readability4j.Article;
import net.dankito.readability4j.Readability4J;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Safelist;

public class ArticleExtractor {

    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";
    private static final int TIMEOUT_MILLIS = 15000;

    // Prefix of stored content made by the current extraction logic. Bump it when extraction improves,
    // so content stored by older versions is extracted again the next time it's opened.
    public static final String VERSION_MARKER = "<!--feedora-content-v2-->";

    // Allowed markup in post content: text formatting, lists, tables, links and images
    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("figure", "figcaption", "hr")
            .addAttributes("a", "target", "rel");

    /**
     * Downloads a web page and extracts its main article content, like the browsers' reader view.
     *
     * @param url The page URL
     * @return Sanitized article HTML, or null if no content could be extracted
     * @throws Exception if the page can't be downloaded
     */
    public static String extractFromUrl(String url) throws Exception {
        Document page = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MILLIS)
                .followRedirects(true)
                .maxBodySize(5 * 1024 * 1024)
                .get();

        fixLazyImages(page);
        String leadImage = findLeadImage(page);

        Article article = new Readability4J(page.location(), page.outerHtml()).parse();
        String content = sanitize(article.getContent(), page.location());

        // The main photo is often outside the article body (e.g. in the page header), so add it if the content has none
        if (content != null && leadImage != null && !content.contains("<img")) {
            content = "<figure><img src=\"" + Entities.escape(leadImage) + "\" alt=\"\"></figure>" + content;
        }
        return content;
    }

    /**
     * Copies the real URL of lazy-loaded images (data-src, data-lazy-src, data-original or srcset) into src,
     * so they survive extraction and sanitization.
     */
    private static void fixLazyImages(Document page) {
        for (Element img : page.select("img")) {
            String src = img.attr("src");
            boolean hasRealSrc = !src.isBlank() && !src.startsWith("data:");
            if (hasRealSrc) continue;

            for (String attribute : new String[]{"data-src", "data-lazy-src", "data-original", "data-srcset", "srcset"}) {
                String value = img.attr(attribute).trim();
                if (value.isEmpty()) continue;
                // srcset: "url1 300w, url2 600w", take the first URL
                String url = value.split("\\s*,\\s*")[0].split("\\s+")[0];
                if (!url.startsWith("data:")) {
                    img.attr("src", url); // Relative URLs are made absolute by sanitize()
                    break;
                }
            }
        }
    }

    /**
     * Main image of the page from its social media meta tags.
     *
     * @return Absolute image URL, or null if the page declares none
     */
    private static String findLeadImage(Document page) {
        Element meta = page.selectFirst("meta[property=og:image], meta[name=og:image], meta[name=twitter:image], meta[property=twitter:image]");
        String url = meta != null ? meta.absUrl("content") : "";
        if (url.isEmpty()) {
            Element link = page.selectFirst("link[rel=image_src]");
            url = link != null ? link.absUrl("href") : "";
        }
        return url.startsWith("http") ? url : null;
    }

    /**
     * Sanitizes HTML for display: removes scripts, styles and unknown markup, makes links and images absolute
     * and turns plain text with line breaks into paragraphs.
     *
     * @param html The HTML (or plain text) to sanitize
     * @param baseUri The URL relative links and images are resolved against
     * @return Sanitized HTML, or null if it has no text or images
     */
    public static String sanitize(String html, String baseUri) {
        if (html == null || html.isBlank()) return null;

        // Plain text: one paragraph per blank-line separated block, line breaks kept inside
        if (!html.matches("(?s).*<[a-zA-Z/!].*")) {
            StringBuilder paragraphs = new StringBuilder();
            for (String block : html.trim().split("\\R\\s*\\R")) {
                paragraphs.append("<p>").append(Entities.escape(block.trim()).replaceAll("\\R", "<br>")).append("</p>");
            }
            html = paragraphs.toString();
        }

        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
        String clean = Jsoup.clean(html, baseUri != null ? baseUri : "", SAFELIST, settings);
        Document doc = Jsoup.parseBodyFragment(clean);
        doc.select("a[href]").attr("target", "_blank").attr("rel", "noopener noreferrer");

        boolean empty = doc.body().text().isBlank() && doc.select("img").isEmpty();
        return empty ? null : doc.body().html();
    }

    /**
     * Length of the visible text of an HTML fragment.
     */
    public static int textLength(String html) {
        return html == null ? 0 : Jsoup.parse(html).text().length();
    }
}
