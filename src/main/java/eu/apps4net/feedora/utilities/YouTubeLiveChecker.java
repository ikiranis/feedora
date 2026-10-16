package eu.apps4net.feedora.utilities;

import eu.apps4net.feedora.models.LiveStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds out whether a YouTube video is a scheduled live stream/premiere or is live now.
 * YouTube's RSS feeds don't say, so the video's watch page is read.
 */
public class YouTubeLiveChecker {

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
    private static final Pattern PLAYABILITY = Pattern.compile("\"playabilityStatus\":\\{\"status\":\"(\\w+)\"(?:,\"reason\":\"([^\"]*)\")?");

    /**
     * Live state of a video, with the (scheduled) start time if known.
     */
    public record LiveInfo(LiveStatus status, LocalDateTime startAt) {}

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
     * Reads the watch page of a video and returns its live state.
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
     * Extracts the live state from a watch page. Only the video's player data is read,
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

        LocalDateTime startAt = null;
        Matcher startMatch = START_TIMESTAMP.matcher(player);
        if (status != null && startMatch.find()) {
            try {
                startAt = OffsetDateTime.parse(startMatch.group(1)).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            } catch (Exception e) {
                // Unknown start time format
            }
        }
        return new LiveInfo(status, startAt);
    }
}
