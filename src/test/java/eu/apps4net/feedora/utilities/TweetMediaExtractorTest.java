package eu.apps4net.feedora.utilities;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TweetMediaExtractorTest {

    private static final String THUMB = "https://pbs.twimg.com/amplify_video_thumb/1/img/a.jpg";

    @Test
    void recognisesVideoThumbnails() {
        assertThat(TweetMediaExtractor.isVideoThumbnail(THUMB)).isTrue();
        assertThat(TweetMediaExtractor.isVideoThumbnail("https://pbs.twimg.com/ext_tw_video_thumb/1/pu/img/a.jpg")).isTrue();
        assertThat(TweetMediaExtractor.isVideoThumbnail("https://pbs.twimg.com/tweet_video_thumb/a.jpg")).isTrue();
        assertThat(TweetMediaExtractor.isVideoThumbnail("https://pbs.twimg.com/media/a.jpg")).isFalse();
        assertThat(TweetMediaExtractor.isVideoThumbnail(null)).isFalse();
    }

    @Test
    void findsThumbnailInImagesOrDescription() {
        assertThat(TweetMediaExtractor.findThumbnail("https://pbs.twimg.com/media/a.jpg", List.of(THUMB), null)).isEqualTo(THUMB);
        assertThat(TweetMediaExtractor.findThumbnail(null, List.of(), "<p>Text</p><p><img src=\"" + THUMB + "\"></p>")).isEqualTo(THUMB);
        assertThat(TweetMediaExtractor.findThumbnail("https://pbs.twimg.com/media/a.jpg", List.of(), "<p>Text</p>")).isNull();
    }

    @Test
    void checksTweetsWithVideoThumbnailOrNoImage() {
        // Some feeds give a video tweet only its text
        assertThat(TweetMediaExtractor.mayHaveMissingMedia(null, "<p>Text</p>")).isTrue();
        assertThat(TweetMediaExtractor.mayHaveMissingMedia(null, null)).isTrue();
        assertThat(TweetMediaExtractor.mayHaveMissingMedia(THUMB, "<p>Text</p>")).isTrue();
        assertThat(TweetMediaExtractor.mayHaveMissingMedia(null, "<p>Text</p><p><img src=\"" + THUMB + "\"></p>")).isTrue();
        assertThat(TweetMediaExtractor.mayHaveMissingMedia("https://pbs.twimg.com/media/a.jpg", "<p>Text</p>")).isFalse();
        assertThat(TweetMediaExtractor.mayHaveMissingMedia(null, "<p>Text</p><p><img src=\"https://pbs.twimg.com/media/a.jpg\"></p>")).isFalse();
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

        assertThat(TweetMediaExtractor.parseMedia(json, THUMB + "?format=jpg").video())
                .isEqualTo(new TweetMediaExtractor.TweetVideo("https://video.twimg.com/720.mp4", THUMB));
        assertThat(TweetMediaExtractor.parseMedia(json, null).video().url()).isEqualTo("https://video.twimg.com/other.mp4");
    }

    @Test
    void findsVideoOfQuotedTweetAndGifs() throws Exception {
        String json = """
                {"mediaDetails": [], "quoted_tweet": {"mediaDetails": [{"type": "animated_gif",
                  "media_url_https": "https://pbs.twimg.com/tweet_video_thumb/g.jpg",
                  "video_info": {"variants": [{"content_type": "video/mp4", "bitrate": 0, "url": "https://video.twimg.com/tweet_video/g.mp4"}]}}]}}""";

        assertThat(TweetMediaExtractor.parseMedia(json, null).video()).isEqualTo(new TweetMediaExtractor.TweetVideo(
                "https://video.twimg.com/tweet_video/g.mp4", "https://pbs.twimg.com/tweet_video_thumb/g.jpg"));
        assertThat(TweetMediaExtractor.parseMedia("{\"mediaDetails\": []}", null)).isEqualTo(new TweetMediaExtractor.TweetMedia(null, List.of()));
        assertThat(TweetMediaExtractor.isGif("https://video.twimg.com/tweet_video/g.mp4")).isTrue();
        assertThat(TweetMediaExtractor.isGif("https://video.twimg.com/amplify_video/1/vid/a.mp4")).isFalse();
    }

    @Test
    void findsPhotosOfTweetOrElseOfQuotedTweet() throws Exception {
        String json = """
                {"mediaDetails": [
                  {"type": "photo", "media_url_https": "https://pbs.twimg.com/media/a.jpg"},
                  {"type": "photo", "media_url_https": "https://pbs.twimg.com/media/b.jpg"}],
                 "quoted_tweet": {"mediaDetails": [{"type": "photo", "media_url_https": "https://pbs.twimg.com/media/q.jpg"}]}}""";

        assertThat(TweetMediaExtractor.parseMedia(json, null))
                .isEqualTo(new TweetMediaExtractor.TweetMedia(null, List.of("https://pbs.twimg.com/media/a.jpg", "https://pbs.twimg.com/media/b.jpg")));
        assertThat(TweetMediaExtractor.parseMedia("{\"quoted_tweet\": {\"mediaDetails\": [{\"type\": \"photo\", "
                + "\"media_url_https\": \"https://pbs.twimg.com/media/q.jpg\"}]}}", null).photos())
                .containsExactly("https://pbs.twimg.com/media/q.jpg");
    }

    @Test
    void replacesThumbnailWithVideo() {
        String html = TweetMediaExtractor.withVideo("<p>Text</p><p><img src=\"" + THUMB + "?name=900x900\" alt=\"\"></p>",
                "https://video.twimg.com/v.mp4", THUMB);

        assertThat(html).isEqualTo("<p>Text</p><p><video src=\"https://video.twimg.com/v.mp4\" playsinline=\"\" preload=\"metadata\" poster=\""
                + THUMB + "\" controls=\"\"></video></p>");
    }

    @Test
    void movesVideoOutOfPhotosAndAddsItWhenThumbnailIsntShown() {
        String html = TweetMediaExtractor.withVideo("<p>Text</p><div class=\"tweet-photos\"><img src=\"" + THUMB + "\" alt=\"\"></div>",
                "https://video.twimg.com/tweet_video/g.mp4", THUMB);

        assertThat(html).isEqualTo("<p>Text</p><video src=\"https://video.twimg.com/tweet_video/g.mp4\" playsinline=\"\" preload=\"metadata\" poster=\""
                + THUMB + "\" autoplay=\"\" loop=\"\" muted></video>");
        assertThat(TweetMediaExtractor.withVideo("<p>Text</p>", "https://video.twimg.com/v.mp4", null))
                .isEqualTo("<p>Text</p><video src=\"https://video.twimg.com/v.mp4\" playsinline=\"\" preload=\"metadata\" controls=\"\"></video>");
    }
}
