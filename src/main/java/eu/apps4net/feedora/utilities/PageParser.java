package eu.apps4net.feedora.utilities;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEnclosureImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.SyndFeedOutput;
import eu.apps4net.feedora.exceptions.PageBlockedException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reads the posts of a web page that has no feed, from the elements that repeat on it (e.g. the cards of a news list),
 * and turns them into an RSS feed.
 */
public final class PageParser {
    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MILLIS = 15000;
    private static final int MAX_BODY_SIZE = 10 * 1024 * 1024;

    // An element has to repeat at least this many times to hold the posts
    private static final int MIN_REPEATS = 3;
    private static final int MIN_TITLE_LENGTH = 10;
    private static final int MAX_CANDIDATES = 10;
    private static final int SAMPLE_COUNT = 3;
    private static final int MAX_ITEMS = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private static final Pattern CSS_IDENTIFIER = Pattern.compile("-?[_a-zA-Z][_a-zA-Z0-9-]*");

    // Elements that never hold a post
    private static final Set<String> SKIPPED_TAGS = Set.of("script", "style", "noscript", "template", "svg", "path",
            "option", "br", "hr", "img", "source", "picture", "meta", "link", "input", "button", "select", "iframe");

    // Parts of the page that hold menus and links, not posts
    private static final String NAVIGATION = "nav, footer, header, aside, [role=navigation], [role=menu]";

    // Found only on the challenge pages of anti-bot services, never on their protected pages
    private static final List<String> CHALLENGE_MARKERS = List.of("cf_chl_opt", "cf-browser-verification",
            "cf-challenge", "captcha-delivery.com", "_incapsula_resource", "px-captcha", "ddos-guard");
    private static final List<String> CHALLENGE_TITLES = List.of("just a moment", "attention required",
            "access denied", "are you a robot", "verify you are human", "security check", "captcha", "ddos-guard");
    // Captchas that a normal page may also have (e.g. in a contact form), a block only when the page has nothing else
    private static final List<String> CAPTCHA_MARKERS = List.of("g-recaptcha", "h-captcha", "hcaptcha.com",
            "cf-turnstile", "recaptcha/api");
    private static final int MIN_CONTENT_LENGTH = 1000;

    private PageParser() {}

    /**
     * A post read from the page
     */
    public record PageItem(String title, String link, String description, String image, Date published) {}

    /**
     * Repeated elements of a page that may hold its posts
     *
     * @param selector CSS selector of the elements
     * @param count Number of posts found with the selector
     * @param samples The first posts, so the user can tell which candidate is the right one
     */
    public record Candidate(String selector, int count, List<PageItem> samples) {}

    /**
     * Downloads and parses a web page.
     *
     * @throws PageBlockedException if the page answers with a captcha, an anti-bot challenge or an error status
     * @throws Exception if the page can't be downloaded
     */
    public static Document fetch(String url) throws Exception {
        Connection.Response response = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .timeout(TIMEOUT_MILLIS)
                .maxBodySize(MAX_BODY_SIZE)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .execute();
        response.bufferUp();
        Document doc = response.parse();

        String reason = blockReason(response.statusCode(), response.header("Server"), doc);
        if (reason != null) {
            throw new PageBlockedException(reason);
        }
        return doc;
    }

    /**
     * Why the page can't be parsed, or null if it can be
     */
    static String blockReason(int status, String server, Document doc) {
        String html = doc.html().toLowerCase();
        String title = doc.title().toLowerCase();
        boolean cloudflare = server != null && server.toLowerCase().contains("cloudflare");

        for (String marker : CHALLENGE_MARKERS) {
            if (html.contains(marker)) {
                return "The page is protected by an anti-bot challenge (" + marker + ")";
            }
        }
        for (String marker : CHALLENGE_TITLES) {
            if (title.contains(marker)) {
                return "The page asks to verify that the visitor is human (\"" + doc.title() + "\")";
            }
        }

        if (status == 403 || status == 429 || status == 503) {
            return cloudflare
                    ? "The page is protected by Cloudflare (HTTP " + status + ")"
                    : "The page blocks the access (HTTP " + status + ")";
        }
        if (status >= 400) {
            return "The page responded with HTTP " + status;
        }

        Element body = doc.body();
        boolean littleContent = body == null || body.text().length() < MIN_CONTENT_LENGTH;
        if (littleContent) {
            for (String marker : CAPTCHA_MARKERS) {
                if (html.contains(marker)) {
                    return "The page asks for a captcha";
                }
            }
        }
        return null;
    }

