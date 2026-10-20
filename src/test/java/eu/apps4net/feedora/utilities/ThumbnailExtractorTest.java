package eu.apps4net.feedora.utilities;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ThumbnailExtractorTest {

    private static SyndEntry parseItem(String item) throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<rss version=\"2.0\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\" xmlns:media=\"http://search.yahoo.com/mrss/\">"
                + "<channel><title>Feed</title><link>http://example.com/</link><description>Feed</description>"
                + "<item><title>Post</title><link>http://example.com/posts/1</link>" + item + "</item>"
                + "</channel></rss>";
        SyndFeed feed = new SyndFeedInput().build(new XmlReader(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
        return feed.getEntries().get(0);
    }

    @Test
    void usesFirstImageOfDescription() throws Exception {
        SyndEntry entry = parseItem("<description><![CDATA[<p>Text</p><img src=\"/a.jpg\"><img src=\"/b.jpg\">]]></description>"
                + "<enclosure url=\"http://example.com/c.jpg\" length=\"1\" type=\"image/jpeg\"/>");

        assertThat(ThumbnailExtractor.find(entry)).isEqualTo("http://example.com/a.jpg");
    }

    @Test
    void fallsBackToFullContent() throws Exception {
        SyndEntry entry = parseItem("<description>Text only</description>"
                + "<content:encoded><![CDATA[<p>Text</p><img src=\"http://cdn.example.com/a.jpg\">]]></content:encoded>");

        assertThat(ThumbnailExtractor.find(entry)).isEqualTo("http://cdn.example.com/a.jpg");
    }

    @Test
    void fallsBackToImageEnclosure() throws Exception {
        SyndEntry entry = parseItem("<description>Text only</description>"
                + "<enclosure url=\"http://example.com/a.mp3\" length=\"1\" type=\"audio/mpeg\"/>"
                + "<enclosure url=\"http://example.com/a.jpg\" length=\"1\" type=\"image/jpeg\"/>");

        assertThat(ThumbnailExtractor.find(entry)).isEqualTo("http://example.com/a.jpg");
    }

    @Test
    void fallsBackToMediaRss() throws Exception {
        SyndEntry content = parseItem("<description>Text only</description>"
                + "<media:content url=\"http://example.com/a.mp4\" type=\"video/mp4\"/>"
                + "<media:content url=\"http://example.com/a.jpg\" medium=\"image\"/>");
        SyndEntry thumbnail = parseItem("<description>Text only</description>"
                + "<media:group><media:thumbnail url=\"http://example.com/t.jpg\"/></media:group>");

        assertThat(ThumbnailExtractor.find(content)).isEqualTo("http://example.com/a.jpg");
        assertThat(ThumbnailExtractor.find(thumbnail)).isEqualTo("http://example.com/t.jpg");
    }

    @Test
    void returnsNullWithoutImages() throws Exception {
        SyndEntry entry = parseItem("<description><![CDATA[<p>Text</p><img src=\"data:image/png;base64,AAAA\">]]></description>");

        assertThat(ThumbnailExtractor.find(entry)).isNull();
    }

    @Test
    void attachedImagesListsEnclosuresAndMediaContentImagesOnce() throws Exception {
        SyndEntry entry = parseItem("<description>Text</description>"
                + "<enclosure url=\"http://example.com/a.jpg\" length=\"1\" type=\"image/jpeg\"/>"
                + "<media:content medium=\"image\" url=\"http://example.com/b.jpg\"/>"
                + "<media:content medium=\"video\" url=\"http://example.com/v.mp4\"/>"
                + "<media:group><media:content type=\"image/png\" url=\"/c.png\"/><media:content medium=\"image\" url=\"http://example.com/b.jpg\"/></media:group>");

        assertThat(ThumbnailExtractor.attachedImages(entry))
                .containsExactly("http://example.com/a.jpg", "http://example.com/b.jpg", "http://example.com/c.png");
    }

    @Test
    void attachedVideoIsTheFirstHttpsMp4MediaContent() throws Exception {
        SyndEntry entry = parseItem("<description>Text</description>"
                + "<media:content medium=\"image\" url=\"https://pbs.twimg.com/p.jpg\"/>"
                + "<media:content medium=\"video\" url=\"http://example.com/insecure.mp4\"/>"
                + "<media:group><media:content type=\"video/mp4\" medium=\"video\" url=\"https://video.twimg.com/v.mp4\"/></media:group>");

        assertThat(ThumbnailExtractor.attachedVideo(entry)).isEqualTo("https://video.twimg.com/v.mp4");
        assertThat(ThumbnailExtractor.attachedVideo(parseItem("<description>Text</description>"))).isNull();
    }
}
