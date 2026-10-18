package eu.apps4net.feedora.utilities;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the link in a tweet's text and reads the title and image of the linked page, for a preview on the post's card.
 */
public final class LinkPreviewExtractor {

    // Same rule as the frontend (PostElement.vue), which removes the link from the card's text
    private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"]+");
    // Twitter cuts long links in the text with an ellipsis; sentence punctuation after a link isn't part of it
    private static final Pattern TRAILING = Pattern.compile("[…\\.,;:!?)\\]»'\"]+$");
    private static final int MAX_URL_LENGTH = 2048;
    private static final int MAX_TITLE_LENGTH = 1000;

    // Pages that forward with a meta refresh instead of an HTTP redirect (e.g. t.co for browsers) are followed this many times
    private static final int MAX_META_REFRESHES = 3;
    private static final Pattern REFRESH_URL = Pattern.compile("(?i)url\\s*=\\s*['\"]?([^'\"]+)");

    public record LinkPreview(String url, String title, String image) {}

    /**
     * A link found in a tweet, with the title of Twitter's link card for it if the tweet has one.
     */
    public record FoundLink(String url, String title) {}

    private LinkPreviewExtractor() {}

    /**
     * Whether a post link points to Twitter/X.
     */
    public static boolean isTweet(String link) {
        String host = host(link);
        return host != null && isTwitterHost(host);
    }

    /**
     * Returns the link of a tweet's description to preview: Twitter's link card (a link whose text is the page title,
     * e.g. <a href="https://t.co/…">Article title - Site</a>), otherwise the first link in the text.
     * Links to Twitter/X itself (e.g. quoted tweets) are skipped.
     * A link cut with an ellipsis is returned without it, it often still works.
     * The frontend (functions/tweet.ts) finds the same link, to remove it from the shown text.
     *
     * @param description The description HTML
     * @return The link, or null if the description has none
     */
    public static FoundLink findLink(String description) {
        if (description == null || description.isBlank()) return null;
        Document doc = Jsoup.parse(description);

        for (Element anchor : doc.select("a[href]")) {
            String url = anchor.attr("href");
            String title = anchor.text().trim();
            if (isPreviewable(url) && !title.isEmpty() && !URL.matcher(title).lookingAt()) {
                return new FoundLink(url, title.length() <= MAX_TITLE_LENGTH ? title : null);
            }
        }

        Matcher matcher = URL.matcher(doc.text());
        while (matcher.find()) {
            String url = TRAILING.matcher(matcher.group()).replaceFirst("");
            if (isPreviewable(url)) return new FoundLink(url, null);
        }
        return null;
    }

    private static boolean isPreviewable(String url) {
        String host = host(url);
        return host != null && !isTwitterHost(host) && url.length() <= MAX_URL_LENGTH;
    }

    /**
     * Downloads a page and reads its title and image from its social media meta tags (or its title tag).
     *
     * @param url The page URL
     * @return The preview, or null if the page has neither a title nor an image
     * @throws Exception if the page can't be downloaded
     */
    public static LinkPreview fetch(String url) throws Exception {
        Document page = ArticleExtractor.fetch(url);
        for (int i = 0; i < MAX_META_REFRESHES; i++) {
            String next = metaRefreshUrl(page);
            if (next == null) break;
            page = ArticleExtractor.fetch(next);
        }

        String title = metaContent(page, "og:title", "twitter:title");
        if (title == null && !page.title().isBlank()) title = page.title().trim();
        if (title != null && title.length() > MAX_TITLE_LENGTH) title = title.substring(0, MAX_TITLE_LENGTH - 1) + "…";

        String image = ArticleExtractor.findLeadImage(page);
        if (image != null && image.length() > MAX_URL_LENGTH) image = null;

        if (title == null && image == null) return null;
        String location = page.location().length() <= MAX_URL_LENGTH ? page.location() : url;
        return new LinkPreview(location, title, image);
    }

    /**
     * Absolute URL a page forwards to with <meta http-equiv="refresh" content="0;URL=…">, or null if it doesn't.
     */
    private static String metaRefreshUrl(Document page) {
        Element meta = page.selectFirst("meta[http-equiv~=(?i)^refresh$]");
        if (meta == null) return null;
        Matcher matcher = REFRESH_URL.matcher(meta.attr("content"));
        if (!matcher.find()) return null;
        try {
            String next = URI.create(page.location()).resolve(matcher.group(1).trim()).toString();
            return next.startsWith("http") && !next.equals(page.location()) ? next : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String metaContent(Document page, String... names) {
        for (String name : names) {
            Element meta = page.selectFirst("meta[property=" + name + "], meta[name=" + name + "]");
            if (meta != null && !meta.attr("content").isBlank()) return meta.attr("content").trim();
        }
        return null;
    }

    private static boolean isTwitterHost(String host) {
        return host.equals("twitter.com") || host.equals("x.com")
                || host.endsWith(".twitter.com") || host.endsWith(".x.com");
    }

    private static String host(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null ? host.toLowerCase() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
