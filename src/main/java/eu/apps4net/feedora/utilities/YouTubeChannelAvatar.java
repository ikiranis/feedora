package eu.apps4net.feedora.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the avatar (profile picture) of YouTube channels. All YouTube feeds share youtube.com's favicon,
 * so the color of their cards comes from the channel's avatar instead.
 */
public final class YouTubeChannelAvatar {

    private static final String API_URL = "https://www.googleapis.com/youtube/v3/channels";
    public static final int API_MAX_IDS = 50;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Pattern CHANNEL_ID = Pattern.compile("(?:/channel/|[?&]channel_id=)(UC[\\w-]{22})");
    private static final Pattern OG_IMAGE = Pattern.compile("<meta property=\"og:image\" content=\"([^\"]+)\"");
    // Size parameter of YouTube's image URLs (e.g. =s900-c-k...), so a small copy is downloaded
    private static final Pattern IMAGE_SIZE = Pattern.compile("=s\\d+");
    // The avatar is in the page's head, around 800 KB into it; reading stops there
    private static final int MAX_PAGE_BYTES = 2 * 1024 * 1024;

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT)
            .build();

    private YouTubeChannelAvatar() {}

    /**
     * Channel id of a YouTube channel feed, from its website (/channel/UC...) or feed URL (?channel_id=UC...).
     *
     * @return The id, or null if the feed isn't a YouTube channel (e.g. a playlist or another site)
     */
    public static String channelId(String... urls) {
        for (String url : urls) {
            if (url == null || !url.contains("youtube.com")) continue;
            Matcher matcher = CHANNEL_ID.matcher(url);
            if (matcher.find()) return matcher.group(1);
        }
        return null;
    }

    /**
     * Asks the YouTube Data API for the avatars of up to 50 channels (costs 1 unit of the daily quota).
     *
     * @return Avatar URL by channel id; channels missing from the answer were removed
     * @throws Exception if the API answers with an error, e.g. an invalid API key or exceeded quota
     */
    public static Map<String, String> avatarsWithApi(List<String> channelIds, String apiKey) throws Exception {
        URI uri = URI.create(API_URL + "?part=snippet&fields=items(id,snippet/thumbnails/default/url)"
                + "&id=" + String.join(",", channelIds)
                + "&key=" + URLEncoder.encode(apiKey.trim(), StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            // The message doesn't include the URL, which has the API key
            throw new Exception("YouTube Data API: HTTP " + response.statusCode());
        }
        return parseApiResponse(response.body());
    }

    static Map<String, String> parseApiResponse(String body) throws IOException {
        Map<String, String> avatars = new HashMap<>();
        for (JsonNode item : MAPPER.readTree(body).path("items")) {
            String url = item.path("snippet").path("thumbnails").path("default").path("url").asText("");
            if (!url.isEmpty()) avatars.put(item.path("id").asText(), url);
        }
        return avatars;
    }

    /**
     * Reads the avatar from the channel's page (its og:image), without an API key.
     *
     * @return The avatar URL (a small copy), or null if the page has none
     * @throws IOException if the page can't be downloaded, e.g. when YouTube refuses it (HTTP 429)
     */
    public static String avatarFromPage(String channelId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://www.youtube.com/channel/" + channelId))
                .timeout(TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                // Skip the EU cookie consent interstitial
                .header("Cookie", "SOCS=CAI; CONSENT=YES+1")
                .GET().build();
        HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            if (response.statusCode() == 404) return null;
            if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode() + " for channel " + channelId);

            // Read until the avatar shows up, not the whole (large) page
            ByteArrayOutputStream page = new ByteArrayOutputStream();
            byte[] buffer = new byte[64 * 1024];
            int read;
            while (page.size() < MAX_PAGE_BYTES && (read = body.read(buffer)) != -1) {
                page.write(buffer, 0, read);
                Matcher matcher = OG_IMAGE.matcher(page.toString(StandardCharsets.UTF_8));
                if (matcher.find()) return smallImage(matcher.group(1));
            }
            return null;
        }
    }

    // A 88px copy of a YouTube image, enough for its main color
    static String smallImage(String url) {
        return IMAGE_SIZE.matcher(url).replaceFirst("=s88");
    }
}