    /**
     * Finds the elements that repeat on the page and look like posts (each one has a link and a title),
     * the most likely first.
     */
    public static List<Candidate> findCandidates(Document doc) {
        Element body = doc.body();
        if (body == null) {
            return List.of();
        }

        Map<String, Double> scores = new HashMap<>();
        Map<String, Candidate> candidates = new LinkedHashMap<>();

        for (Element parent : body.getAllElements()) {
            if (parent.childrenSize() < MIN_REPEATS || parent.closest(NAVIGATION) != null) {
                continue;
            }

            // Siblings of the same tag
            Map<String, List<Element>> byTag = new LinkedHashMap<>();
            for (Element child : parent.children()) {
                if (!SKIPPED_TAGS.contains(child.normalName())) {
                    byTag.computeIfAbsent(child.normalName(), tag -> new ArrayList<>()).add(child);
                }
            }

            for (Map.Entry<String, List<Element>> group : byTag.entrySet()) {
                List<Element> members = group.getValue();
                if (members.size() < MIN_REPEATS) {
                    continue;
                }

                List<Element> valid = members.stream().filter(member -> extractItem(member) != null).toList();
                if (valid.size() < MIN_REPEATS || valid.size() * 2 < members.size()) {
                    continue;
                }

                String selector = selectorFor(doc, parent, group.getKey(), commonClasses(valid), members.size());
                if (candidates.containsKey(selector)) {
                    continue;
                }

                List<PageItem> items;
                try {
                    items = extractItems(doc, selector);
                } catch (Selector.SelectorParseException e) {
                    continue;
                }
                long distinctLinks = items.stream().map(PageItem::link).distinct().count();
                if (distinctLinks < MIN_REPEATS) {
                    continue;
                }

                candidates.put(selector, new Candidate(selector, items.size(), items.subList(0, Math.min(SAMPLE_COUNT, items.size()))));
                scores.put(selector, score(items, members.size()));
            }
        }

        return candidates.values().stream()
                .sorted(Comparator.comparingDouble((Candidate candidate) -> scores.get(candidate.selector())).reversed())
                .limit(MAX_CANDIDATES)
                .toList();
    }

    /**
     * How much a group of elements looks like a list of posts: many items, long titles, images and descriptions
     */
    private static double score(List<PageItem> items, int groupSize) {
        double count = Math.min(items.size(), 30);
        double titleLength = Math.min(items.stream().mapToInt(item -> item.title().length()).average().orElse(0), 100);
        double images = items.stream().filter(item -> item.image() != null).count() / (double) items.size();
        double descriptions = items.stream().filter(item -> item.description() != null).count() / (double) items.size();
        double validRatio = Math.min(1, items.size() / (double) groupSize);
        return count * titleLength * (1 + images) * (1 + descriptions) * validRatio;
    }

    /**
     * The classes that all the elements have, usable in a CSS selector
     */
    private static Set<String> commonClasses(List<Element> elements) {
        Set<String> common = new TreeSet<>(elements.get(0).classNames());
        for (Element element : elements) {
            common.retainAll(element.classNames());
        }
        common.removeIf(name -> !CSS_IDENTIFIER.matcher(name).matches());
        return common;
    }

    /**
     * The shortest selector that finds the repeated elements: their tag and classes alone if they're used only for
     * posts across the page, else scoped to their parent.
     */
    private static String selectorFor(Document doc, Element parent, String tag, Set<String> classes, int groupSize) {
        String child = tag + classes.stream().map(name -> "." + name).collect(Collectors.joining());

        if (!classes.isEmpty()) {
            Elements global = doc.select(child);
            long valid = global.stream().filter(element -> extractItem(element) != null).count();
            if (global.size() >= groupSize && valid * 2 >= global.size()) {
                return child;
            }
        }

        String scoped = shortSelector(doc, parent) + " > " + child;
        if (doc.select(scoped).size() == groupSize) {
            return scoped;
        }
        return parent.cssSelector() + " > " + child;
    }

    private static String shortSelector(Document doc, Element element) {
        String id = element.id();
        if (!id.isEmpty() && CSS_IDENTIFIER.matcher(id).matches() && doc.select("#" + id).size() == 1) {
            return "#" + id;
        }
        return element.normalName() + element.classNames().stream()
                .filter(name -> CSS_IDENTIFIER.matcher(name).matches())
                .sorted()
                .map(name -> "." + name)
                .collect(Collectors.joining());
    }

    /**
     * Reads the posts from the elements the selector finds, skipping those without a link or title,
     * and the repeated links.
     *
     * @throws Selector.SelectorParseException if the selector is invalid
     */
    public static List<PageItem> extractItems(Document doc, String selector) {
        Map<String, PageItem> items = new LinkedHashMap<>();
        for (Element element : doc.select(selector)) {
            PageItem item = extractItem(element);
            if (item != null) {
                items.putIfAbsent(item.link(), item);
            }
            if (items.size() >= MAX_ITEMS) {
                break;
            }
        }
        return new ArrayList<>(items.values());
    }

