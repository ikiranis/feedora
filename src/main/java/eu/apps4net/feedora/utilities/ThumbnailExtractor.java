package eu.apps4net.feedora.utilities;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import org.jdom2.Element;
import org.jsoup.Jsoup;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the image to show as a post's thumbnail on its card.
 */
public final class ThumbnailExtractor {

    private static final String MEDIA_RSS_NAMESPACE = "http://search.yahoo.com/mrss/";
    private static final int MAX_URL_LENGTH = 2048;

    private ThumbnailExtractor() {}

    /**
     * Returns the thumbnail of a feed entry: the first image of its description, then of its full content
     * (e.g. RSS content:encoded), then an image enclosure, then a Media RSS thumbnail or image.
     *
     * @param entry The feed entry
     * @return Absolute http(s) image URL, or null if the entry has no image
     */
    public static String find(SyndEntry entry) {
        String link = entry.getLink();

        if (entry.getDescription() != null) {
            String image = firstImage(entry.getDescription().getValue(), link);
            if (image != null) return image;
        }

        if (entry.getContents() != null) {
            for (SyndContent content : entry.getContents()) {
                String image = firstImage(content.getValue(), link);
                if (image != null) return image;
            }
        }

        if (entry.getEnclosures() != null) {
            for (SyndEnclosure enclosure : entry.getEnclosures()) {
                if (enclosure.getType() != null && enclosure.getType().startsWith("image/")) {
                    String image = absolute(enclosure.getUrl(), link);
                    if (image != null) return image;
                }
            }
        }

        return mediaImage(entry.getForeignMarkup(), link);
    }

    /**
     * Returns all the images attached to a feed entry: image enclosures and Media RSS media:content images
     * (also inside media:group), in feed order, without duplicates. Used for tweets, whose photos are only given this way.
     *
     * @param entry The feed entry
     * @return Absolute http(s) image URLs, empty if the entry has none
     */
    public static List<String> attachedImages(SyndEntry entry) {
        Set<String> images = new LinkedHashSet<>();
        if (entry.getEnclosures() != null) {
            for (SyndEnclosure enclosure : entry.getEnclosures()) {
                if (enclosure.getType() != null && enclosure.getType().startsWith("image/")) {
                    String image = absolute(enclosure.getUrl(), entry.getLink());
                    if (image != null) images.add(image);
                }
            }
        }
        addMediaContentImages(entry.getForeignMarkup(), entry.getLink(), images);
        return new ArrayList<>(images);
    }

    private static void addMediaContentImages(List<Element> elements, String baseUri, Set<String> images) {
        if (elements == null) return;
        for (Element element : elements) {
            if (!MEDIA_RSS_NAMESPACE.equals(element.getNamespaceURI())) continue;
            if (element.getName().equals("group")) {
                addMediaContentImages(element.getChildren(), baseUri, images);
            } else if (element.getName().equals("content")) {
                String type = element.getAttributeValue("type", "");
                if (type.startsWith("image/") || "image".equals(element.getAttributeValue("medium"))) {
                    String image = absolute(element.getAttributeValue("url"), baseUri);
                    if (image != null) images.add(image);
                }
            }
        }
    }

    /**
     * First image of an HTML fragment.
     */
    static String firstImage(String html, String baseUri) {
        if (html == null || !html.contains("<img")) return null;
        org.jsoup.nodes.Element img = Jsoup.parseBodyFragment(html, baseUri != null ? baseUri : "").selectFirst("img[src]");
        return img != null ? absolute(img.attr("src"), baseUri) : null;
    }

    /**
     * Media RSS image: media:thumbnail, or media:content with an image type or medium, also inside media:group.
     */
    private static String mediaImage(List<Element> elements, String baseUri) {
        if (elements == null) return null;

        String content = null;
        for (Element element : elements) {
            if (!MEDIA_RSS_NAMESPACE.equals(element.getNamespaceURI())) continue;
            switch (element.getName()) {
                case "thumbnail" -> {
                    String image = absolute(element.getAttributeValue("url"), baseUri);
                    if (image != null) return image;
                }
                case "content" -> {
                    String type = element.getAttributeValue("type", "");
                    if (content == null && (type.startsWith("image/") || "image".equals(element.getAttributeValue("medium")))) {
                        content = absolute(element.getAttributeValue("url"), baseUri);
                    }
                }
                case "group" -> {
                    String image = mediaImage(element.getChildren(), baseUri);
                    if (image != null) return image;
                }
                default -> {}
            }
        }
        return content;
    }

    /**
     * Resolves a URL against the entry's link, keeping only http(s) URLs that fit in the database column.
     */
    private static String absolute(String url, String baseUri) {
        if (url == null || url.isBlank()) return null;
        try {
            URI uri = baseUri != null ? URI.create(baseUri).resolve(url.trim()) : URI.create(url.trim());
            String resolved = uri.toString();
            boolean http = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            return http && resolved.length() <= MAX_URL_LENGTH ? resolved : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
