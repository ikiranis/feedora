package eu.apps4net.feedora.utilities;

import eu.apps4net.feedora.models.LiveStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class YouTubeLiveCheckerTest {

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
    void upcomingVideo() {
        YouTubeLiveChecker.LiveInfo info = YouTubeLiveChecker.parse(page(
                "\"isLiveContent\":true,\"isUpcoming\":true,\"liveBroadcastDetails\":{\"isLiveNow\":false,\"startTimestamp\":\"2026-10-17T09:30:00+00:00\"}"));
        assertThat(info.status()).isEqualTo(LiveStatus.UPCOMING);
        assertThat(info.startAt()).isEqualTo(OffsetDateTime.parse("2026-10-17T09:30:00+00:00")
                .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
    }

    @Test
    void liveVideo() {
        YouTubeLiveChecker.LiveInfo info = YouTubeLiveChecker.parse(page(
                "\"isLive\":true,\"liveBroadcastDetails\":{\"isLiveNow\":true,\"startTimestamp\":\"2026-08-21T01:45:06+00:00\"}"));
        assertThat(info.status()).isEqualTo(LiveStatus.LIVE);
    }

    @Test
    void endedPremiereAndRelatedLiveVideosAreIgnored() {
        YouTubeLiveChecker.LiveInfo info = YouTubeLiveChecker.parse(page(
                "\"liveBroadcastDetails\":{\"isLiveNow\":false,\"startTimestamp\":\"2026-10-14T16:00:06+00:00\",\"endTimestamp\":\"2026-10-15T00:02:06+00:00\"}")
                + "<script>var ytInitialData = {\"isUpcoming\":true,\"isLiveNow\":true};</script>");
        assertThat(info.status()).isNull();
        assertThat(info.startAt()).isNull();
    }

    private static String page(String playerData) {
        return "<script>var ytInitialPlayerResponse = {" + playerData + "};</script>";
    }
}
