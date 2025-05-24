package eu.apps4net.feedora.utilities;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class RssFetcher {
    /**
     * Fetches and parses an RSS/Atom feed from the given URL.
     *
     * @param feedUrl The URL of the RSS/Atom feed
     * @return List of SyndEntry (feed posts)
     * @throws Exception if fetching or parsing fails
     */
    public static List<SyndEntry> fetch(String feedUrl) {
        List<SyndEntry> entries = new ArrayList<>();
        try (XmlReader reader = new XmlReader(new URL(feedUrl))) {
            SyndFeed feed = new SyndFeedInput().build(reader);
            entries.addAll(feed.getEntries());
        } catch (Exception e) {
            // Log and skip feeds with DOCTYPE or other parse errors
            System.err.println("Failed to parse feed: " + feedUrl + " - " + e.getMessage());
        }
        return entries;
    }
}
