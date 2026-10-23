package eu.apps4net.feedora.utilities;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Finds the RSS/Atom feeds of a web page, for when the user adds a site's URL instead of its feed's URL.
 * Reads the page's <link rel="alternate"> tags, and if it has none, tries the usual feed paths of the site.
 */
public final class FeedDiscovery {

    public record DiscoveredFeed(String url, String title) {}

    private static final Set<String> FEED_TYPES = Set.of(
            "application/rss+xml", "application/atom+xml", "application/rdf+xml", "application/feed+xml");

    // Tried in this order when the page doesn't link to its feeds
    private static final List<String> COMMON_PATHS = List.of(
            "/feed", "/rss", "/feed.xml", "/rss.xml", "/atom.xml", "/index.xml", "/feeds/posts/default");

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    // Enough of the start of a response to see its root element
    private static final int SNIFF_SIZE = 4096;
    private static final Pattern FEED_ROOT = Pattern.compile("(?i)<(rss|feed|rdf:RDF)[\\s>]");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT)
            .build();

    private FeedDiscovery() {}

    /**
     * Returns the feeds of a web page, in the order the page lists them.
     *
     * @param url The page URL
     * @return The feeds found, empty if the page has none
     * @throws Exception if the page can't be downloaded
     */
    public static List<DiscoveredFeed> discover(String url) throws Exception {
        Document page = ArticleExtractor.fetch(url);

        List<DiscoveredFeed> feeds = linkedFeeds(page);
        if (!feeds.isEmpty()) return feeds;

        return probeCommonPaths(page.location());
    }

    /**
     * The feeds of the page's <link rel="alternate" type="application/rss+xml" href="..."> tags.
     */
    static List<DiscoveredFeed> linkedFeeds(Document page) {
        Map<String, DiscoveredFeed> feeds = new LinkedHashMap<>();
        for (Element link : page.select("link[rel~=(?i)^alternate$][type][href]")) {
            String type = link.attr("type").trim().toLowerCase();
            String href = link.absUrl("href");
            if (!FEED_TYPES.contains(type) || href.isEmpty()) continue;

            String title = link.attr("title").trim();
            feeds.putIfAbsent(href, new DiscoveredFeed(href, title.isEmpty() ? null : title));
        }
        return new ArrayList<>(feeds.values());
    }

    /**
     * Tries the usual feed paths at the site's root, all at once, and returns the ones that are feeds.
     */
    private static List<DiscoveredFeed> probeCommonPaths(String pageUrl) {
        URI base = URI.create(pageUrl);
        String root = base.getScheme() + "://" + base.getRawAuthority();

        List<CompletableFuture<String>> probes = COMMON_PATHS.stream()
                .map(path -> probe(root + path))
                .toList();

        // Different paths can redirect to the same feed (e.g. /rss -> /feed)
        Map<String, DiscoveredFeed> feeds = new LinkedHashMap<>();
        for (CompletableFuture<String> probe : probes) {
            String feedUrl = probe.join();
            if (feedUrl != null) feeds.putIfAbsent(feedUrl, new DiscoveredFeed(feedUrl, null));
        }
        return new ArrayList<>(feeds.values());
    }

    /**
     * Downloads the start of a URL, and completes with its final URL if it's a feed, otherwise with null.
     */
    private static CompletableFuture<String> probe(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", "Feedora RSS Reader")
                .header("Accept", "application/rss+xml,application/atom+xml,application/xml;q=0.9,*/*;q=0.8")
                .GET()
                .build();
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    try (InputStream in = response.body()) {
                        if (response.statusCode() != 200) return null;
                        String start = new String(in.readNBytes(SNIFF_SIZE), StandardCharsets.UTF_8);
                        return FEED_ROOT.matcher(start).find() ? response.uri().toString() : null;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .exceptionally(e -> null);
    }
}