    /**
     * Reads a post from an element: the link of its heading (or its longest link), its title, first paragraph,
     * first image and date. Null if it has no link or title.
     */
    static PageItem extractItem(Element element) {
        Element heading = element.selectFirst("h1, h2, h3, h4, h5, h6");
        Element link = null;
        if (heading != null) {
            link = heading.selectFirst("a[href]");
            if (link == null) {
                Element wrapping = heading.closest("a[href]");
                // The heading may be inside the post's link, which must be inside the element
                if (wrapping != null && (wrapping == element || wrapping.parents().contains(element))) {
                    link = wrapping;
                }
            }
        }
        if (link == null) {
            link = element.is("a[href]") ? element : element.select("a[href]").stream()
                    .filter(a -> isPostLink(a.absUrl("href")))
                    .max(Comparator.comparingInt(a -> a.text().length()))
                    .orElse(null);
        }
        if (link == null) {
            return null;
        }

        String href = link.absUrl("href");
        if (!isPostLink(href)) {
            return null;
        }

        String title = heading != null ? heading.text().trim() : link.text().trim();
        if (title.isEmpty()) {
            title = link.attr("title").trim();
        }
        if (title.isEmpty()) {
            Element image = link.selectFirst("img[alt]");
            title = image != null ? image.attr("alt").trim() : "";
        }
        if (title.length() < MIN_TITLE_LENGTH) {
            return null;
        }

        return new PageItem(title, href, description(element, title), image(element), published(element));
    }

    private static boolean isPostLink(String href) {
        return (href.startsWith("http://") || href.startsWith("https://")) && !href.contains("#");
    }

    private static String description(Element element, String title) {
        for (Element paragraph : element.select("p")) {
            String text = paragraph.text().trim();
            if (text.length() >= 20 && !text.equals(title)) {
                return text.length() > MAX_DESCRIPTION_LENGTH ? text.substring(0, MAX_DESCRIPTION_LENGTH) + "…" : text;
            }
        }
        return null;
    }

    private static String image(Element element) {
        for (Element img : element.select("img")) {
            // Lazy loaded images keep the real address in a data attribute
            for (String attribute : List.of("data-src", "data-lazy-src", "data-original", "src")) {
                String src = img.absUrl(attribute);
                if (!src.isEmpty() && !src.startsWith("data:")) {
                    return src;
                }
            }
            String srcset = img.hasAttr("data-srcset") ? img.attr("data-srcset") : img.attr("srcset");
            if (!srcset.isBlank()) {
                String src = resolve(img, srcset.trim().split("[\\s,]+")[0]);
                if (!src.isEmpty() && !src.startsWith("data:")) {
                    return src;
                }
            }
        }
        return null;
    }

    private static String resolve(Element element, String url) {
        try {
            return java.net.URI.create(element.baseUri()).resolve(url).toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static Date published(Element element) {
        Element time = element.selectFirst("time[datetime]");
        if (time == null) {
            return null;
        }
        String value = time.attr("datetime").trim();
        try {
            return Date.from(OffsetDateTime.parse(value).toInstant());
        } catch (Exception ignored) {}
        try {
            return Date.from(LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception ignored) {}
        try {
            return Date.from(LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Builds an RSS 2.0 feed of the posts.
     *
     * @param knownDates Dates of the posts already in the feed by their link, so they keep their date;
     *                   posts without a date on the page get the current time, in page order
     */
    public static String toRss(String title, String pageUrl, List<PageItem> items, Map<String, Date> knownDates) throws Exception {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        feed.setTitle(title);
        feed.setLink(pageUrl);
        feed.setDescription("Generated by Feedora from " + pageUrl);
        feed.setPublishedDate(new Date());

        long now = System.currentTimeMillis();
        List<SyndEntry> entries = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            PageItem item = items.get(i);
            SyndEntry entry = new SyndEntryImpl();
            entry.setTitle(item.title());
            entry.setLink(item.link());
            entry.setUri(item.link());

            Date published = item.published();
            if (published == null) {
                published = knownDates.get(item.link());
            }
            if (published == null) {
                // A second apart, so the first post of the page is the newest
                published = new Date(now - i * 1000L);
            }
            entry.setPublishedDate(published);

            StringBuilder html = new StringBuilder();
            if (item.image() != null) {
                html.append("<img src=\"").append(Entities.escape(item.image())).append("\" alt=\"\"/>");
                SyndEnclosure enclosure = new SyndEnclosureImpl();
                enclosure.setUrl(item.image());
                enclosure.setType(imageType(item.image()));
                entry.setEnclosures(List.of(enclosure));
            }
            if (item.description() != null) {
                html.append("<p>").append(Entities.escape(item.description())).append("</p>");
            }
            if (!html.isEmpty()) {
                SyndContent description = new SyndContentImpl();
                description.setType("text/html");
                description.setValue(html.toString());
                entry.setDescription(description);
            }
            entries.add(entry);
        }
        feed.setEntries(entries);

        return new SyndFeedOutput().outputString(feed);
    }

    private static String imageType(String url) {
        String path = url.toLowerCase().split("\\?")[0];
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".webp")) return "image/webp";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "image/jpeg";
    }
}
