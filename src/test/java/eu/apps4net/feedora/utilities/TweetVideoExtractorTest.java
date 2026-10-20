package eu.apps4net.feedora.utilities;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TweetVideoExtractorTest {

    private static final String THUMB = "https://pbs.twimg.com/amplify_video_thumb/1/img/a.jpg";

    @Test
    void recognisesVideoThumbnails() {
        assertThat(TweetVideoExtractor.isVideoThumbnail(THUMB)).isTrue();
        assertThat(TweetVideoExtractor.isVideoThumbnail("https://pbs.twimg.com/ext_tw_video_thumb/1/pu/img/a.jpg")).isTrue();
        assertThat(TweetVideoExtractor.isVideoThumbnail("https://pbs.twimg.com/tweet_video_thumb/a.jpg")).isTrue();
        assertThat(TweetVideoExtractor.isVideoThumbnail("https://pbs.twimg.com/media/a.jpg")).isFalse();
        assertThat(TweetVideoExtractor.isVideoThumbnail(null)).isFalse();
    }

    @Test
    void findsThumbnailInImagesOrDescription() {
        assertThat(TweetVideoExtractor.findThumbnail("https://pbs.twimg.com/media/a.jpg", List.of(THUMB), null)).isEqualTo(THUMB);
        assertThat(TweetVideoExtractor.findThumbnail(null, List.of(), "<p>Text</p><p><img src=\"" + THUMB + "\"></p>")).isEqualTo(THUMB);
        assertThat(TweetVideoExtractor.findThumbnail("https://pbs.twimg.com/media/a.jpg", List.of(), "<p>Text</p>")).isNull();
    }

    @Test
    void checksTweetsWithVideoThumbnailOrNoImage() {
        // Some feeds give a video tweet only its text
        assertThat(TweetVideoExtractor.mayHaveVideo(null, "<p>Text</p>")).isTrue();
        assertThat(TweetVideoExtractor.mayHaveVideo(null, null)).isTrue();
        assertThat(TweetVideoExtractor.mayHaveVideo(THUMB, "<p>Text</p>")).isTrue();
        assertThat(TweetVideoExtractor.mayHaveVideo(null, "<p>Text</p><p><img src=\"" + THUMB + "\"></p>")).isTrue();
        assertThat(TweetVideoExtractor.mayHaveVideo("https://pbs.twimg.com/media/a.jpg", "<p>Text</p>")).isFalse();
        assertThat(TweetVideoExtractor.mayHaveVideo(null, "<p>Text</p><p><img src=\"https://pbs.twimg.com/media/a.jpg\"></p>")).isFalse();
    }

    @Test
    void picksLargestMp4UpToMaxBitrateOfVideoWithThumbnail() throws Exception {
        String json = """
                {"mediaDetails": [
                  {"type": "photo", "media_url_https": "https://pbs.twimg.com/media/p.jpg"},
                  {"type": "video", "media_url_https": "https://pbs.twimg.com/amplify_video_thumb/2/img/b.jpg",
                   "video_info": {"variants": [{"content_type": "video/mp4", "bitrate": 832000, "url": "https://video.twimg.com/other.mp4"}]}},
                  {"type": "video", "media_url_https": "https://pbs.twimg.com/amplify_video_thumb/1/img/a.jpg",
                   "video_info": {"variants": [
                     {"content_type": "application/x-mpegURL", "url": "https://video.twimg.com/a.m3u8"},
                     {"content_type": "video/mp4", "bitrate": 832000, "url": "https://video.twimg.com/360.mp4"},
                     {"content_type": "video/mp4", "bitrate": 2176000, "url": "https://video.twimg.com/720.mp4"},
                     {"content_type": "video/mp4", "bitrate": 10368000, "url": "https://video.twimg.com/1080.mp4"}]}}
                ]}""";

        assertThat(TweetVideoExtractor.parseVideo(json, THUMB + "?format=jpg"))
                .isEqualTo(new TweetVideoExtractor.TweetVideo("https://video.twimg.com/720.mp4", THUMB));
        assertThat(TweetVideoExtractor.parseVideo(json, null).url()).isEqualTo("https://video.twimg.com/other.mp4");
    }

    @Test
    void findsVideoOfQuotedTweetAndGifs() throws Exception {
        String json = """
                {"mediaDetails": [], "quoted_tweet": {"mediaDetails": [{"type": "animated_gif",
                  "media_url_https": "https://pbs.twimg.com/tweet_video_thumb/g.jpg",
                  "video_info": {"variants": [{"content_type": "video/mp4", "bitrate": 0, "url": "https://video.twimg.com/tweet_video/g.mp4"}]}}]}}""";

        assertThat(TweetVideoExtractor.parseVideo(json, null)).isEqualTo(new TweetVideoExtractor.TweetVideo(
                "https://video.twimg.com/tweet_video/g.mp4", "https://pbs.twimg.com/tweet_video_thumb/g.jpg"));
        assertThat(TweetVideoExtractor.parseVideo("{\"mediaDetails\": []}", null)).isNull();
        assertThat(TweetVideoExtractor.isGif("https://video.twimg.com/tweet_video/g.mp4")).isTrue();
        assertThat(TweetVideoExtractor.isGif("https://video.twimg.com/amplify_video/1/vid/a.mp4")).isFalse();
    }

    @Test
    void replacesThumbnailWithVideo() {
        String html = TweetVideoExtractor.withVideo("<p>Text</p><p><img src=\"" + THUMB + "?name=900x900\" alt=\"\"></p>",
                "https://video.twimg.com/v.mp4", THUMB);

        assertThat(html).isEqualTo("<p>Text</p><p><video src=\"https://video.twimg.com/v.mp4\" playsinline=\"\" preload=\"metadata\" poster=\""
                + THUMB + "\" controls=\"\"></video></p>");
    }

    @Test
    void movesVideoOutOfPhotosAndAddsItWhenThumbnailIsntShown() {
        String html = TweetVideoExtractor.withVideo("<p>Text</p><div class=\"tweet-photos\"><img src=\"" + THUMB + "\" alt=\"\"></div>",
                "https://video.twimg.com/tweet_video/g.mp4", THUMB);

        assertThat(html).isEqualTo("<p>Text</p><video src=\"https://video.twimg.com/tweet_video/g.mp4\" playsinline=\"\" preload=\"metadata\" poster=\""
                + THUMB + "\" autoplay=\"\" loop=\"\" muted></video>");
        assertThat(TweetVideoExtractor.withVideo("<p>Text</p>", "https://video.twimg.com/v.mp4", null))
                .isEqualTo("<p>Text</p><video src=\"https://video.twimg.com/v.mp4\" playsinline=\"\" preload=\"metadata\" controls=\"\"></video>");
    }
}
