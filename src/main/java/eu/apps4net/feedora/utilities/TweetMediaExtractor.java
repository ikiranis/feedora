package eu.apps4net.feedora.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the photos and video of a tweet. Feeds give a tweet's video as its thumbnail image at best, and some give
 * a tweet only its text, so they're asked from Twitter's syndication endpoint (the one its embedded tweets use),
 * which answers without logging in.
 */
public final class TweetMediaExtractor {

    // Thumbnails of videos (amplify_video_thumb, ext_tw_video_thumb) and GIFs (tweet_video_thumb)
    private static final Pattern VIDEO_THUMBNAIL = Pattern.compile("^https://pbs\\.twimg\\.com/(amplify_video_thumb|ext_tw_video_thumb|tweet_video_thumb)/");
    private static final Pattern STATUS_ID = Pattern.compile("/status(?:es)?/(\\d+)");
    private static final String SYNDICATION_URL = "https://cdn.syndication.twimg.com/tweet-result?id=";
    // The endpoint only asks for a non-empty token
    private static final String SYNDICATION_TOKEN = "a";
    // Largest video variant picked (720p is about 2.2 Mbps); videos with only bigger variants get the smallest
    private static final int MAX_BITRATE = 2_500_000;
    private static final int MAX_URL_LENGTH = 2048;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT)
            .build();

    /**
     * A tweet's video: its MP4 URL and its thumbnail (null if Twitter gives none).
     */
    public record TweetVideo(String url, String thumbnail) {}

    /**
     * A tweet's media: its video (null if none) and its photos (empty if none).
     */
    public record TweetMedia(TweetVideo video, List<String> photos) {}

    private TweetMediaExtractor() {}

    /**
     * Whether an image is a photo attached to a tweet (not a video thumbnail or a link card image).
     */
    public static boolean isPhoto(String url) {
        return url != null && url.startsWith("https://pbs.twimg.com/media/");
    }

    /**
     * Whether an image is the thumbnail of a tweet's video or GIF.
     */
    public static boolean isVideoThumbnail(String url) {
        return url != null && VIDEO_THUMBNAIL.matcher(url).find();
    }

    /**
     * Thumbnail of a tweet's video: the first video thumbnail among its card thumbnail, attached images and the images
     * of its description.
     *
     * @return The thumbnail URL, or null if the tweet has no video
     */
    public static String findThumbnail(String thumbnail, List<String> images, String description) {
        if (isVideoThumbnail(thumbnail)) return thumbnail;
        for (String image : images) {
            if (isVideoThumbnail(image)) return image;
        }
        if (description == null || !description.contains("<img")) return null;
        for (Element img : Jsoup.parseBodyFragment(description).select("img[src]")) {
            if (isVideoThumbnail(img.attr("src"))) return img.attr("src");
        }
        return null;
    }

    /**
     * Whether a tweet may have media the feed didn't give: it shows a video thumbnail (the video itself is missing),
     * or it shows no image at all (some feeds give a tweet only its text).
     *
     * @param thumbnail The tweet's card thumbnail
     * @param html Its description or content HTML
     */
    public static boolean mayHaveMissingMedia(String thumbnail, String html) {
        return findThumbnail(thumbnail, List.of(), html) != null
                || (thumbnail == null && (html == null || !html.contains("<img")));
    }

    /**
     * Asks Twitter for a tweet's photos and the MP4 file of its video (or GIF), the one shown by the given thumbnail
     * if it has several. A tweet without media of its own gets those of the tweet it quotes.
     *
     * @param link The tweet's link, e.g. https://x.com/AP/status/123
     * @param thumbnail The video's thumbnail if known, see findThumbnail
     * @return The media, or null if Twitter doesn't know the tweet (e.g. it was deleted)
     * @throws Exception if Twitter can't be reached or answers with an error
     */
    public static TweetMedia fetch(String link, String thumbnail) throws Exception {
        Matcher id = STATUS_ID.matcher(link != null ? link : "");
        if (!id.find()) return null;

        HttpRequest request = HttpRequest.newBuilder(URI.create(SYNDICATION_URL + id.group(1) + "&token=" + SYNDICATION_TOKEN))
                .timeout(TIMEOUT).GET().build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        // Deleted, protected or unknown tweets
        if (response.statusCode() == 404) return null;
        if (response.statusCode() != 200) throw new Exception("Twitter syndication: HTTP " + response.statusCode());
        return parseMedia(response.body(), thumbnail);
    }

    /**
     * Photos and video in a syndication answer.
     */
    static TweetMedia parseMedia(String json, String thumbnail) throws Exception {
        JsonNode tweet = MAPPER.readTree(json);
        List<String> photos = photos(tweet);
        if (photos.isEmpty()) photos = photos(tweet.path("quoted_tweet"));
        return new TweetMedia(parseVideo(tweet, thumbnail), photos);
    }

    private static List<String> photos(JsonNode tweet) {
        List<String> photos = new ArrayList<>();
        for (JsonNode media : tweet.path("mediaDetails")) {
            String url = media.path("media_url_https").asText("");
            if ("photo".equals(media.path("type").asText()) && url.startsWith("https://") && url.length() <= MAX_URL_LENGTH) photos.add(url);
        }
        return photos;
    }

    /**
     * Video in a syndication answer, preferring the one with the given thumbnail.
     */
    private static TweetVideo parseVideo(JsonNode tweet, String thumbnail) {
        JsonNode fallback = null;
        for (JsonNode node : new JsonNode[]{tweet, tweet.path("quoted_tweet")}) {
            for (JsonNode media : node.path("mediaDetails")) {
                if (!media.has("video_info")) continue;
                if (thumbnail != null && sameImage(media.path("media_url_https").asText(), thumbnail)) return video(media);
                if (fallback == null) fallback = media;
            }
        }
        return fallback != null ? video(fallback) : null;
    }

    private static TweetVideo video(JsonNode media) {
        String url = bestVariant(media);
        if (url == null) return null;
        String thumbnail = media.path("media_url_https").asText("");
        return new TweetVideo(url, thumbnail.startsWith("https://") && thumbnail.length() <= MAX_URL_LENGTH ? thumbnail : null);
    }

    /**
     * Largest MP4 variant up to MAX_BITRATE, otherwise the smallest one (HLS playlists are skipped, most browsers
     * can't play them in a video element).
     */
    private static String bestVariant(JsonNode media) {
        String best = null, smallest = null;
        int bestBitrate = -1, smallestBitrate = Integer.MAX_VALUE;
        for (JsonNode variant : media.path("video_info").path("variants")) {
            String url = variant.path("url").asText("");
            if (!"video/mp4".equals(variant.path("content_type").asText()) || !url.startsWith("https://") || url.length() > MAX_URL_LENGTH) continue;
            int bitrate = variant.path("bitrate").asInt(0);
            if (bitrate <= MAX_BITRATE && bitrate > bestBitrate) {
                best = url;
                bestBitrate = bitrate;
            }
            if (bitrate < smallestBitrate) {
                smallest = url;
                smallestBitrate = bitrate;
            }
        }
        return best != null ? best : smallest;
    }

    /**
     * Whether two image URLs are the same image, ignoring their size options (e.g. ?format=jpg&name=900x900).
     */
    private static boolean sameImage(String a, String b) {
        return a.split("\\?")[0].equals(b.split("\\?")[0]);
    }

    /**
     * Whether a video is a GIF, which Twitter plays muted in a loop.
     */
    public static boolean isGif(String video) {
        return video != null && video.startsWith("https://video.twimg.com/tweet_video/");
    }

    /**
     * Puts a tweet's video in its content HTML, in place of its thumbnail (or after the text if the content doesn't
     * show the thumbnail). Videos start muted, their controls are shown on hover (by the frontend); GIFs play in a loop,
     * like on Twitter.
     *
     * @param html The tweet's content HTML, may be null
     * @param video The MP4 URL
     * @param thumbnail The video's thumbnail, shown until it plays
     * @return The content HTML with the video
     */
    public static String withVideo(String html, String video, String thumbnail) {
        Document doc = Jsoup.parseBodyFragment(html != null ? html : "");
        doc.outputSettings().prettyPrint(false);

        Element element = doc.createElement("video").attr("src", video).attr("playsinline", "").attr("preload", "metadata").attr("muted", "");
        if (thumbnail != null) element.attr("poster", thumbnail);
        if (isGif(video)) element.attr("autoplay", "").attr("loop", "");

        Element shown = thumbnail == null ? null : doc.select("img[src]").stream()
                .filter(img -> sameImage(img.attr("src"), thumbnail)).findFirst().orElse(null);
        if (shown != null) {
            Element photos = shown.parent();
            shown.replaceWith(element);
            // The video gets the full width, out of the row of photos
            if (photos != null && photos.hasClass("tweet-photos")) {
                photos.before(element);
                if (photos.children().isEmpty()) photos.remove();
            }
        } else {
            doc.body().appendChild(element);
        }
        return doc.body().html();
    }
}
