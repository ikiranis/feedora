package eu.apps4net.feedora.utilities;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private static final Pattern HOST = Pattern.compile("https?://([^/?#:\\s]+)");
    private static final int MAX_URL_LENGTH = 2048;
    private static final Pattern TRACKING_PARAM = Pattern.compile("(?i)(utm_[^=]*|fbclid|gclid)(=|$)");
    private static final int MAX_TITLE_LENGTH = 1000;

    // Pages that forward with a meta refresh instead of an HTTP redirect (e.g. t.co for browsers) are followed this many times
    private static final int MAX_META_REFRESHES = 3;
    private static final Pattern REFRESH_URL = Pattern.compile("(?i)url\\s*=\\s*['\"]?([^'\"]+)");

    // Prefix of a tweet's stored content (its text and photos, see tweetContent), to tell it from content stored before
    // it was made, e.g. X's login page read as the tweet's article
    public static final String TWEET_CONTENT_MARKER = "<!--feedora-tweet-v1-->";

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

    /**
     * Content of a tweet to show when it's opened: its sanitized description, followed by its photos that the description
     * doesn't show (feeds give them as attachments). The image of Twitter's link card is left out, the link preview shows it.
     *
     * @param description The description HTML
     * @param images The tweet's attached images
     * @param link The tweet's link, to resolve relative URLs
     * @return The content HTML, or null if the tweet has neither text nor photos
     */
    public static String tweetContent(String description, List<String> images, String link) {
        String html = ArticleExtractor.sanitize(description, link);
        Set<String> shown = new HashSet<>();
        if (html != null) {
            Jsoup.parseBodyFragment(html).select("img[src]").forEach(img -> shown.add(img.attr("src")));
        }

        StringBuilder photos = new StringBuilder();
        for (String image : images) {
            if (image.contains("//pbs.twimg.com/card_img/") || !shown.add(image)) continue;
            photos.append("<img src=\"").append(Entities.escape(image)).append("\" alt=\"\">");
        }
        if (photos.isEmpty()) return html;
        // The photos are laid out side by side (class added after sanitizing, which removes classes)
        return (html != null ? html : "") + "<div class=\"tweet-photos\">" + photos + "</div>";
    }

    /**
     * Whether the text of a tweet's HTML has a link cut with an ellipsis, e.g. https://a.example/article/1/slug-cu…
     * Some feeds give a tweet's text as Twitter shows it, so long links are cut (and sometimes mangled before the ellipsis).
     */
    public static boolean hasCutLink(String html) {
        if (html == null) return false;
        Matcher matcher = URL.matcher(Jsoup.parse(html).text());
        while (matcher.find()) {
            if (matcher.group().endsWith("…")) return true;
        }
        return false;
    }

    /**
     * Replaces the links cut with an ellipsis in the text of a tweet's HTML (see hasCutLink) with links to their full URL,
     * as Twitter gives them (see TweetMediaExtractor). Each cut link gets the next full URL to the same site, in the order
     * of the text, so a cut link nothing matches is left as is.
     *
     * @param html The tweet's description or content HTML
     * @param links The full URLs of the tweet's links, in the order of its text
     * @return The HTML with the full links, or the given HTML if it has no cut link to replace
     */
    public static String withFullLinks(String html, List<String> links) {
        if (html == null || links.isEmpty() || !hasCutLink(html)) return html;
        Document doc = Jsoup.parseBodyFragment(html);
        doc.outputSettings().prettyPrint(false);

        List<String> unused = new ArrayList<>(links);
        boolean replaced = false;
        for (TextNode text : textNodes(doc.body())) {
            replaced |= replaceCutLinks(text, unused);
        }
        return replaced ? doc.body().html() : html;
    }

    /**
     * Text nodes of an element and its descendants, outside of links.
     */
    private static List<TextNode> textNodes(Element element) {
        List<TextNode> texts = new ArrayList<>();
        for (Node child : element.childNodes()) {
            if (child instanceof TextNode text) texts.add(text);
            else if (child instanceof Element childElement && !childElement.nameIs("a")) texts.addAll(textNodes(childElement));
        }
        return texts;
    }

    /**
     * Replaces the cut links of a text node with links to the first unused full URL to the same site, which is then used.
     *
     * @return Whether a link was replaced
     */
    private static boolean replaceCutLinks(TextNode text, List<String> unused) {
        String value = text.getWholeText();
        Matcher matcher = URL.matcher(value);
        List<Node> nodes = new ArrayList<>();
        int start = 0;
        while (matcher.find()) {
            if (!matcher.group().endsWith("…")) continue;
            String full = fullLink(matcher.group(), unused);
            if (full == null) continue;
            unused.remove(full);
            if (matcher.start() > start) nodes.add(new TextNode(value.substring(start, matcher.start())));
            nodes.add(new Element("a").attr("href", full).attr("target", "_blank").attr("rel", "noopener noreferrer").text(full));
            start = matcher.end();
        }
        if (nodes.isEmpty()) return false;
        if (start < value.length()) nodes.add(new TextNode(value.substring(start)));
        for (Node node : nodes) {
            text.before(node);
        }
        text.remove();
        return true;
    }

    /**
     * First of the full URLs to the same site as a cut link (ignoring www.), or null if none is.
     */
    private static String fullLink(String cutLink, List<String> links) {
        String site = site(TRAILING.matcher(cutLink).replaceFirst(""));
        if (site == null) return null;
        return links.stream().filter(link -> site.equals(site(link))).findFirst().orElse(null);
    }

    /**
     * Host of a URL without www., read from the text since a cut link may not parse as a URI (e.g. a mangled path).
     */
    private static String site(String url) {
        Matcher matcher = HOST.matcher(url);
        return matcher.lookingAt() ? matcher.group(1).toLowerCase().replaceFirst("^www\\.", "") : null;
    }

    /**
     * A page URL reduced to what tells the page apart, to find the same page linked from different places: its host
     * without www., its path without a trailing slash and its query without tracking parameters (utm_…, fbclid);
     * the scheme and fragment are ignored.
     *
     * @return The key, or null if the URL can't be read
     */
    public static String pageKey(String url) {
        String site = url != null ? site(url) : null;
        if (site == null) return null;
        try {
            URI uri = URI.create(url.trim());
            String path = uri.getPath() != null ? uri.getPath().replaceAll("/+$", "") : "";
            String query = uri.getQuery() == null ? "" : String.join("&", Arrays.stream(uri.getQuery().split("&"))
                    .filter(param -> !TRACKING_PARAM.matcher(param).lookingAt())
                    .toList());
            return site + path + (query.isEmpty() ? "" : "?" + query);
        } catch (IllegalArgumentException e) {
            return null;
        }
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
