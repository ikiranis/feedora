package eu.apps4net.feedora.utilities;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.services.FeedService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class RssFetcher {

    /**
     * Fixes common XML errors in RSS feeds using Jsoup and removes control characters.
     */
    private static String fixXml(String xml) {
        // Remove all control characters except tab (\t), newline (\n), and carriage return (\r)
        xml = xml.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]", "");

        // Ensure XML declaration is present
        if (!xml.trim().startsWith("<?xml")) {
            xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xml;
        }

        // Clean with Jsoup
        Document doc = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser());
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
        return doc.outerHtml();
    }

    /**
     * Fetches and parses an RSS/Atom feed from the given URL.
     *
     * @param feed The Feed entity
     * @param feedService The FeedService instance
     * @return List of SyndEntry (feed posts)
     * @throws Exception if fetching or parsing fails
     */
    public static List<SyndEntry> fetch(Feed feed, FeedService feedService) throws Exception {
        List<SyndEntry> entries = new ArrayList<>();
        String xmlContent;
        // Use HttpURLConnection to follow redirects and check content type
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) URI.create(feed.getXmlUrl()).toURL().openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "FeedoraBot/1.0");
        int status = conn.getResponseCode();

        // Follow up to 5 redirects manually if needed
        int redirects = 0;
        while ((status == 301 || status == 302 || status == 303 || status == 307 || status == 308) && redirects < 5) {
            String newUrl = conn.getHeaderField("Location");
            if (newUrl == null) break;
            conn = (java.net.HttpURLConnection) URI.create(newUrl).toURL().openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "FeedoraBot/1.0");
            status = conn.getResponseCode();
            redirects++;
        }

        String contentType = conn.getContentType();
        try (InputStream is = conn.getInputStream()) {
            xmlContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Check if content is likely XML
        String xmlStart = xmlContent.trim().substring(0, Math.min(100, xmlContent.trim().length())).toLowerCase();
        if ((contentType != null && contentType.contains("html")) || xmlStart.startsWith("<html") || xmlStart.contains("<html")) {
            System.err.println("Feed URL returned HTML, not XML: " + feed.getXmlUrl());
            if (feedService != null) {
                feedService.removeFeedById(feed.getId());
            }
            return entries; // Return empty list
        }

        String fixedXml = fixXml(xmlContent);

        try (XmlReader reader = new XmlReader(new ByteArrayInputStream(fixedXml.getBytes(StandardCharsets.UTF_8)))) {
            SyndFeed newFeed = new SyndFeedInput().build(reader);
            entries.addAll(newFeed.getEntries());
        } catch (Exception e) {
            // Log a snippet of the problematic XML for debugging
            System.err.println("Error parsing feed: " + feed.getXmlUrl() + " - " + e.getMessage());
            System.err.println("XML snippet: " + fixedXml.substring(0, Math.min(1000, fixedXml.length())));
            throw e;
        }
        return entries;
    }
}
