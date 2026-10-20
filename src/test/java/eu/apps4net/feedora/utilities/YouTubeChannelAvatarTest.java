package eu.apps4net.feedora.utilities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YouTubeChannelAvatarTest {

    @Test
    void channelIdComesFromTheWebsiteOrTheFeedUrl() {
        String id = "UCtAanHA6HvqPdhEZco_Rn1Q";
        assertThat(YouTubeChannelAvatar.channelId("https://www.youtube.com/channel/" + id, null)).isEqualTo(id);
        assertThat(YouTubeChannelAvatar.channelId("", "https://www.youtube.com/feeds/videos.xml?channel_id=" + id)).isEqualTo(id);
    }

    @Test
    void playlistsAndOtherSitesHaveNoChannelId() {
        assertThat(YouTubeChannelAvatar.channelId("https://www.youtube.com/playlist?list=PL123",
                "https://www.youtube.com/feeds/videos.xml?playlist_id=PL123")).isNull();
        assertThat(YouTubeChannelAvatar.channelId("https://example.com/channel/UCtAanHA6HvqPdhEZco_Rn1Q", null)).isNull();
    }

    @Test
    void avatarsAreReadFromTheApiAnswer() throws Exception {
        String body = "{\"items\":[{\"id\":\"UC1\",\"snippet\":{\"thumbnails\":{\"default\":{\"url\":\"https://yt3.ggpht.com/a=s88\"}}}},"
                + "{\"id\":\"UC2\",\"snippet\":{}}]}";

        assertThat(YouTubeChannelAvatar.parseApiResponse(body)).containsExactly(java.util.Map.entry("UC1", "https://yt3.ggpht.com/a=s88"));
    }

    @Test
    void pageAvatarsAreDownloadedSmall() {
        assertThat(YouTubeChannelAvatar.smallImage("https://yt3.googleusercontent.com/ytc/abc=s900-c-k-c0x00ffffff-no-rj"))
                .isEqualTo("https://yt3.googleusercontent.com/ytc/abc=s88-c-k-c0x00ffffff-no-rj");
    }
}
