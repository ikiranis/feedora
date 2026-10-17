package eu.apps4net.feedora.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.apps4net.feedora.models.LiveStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds out whether a YouTube video is a scheduled live stream/premiere or is live now, and its length.
 * YouTube's RSS feeds don't say, so it asks the YouTube Data API when an API key is set,
 * otherwise it reads the video's watch page (which YouTube may refuse to servers).
 */
public class YouTubeLiveChecker {

    private static final String API_URL = "https://www.googleapis.com/youtube/v3/videos";
    private static final int API_MAX_IDS = 50;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT)
            .build();

    // YouTube rate-limits (HTTP 429) or asks to sign in when a server reads many watch pages quickly,
    // so the pages are read one at a time with a pause, and not at all for a while after YouTube refuses
    private static final Duration MIN_INTERVAL = Duration.ofSeconds(1);
    private static final Duration COOLDOWN = Duration.ofMinutes(30);
    private static Instant nextRequestAt = Instant.EPOCH;
    private static volatile Instant blockedUntil = Instant.EPOCH;

    private static final Pattern VIDEO_ID_PATH = Pattern.compile("^/(?:shorts|embed|live)/([\\w-]{11})");
    private static final Pattern VIDEO_ID_PARAM = Pattern.compile("[?&]v=([\\w-]{11})");
    private static final Pattern START_TIMESTAMP = Pattern.compile("\"startTimestamp\":\"([^\"]+)\"");
    private static final Pattern LENGTH_SECONDS = Pattern.compile("\"lengthSeconds\":\"(\\d+)\"");
    private static final Pattern PLAYABILITY = Pattern.compile("\"playabilityStatus\":\\{\"status\":\"(\\w+)\"(?:,\"reason\":\"([^\"]*)\")?");

    /**
     * Live state of a video, with the (scheduled) start time if known, and its length in seconds
     * (null if unknown, e.g. for scheduled or live streams).
     */
    public record LiveInfo(LiveStatus status, LocalDateTime startAt, Integer duration) {}

    /**
     * Live states of the checked videos by video id, and the errors of the failed checks.
     * Videos that couldn't be checked are missing from the results.
     */
    public record CheckResult(Map<String, LiveInfo> results, List<String> errors) {}

    /**
     * Checks the live state of several videos: with the YouTube Data API in batches of 50 if an API key is given,
     * otherwise by reading their watch pages one by one (skipped while YouTube refuses them, see isBlocked).
     *
     * @param videoIds The YouTube video ids
     * @param apiKey   The YouTube Data API key, or null/blank to read the watch pages
     */
    public static CheckResult checkAll(Collection<String> videoIds, String apiKey) {
        Map<String, LiveInfo> results = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> ids = videoIds.stream().distinct().toList();

        if (apiKey != null && !apiKey.isBlank()) {
            for (int i = 0; i < ids.size(); i += API_MAX_IDS) {
                List<String> batch = ids.subList(i, Math.min(i + API_MAX_IDS, ids.size()));
                try {
                    results.putAll(checkWithApi(batch, apiKey.trim()));
                } catch (Exception e) {
                    errors.add(e.getMessage());
                }
            }
        } else {
            for (String id : ids) {
                if (isBlocked()) {
                    break;
                }
                try {
                    results.put(id, check(id));
                } catch (Exception e) {
                    errors.add(e.getMessage());
                }
            }
        }
        return new CheckResult(results, errors);
    }

    /**
     * Asks the YouTube Data API for the live state and length of up to 50 videos (costs 1 unit of the daily quota).
     *
     * @throws Exception if the API answers with an error, e.g. an invalid API key or exceeded quota
     */
    private static Map<String, LiveInfo> checkWithApi(List<String> videoIds, String apiKey) throws Exception {
        URI uri = URI.create(API_URL + "?part=snippet,liveStreamingDetails,contentDetails"
                + "&fields=items(id,snippet/liveBroadcastContent,liveStreamingDetails,contentDetails/duration)"
                + "&id=" + String.join(",", videoIds)
                + "&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            // The message doesn't include the URL, which has the API key
            throw new Exception("YouTube Data API: HTTP " + response.statusCode() + apiErrorMessage(response.body()));
        }
        return parseApiResponse(response.body(), videoIds);
    }

    /**
     * Error message of a YouTube Data API answer, e.g. " (quotaExceeded: The request cannot be completed...)".
     */
    private static String apiErrorMessage(String body) {
        try {
            JsonNode error = MAPPER.readTree(body).path("error");
            String reason = error.path("errors").path(0).path("reason").asText("");
            String message = error.path("message").asText("");
            return reason.isEmpty() && message.isEmpty() ? "" : " (" + reason + (reason.isEmpty() || message.isEmpty() ? "" : ": ") + message + ")";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Extracts the live states and lengths from a YouTube Data API videos answer.
     * Videos missing from the answer are private or removed, so they have no live state.
     */
    static Map<String, LiveInfo> parseApiResponse(String json, List<String> videoIds) throws Exception {
        Map<String, LiveInfo> results = new HashMap<>();
        for (String id : videoIds) {
            results.put(id, new LiveInfo(null, null, null));
        }
        for (JsonNode item : MAPPER.readTree(json).path("items")) {
            String content = item.path("snippet").path("liveBroadcastContent").asText("");
            LiveStatus status = content.equals("upcoming") ? LiveStatus.UPCOMING
                    : content.equals("live") ? LiveStatus.LIVE
                    : null;
            JsonNode details = item.path("liveStreamingDetails");
            String start = status == LiveStatus.LIVE && details.hasNonNull("actualStartTime")
                    ? details.path("actualStartTime").asText()
                    : details.path("scheduledStartTime").asText(null);
            Integer duration = status != null ? null : toSeconds(item.path("contentDetails").path("duration").asText(null));
            results.put(item.path("id").asText(), new LiveInfo(status, status != null ? toLocalDateTime(start) : null, duration));
        }
        return results;
    }

    private static LocalDateTime toLocalDateTime(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(timestamp).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            // Unknown start time format
            return null;
        }
    }

    /**
     * Seconds of an ISO 8601 duration (e.g. PT1H2M3S), or null if it's missing, zero (streams not ended yet) or unknown.
     */
    private static Integer toSeconds(String duration) {
        if (duration == null) {
            return null;
        }
        try {
            long seconds = Duration.parse(duration).getSeconds();
            return seconds > 0 && seconds <= Integer.MAX_VALUE ? (int) seconds : null;
        } catch (Exception e) {
            // Unknown duration format, e.g. with weeks
            return null;
        }
    }

    /**
     * True while YouTube refuses the checks; they're skipped until then, and the videos are checked later.
     */
    public static boolean isBlocked() {
        return Instant.now().isBefore(blockedUntil);
    }

    /**
     * Returns the YouTube video id of a watch, youtu.be, shorts, embed or live link, or null for other links.
     */
    public static String videoId(String link) {
        if (link == null) {
            return null;
        }
        try {
            URI uri = URI.create(link.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase().replaceFirst("^(www\\.|m\\.)", "");
            String path = uri.getPath() == null ? "" : uri.getPath();
            if (host.equals("youtu.be")) {
                String id = path.replaceFirst("^/", "").split("/")[0];
                return id.matches("[\\w-]{11}") ? id : null;
            }
            if (host.equals("youtube.com") || host.equals("youtube-nocookie.com")) {
                Matcher pathMatch = VIDEO_ID_PATH.matcher(path);
                if (pathMatch.find()) {
                    return pathMatch.group(1);
                }
                Matcher paramMatch = VIDEO_ID_PARAM.matcher("?" + (uri.getRawQuery() == null ? "" : uri.getRawQuery()));
                return paramMatch.find() ? paramMatch.group(1) : null;
            }
        } catch (IllegalArgumentException e) {
            // Not a valid URL
        }
        return null;
    }

    /**
     * Reads the watch page of a video and returns its live state and length.
     *
     * @param videoId The YouTube video id
     * @return The live state; its status is null for ordinary videos and ended streams
     * @throws Exception if the page can't be downloaded, or doesn't have the video's details
     *                   (e.g. a cookie consent page, or YouTube asks to sign in, as it may do for server IPs)
     */
    public static LiveInfo check(String videoId) throws Exception {
        if (isBlocked()) {
            throw new Exception("Skipped the YouTube video " + videoId + ": YouTube refused the previous checks, retrying after " + blockedUntil);
        }
        waitForTurn();

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://www.youtube.com/watch?v=" + videoId))
                .timeout(TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                // Skip the EU cookie consent interstitial
                .header("Cookie", "SOCS=CAI; CONSENT=YES+1")
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 429) {
            blockedUntil = Instant.now().plus(COOLDOWN);
        }
        if (response.statusCode() != 200) {
            throw new Exception("HTTP " + response.statusCode() + " fetching the YouTube video " + videoId);
        }
        try {
            return parse(response.body(), videoId);
        } catch (Exception e) {
            // No video details, e.g. YouTube asks to confirm we're not a bot: the next pages would be refused too
            blockedUntil = Instant.now().plus(COOLDOWN);
            throw e;
        }
    }

    /**
     * Waits until the minimum interval since the previous request has passed, so the feed threads don't read
     * watch pages in parallel.
     */
    private static void waitForTurn() throws InterruptedException {
        Instant requestAt;
        synchronized (YouTubeLiveChecker.class) {
            Instant now = Instant.now();
            requestAt = nextRequestAt.isAfter(now) ? nextRequestAt : now;
            nextRequestAt = requestAt.plus(MIN_INTERVAL);
        }
        long waitMillis = Duration.between(Instant.now(), requestAt).toMillis();
        if (waitMillis > 0) {
            Thread.sleep(waitMillis);
        }
    }

    /**
     * Extracts the live state and length from a watch page. Only the video's player data is read,
     * since the page data that follows it lists other (related) videos, which may be live too.
     *
     * @throws Exception if the page doesn't have the video's details, so its live state is unknown
     */
    static LiveInfo parse(String html, String videoId) throws Exception {
        int start = html.indexOf("ytInitialPlayerResponse");
        if (start < 0) {
            throw new Exception("The page of the YouTube video " + videoId + " has no video data (e.g. a cookie consent page)");
        }
        int end = html.indexOf("ytInitialData", start);
        String player = end > start ? html.substring(start, end) : html.substring(start);

        if (!player.contains("\"videoDetails\":{\"videoId\":\"" + videoId + "\"")) {
            Matcher playability = PLAYABILITY.matcher(player);
            String reason = !playability.find() ? "unknown reason"
                    : playability.group(1) + (playability.group(2) != null ? ": " + playability.group(2) : "");
            throw new Exception("YouTube didn't return the details of the video " + videoId + " (" + reason + ")");
        }

        LiveStatus status = player.contains("\"isUpcoming\":true") ? LiveStatus.UPCOMING
                : player.contains("\"isLiveNow\":true") ? LiveStatus.LIVE
                : null;

        Matcher startMatch = START_TIMESTAMP.matcher(player);
        LocalDateTime startAt = status != null && startMatch.find() ? toLocalDateTime(startMatch.group(1)) : null;

        // Scheduled and live streams have no length yet ("0")
        Matcher lengthMatch = LENGTH_SECONDS.matcher(player);
        Integer duration = status == null && lengthMatch.find() ? toSeconds("PT" + lengthMatch.group(1) + "S") : null;
        return new LiveInfo(status, startAt, duration);
    }
}
