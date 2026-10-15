package eu.apps4net.feedora.utilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class YouTubeFeedResolverTest {

    @Test
    void nullStaysNull() throws Exception {
        assertThat(YouTubeFeedResolver.resolve(null)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "'  https://example.com/rss  ', https://example.com/rss",
            "https://notyoutube.com/@name, https://notyoutube.com/@name",
            "https://www.youtube.com/feeds/videos.xml?channel_id=UCabc, https://www.youtube.com/feeds/videos.xml?channel_id=UCabc",
            "https://www.youtube.com/watch?v=abc, https://www.youtube.com/watch?v=abc",
            "https://www.youtube.com/c/, https://www.youtube.com/c/",
            "'https://exa mple.com/rss', 'https://exa mple.com/rss'",
    })
    void urlsThatArentChannelsOrPlaylistsAreUnchanged(String url, String expected) throws Exception {
        assertThat(YouTubeFeedResolver.resolve(url)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "https://www.youtube.com/channel/UC1234567890123456789012, https://www.youtube.com/feeds/videos.xml?channel_id=UC1234567890123456789012",
            "https://youtube.com/channel/UC1234567890123456789012/videos, https://www.youtube.com/feeds/videos.xml?channel_id=UC1234567890123456789012",
            "m.youtube.com/channel/UCabcdefghij-_abcdefghij, https://www.youtube.com/feeds/videos.xml?channel_id=UCabcdefghij-_abcdefghij",
            "https://www.youtube.com/playlist?list=PLabc-123_x, https://www.youtube.com/feeds/videos.xml?playlist_id=PLabc-123_x",
            "https://www.youtube.com/playlist?foo=1&list=PLxyz, https://www.youtube.com/feeds/videos.xml?playlist_id=PLxyz",
    })
    void channelAndPlaylistUrlsAreConvertedWithoutDownloading(String url, String expected) throws Exception {
        assertThat(YouTubeFeedResolver.resolve(url)).isEqualTo(expected);
    }
}
