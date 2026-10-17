package eu.apps4net.feedora.utilities;

import eu.apps4net.feedora.models.LiveStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YouTubeLiveCheckerTest {

    private static final String ID = "lAaGXBHF0yA";

    @ParameterizedTest
    @CsvSource({
            "https://www.youtube.com/watch?v=lAaGXBHF0yA, lAaGXBHF0yA",
            "https://m.youtube.com/watch?feature=share&v=lAaGXBHF0yA, lAaGXBHF0yA",
            "https://youtu.be/lAaGXBHF0yA, lAaGXBHF0yA",
            "https://www.youtube.com/shorts/lAaGXBHF0yA, lAaGXBHF0yA",
            "https://www.youtube.com/live/lAaGXBHF0yA?si=x, lAaGXBHF0yA",
    })
    void videoIdOfYouTubeLinks(String link, String expected) {
        assertThat(YouTubeLiveChecker.videoId(link)).isEqualTo(expected);
    }

    @Test
    void noVideoIdForOtherLinks() {
        assertThat(YouTubeLiveChecker.videoId(null)).isNull();
        assertThat(YouTubeLiveChecker.videoId("https://example.com/watch?v=lAaGXBHF0yA")).isNull();
        assertThat(YouTubeLiveChecker.videoId("https://www.youtube.com/@channel")).isNull();
    }

    @Test
    void pageWithoutVideoDetailsFails() {
        assertThatThrownBy(() -> YouTubeLiveChecker.parse("<html>consent</html>", ID)).hasMessageContaining("no video data");
        assertThatThrownBy(() -> YouTubeLiveChecker.parse("<script>var ytInitialPlayerResponse = {\"playabilityStatus\":"
                + "{\"status\":\"LOGIN_REQUIRED\",\"reason\":\"Sign in to confirm you’re not a bot\"}};</script>", ID))
                .hasMessageContaining("LOGIN_REQUIRED: Sign in to confirm you’re not a bot");
    }

    @Test
    void upcomingVideo() throws Exception {
        YouTubeLiveChecker.LiveInfo info = YouTubeLiveChecker.parse(page(
                "\"isLiveContent\":true,\"isUpcoming\":true,\"liveBroadcastDetails\":{\"isLiveNow\":false,\"startTimestamp\":\"2026-10-17T09:30:00+00:00\"}"), ID);
        assertThat(info.status()).isEqualTo(LiveStatus.UPCOMING);
        assertThat(info.startAt()).isEqualTo(OffsetDateTime.parse("2026-10-17T09:30:00+00:00")
                .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
    }

    @Test
    void liveVideo() throws Exception {
        YouTubeLiveChecker.LiveInfo info = YouTubeLiveChecker.parse(page(
                "\"isLive\":true,\"liveBroadcastDetails\":{\"isLiveNow\":true,\"startTimestamp\":\"2026-08-21T01:45:06+00:00\"}"), ID);
        assertThat(info.status()).isEqualTo(LiveStatus.LIVE);
    }

    @Test
    void endedPremiereAndRelatedLiveVideosAreIgnored() throws Exception {
        YouTubeLiveChecker.LiveInfo info = YouTubeLiveChecker.parse(page(
                "\"liveBroadcastDetails\":{\"isLiveNow\":false,\"startTimestamp\":\"2026-10-14T16:00:06+00:00\",\"endTimestamp\":\"2026-10-15T00:02:06+00:00\"}")
                + "<script>var ytInitialData = {\"isUpcoming\":true,\"isLiveNow\":true};</script>", ID);
        assertThat(info.status()).isNull();
        assertThat(info.startAt()).isNull();
    }

    @Test
    void videoLength() throws Exception {
        assertThat(YouTubeLiveChecker.parse(page("\"lengthSeconds\":\"634\",\"isLiveContent\":false"), ID).duration()).isEqualTo(634);
        assertThat(YouTubeLiveChecker.parse(page("\"lengthSeconds\":\"0\",\"isUpcoming\":true"), ID).duration()).isNull();
    }

    @Test
    void apiResponse() throws Exception {
        String json = """
                {"items": [
                  {"id": "upcoming001", "snippet": {"liveBroadcastContent": "upcoming"},
                   "liveStreamingDetails": {"scheduledStartTime": "2026-10-17T09:30:00Z"}},
                  {"id": "livenow0001", "snippet": {"liveBroadcastContent": "live"},
                   "liveStreamingDetails": {"scheduledStartTime": "2026-10-16T20:00:00Z", "actualStartTime": "2026-10-16T20:03:00Z"}},
                  {"id": "endedstream", "snippet": {"liveBroadcastContent": "none"},
                   "liveStreamingDetails": {"actualStartTime": "2026-10-14T16:00:06Z", "actualEndTime": "2026-10-15T00:02:06Z"},
                   "contentDetails": {"duration": "PT8H2M"}},
                  {"id": "ordinaryvid", "snippet": {"liveBroadcastContent": "none"}, "contentDetails": {"duration": "PT10M34S"}},
                  {"id": "nolength001", "snippet": {"liveBroadcastContent": "none"}, "contentDetails": {"duration": "P0D"}}
                ]}""";
        Map<String, YouTubeLiveChecker.LiveInfo> results = YouTubeLiveChecker.parseApiResponse(json,
                List.of("upcoming001", "livenow0001", "endedstream", "ordinaryvid", "nolength001", "removedvide"));

        assertThat(results.get("upcoming001").status()).isEqualTo(LiveStatus.UPCOMING);
        assertThat(results.get("upcoming001").startAt()).isEqualTo(local("2026-10-17T09:30:00Z"));
        assertThat(results.get("livenow0001").status()).isEqualTo(LiveStatus.LIVE);
        assertThat(results.get("livenow0001").startAt()).isEqualTo(local("2026-10-16T20:03:00Z"));
        assertThat(results.get("upcoming001").duration()).isNull();
        assertThat(results.get("endedstream")).isEqualTo(new YouTubeLiveChecker.LiveInfo(null, null, 8 * 3600 + 2 * 60));
        assertThat(results.get("ordinaryvid")).isEqualTo(new YouTubeLiveChecker.LiveInfo(null, null, 634));
        assertThat(results.get("nolength001")).isEqualTo(new YouTubeLiveChecker.LiveInfo(null, null, null));
        // Private or removed videos are missing from the answer
        assertThat(results.get("removedvide")).isEqualTo(new YouTubeLiveChecker.LiveInfo(null, null, null));
    }

    private static LocalDateTime local(String timestamp) {
        return OffsetDateTime.parse(timestamp).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static String page(String videoDetails) {
        return "<script>var ytInitialPlayerResponse = {\"playabilityStatus\":{\"status\":\"OK\"},"
                + "\"videoDetails\":{\"videoId\":\"" + ID + "\"," + videoDetails + "}};</script>";
    }
}
