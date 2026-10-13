package eu.apps4net.feedora.utilities;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts YouTube channel/playlist page URLs to their RSS feed URLs.
 * Supports /channel/UC..., /@handle, /c/name, /user/name and playlist (?list=) URLs.
 */
public class YouTubeFeedResolver {

    private static final String FEED_BASE = "https://www.youtube.com/feeds/videos.xml";

    private static final Pattern CHANNEL_ID_PATH = Pattern.compile("^/channel/(UC[\\w-]{22})");
    private static final Pattern PLAYLIST_ID = Pattern.compile("[?&]list=([\\w-]+)");

    // Patterns used to find the channel id in the channel page HTML, most specific first
    private static final Pattern[] PAGE_CHANNEL_ID_PATTERNS = {
            Pattern.compile("feeds/videos\\.xml\\?channel_id=(UC[\\w-]{22})"),
            Pattern.compile("<link rel=\"canonical\" href=\"https://www\\.youtube\\.com/channel/(UC[\\w-]{22})\""),
            Pattern.compile("\"externalId\":\"(UC[\\w-]{22})\""),
            Pattern.compile("<meta itemprop=\"(?:identifier|channelId)\" content=\"(UC[\\w-]{22})\""),
            Pattern.compile("\"channelId\":\"(UC[\\w-]{22})\"")
    };

    /**
     * Returns the RSS feed URL for a YouTube channel or playlist URL.
     * Non-YouTube URLs and URLs that are already feed URLs are returned unchanged.
     *
     * @param url The URL entered by the user
     * @return The resolved feed URL
     * @throws Exception if the URL is a YouTube channel page but the channel id cannot be found
     */
    public static String resolve(String url) throws Exception {
        if (url == null) return null;
        url = url.trim();

        URI uri;
        try {
            uri = URI.create(url.contains("://") ? url : "https://" + url);
        } catch (IllegalArgumentException e) {
            return url;
        }

        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        if (!host.equals("youtube.com") && !host.endsWith(".youtube.com")) {
            return url;
        }

        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();

        if (path.startsWith("/feeds/")) {
            return url;
        }

        Matcher playlist = PLAYLIST_ID.matcher(query);
        if (path.equals("/playlist") && playlist.find()) {
            return FEED_BASE + "?playlist_id=" + playlist.group(1);
        }

        Matcher channel = CHANNEL_ID_PATH.matcher(path);
        if (channel.find()) {
            return FEED_BASE + "?channel_id=" + channel.group(1);
        }

        if (path.startsWith("/@") || path.startsWith("/c/") || path.startsWith("/user/")) {
            // Keep only the channel part of the path (e.g. /@name/videos -> /@name)
            String[] segments = path.split("/");
            if (!path.startsWith("/@") && segments.length < 3) {
                return url;
            }
            String channelPath = path.startsWith("/@") ? "/" + segments[1] : "/" + segments[1] + "/" + segments[2];
            String channelId = fetchChannelId("https://www.youtube.com" + channelPath);
            if (channelId != null) {
                return FEED_BASE + "?channel_id=" + channelId;
            }
            if (path.startsWith("/user/")) {
                return FEED_BASE + "?user=" + URLEncoder.encode(segments[2], StandardCharsets.UTF_8);
            }
            throw new Exception("Could not find the YouTube channel id for " + url);
        }

        return url;
    }

    /**
     * Downloads the channel page and extracts the channel id from it.
     */
    private static String fetchChannelId(String pageUrl) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(pageUrl).toURL().openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        // Skip the EU cookie consent interstitial
        conn.setRequestProperty("Cookie", "SOCS=CAI; CONSENT=YES+1");

        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP " + conn.getResponseCode() + " fetching " + pageUrl);
        }

        String html;
        try (InputStream is = conn.getInputStream()) {
            html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        for (Pattern pattern : PAGE_CHANNEL_ID_PATTERNS) {
            Matcher m = pattern.matcher(html);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }
}
