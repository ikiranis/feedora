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

    public record LinkPreview(String url, String title, String image) {}

    private LinkPreviewExtractor() {}

    /**
     * Whether a post link points to Twitter/X.
     */
    public static boolean isTweet(String link) {
        String host = host(link);
        return host != null && isTwitterHost(host);
    }

    /**
     * Returns the first link in the text of a tweet's description, skipping links to Twitter/X itself
     * (e.g. quoted tweets). A link cut with an ellipsis is returned without it, it often still works.
     *
     * @param description The description HTML
     * @return The link, or null if the text has none
     */
    public static String findLink(String description) {
        if (description == null || description.isBlank()) return null;

        Matcher matcher = URL.matcher(Jsoup.parse(description).text());
        while (matcher.find()) {
            String url = TRAILING.matcher(matcher.group()).replaceFirst("");
            String host = host(url);
            if (host != null && !isTwitterHost(host) && url.length() <= MAX_URL_LENGTH) {
                return url;
            }
        }
        return null;
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

        String title = metaContent(page, "og:title", "twitter:title");
        if (title == null && !page.title().isBlank()) title = page.title().trim();
        if (title != null && title.length() > MAX_TITLE_LENGTH) title = title.substring(0, MAX_TITLE_LENGTH - 1) + "…";

        String image = ArticleExtractor.findLeadImage(page);
        if (image != null && image.length() > MAX_URL_LENGTH) image = null;

        if (title == null && image == null) return null;
        String location = page.location().length() <= MAX_URL_LENGTH ? page.location() : url;
        return new LinkPreview(location, title, image);
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
