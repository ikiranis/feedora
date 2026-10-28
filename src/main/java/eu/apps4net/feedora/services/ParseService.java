package eu.apps4net.feedora.services;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.exceptions.PageBlockedException;
import eu.apps4net.feedora.models.Parse;
import eu.apps4net.feedora.models.ParseDTO;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.ParseRepository;
import eu.apps4net.feedora.utilities.PageParser;
import org.jsoup.nodes.Document;
import org.jsoup.select.QueryParser;
import org.jsoup.select.Selector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ParseService {
    @Autowired
    private ParseRepository parseRepository;
    @Autowired
    private FeedRepository feedRepository;
    @Autowired
    private FeedService feedService;

    // Folder of the generated RSS files
    @Value("${feedora.parser.feeds-dir:feeds}")
    private String feedsDir;

    // Address the feed parser downloads the generated feeds from, i.e. this server
    @Value("${feedora.parser.feed-base-url:http://localhost:${server.port:9999}}")
    private String feedBaseUrl;

    private static final Pattern FEED_FILE_NAME = Pattern.compile("[a-z0-9][a-z0-9.-]*\\.xml");

    /**
     * Downloads a page and finds its repeated elements, for the user to choose the one that holds the posts.
     *
     * @param url The page's URL
     * @return The page's final URL and title, and the candidate elements, the most likely first
     * @throws Exception if the page blocks the access, can't be downloaded or has no repeated elements
     */
    public Map<String, Object> analyze(String url) throws Exception {
        url = normalizeUrl(url);
        Document doc = fetch(url);

        List<PageParser.Candidate> candidates = PageParser.findCandidates(doc);
        if (candidates.isEmpty()) {
            throw new Exception(Language.getActionString("No repeated elements with posts found on this page"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("url", doc.location().isEmpty() ? url : doc.location());
        result.put("title", doc.title().isBlank() ? URI.create(url).getHost() : doc.title().trim());
        result.put("candidates", candidates);
        return result;
    }

    /**
     * The pages the user parses, the newest first
     */
    public List<ParseDTO> getParses(User user) {
        return parseRepository.findAllByUserOrderByCreatedAtDesc(user).stream().map(parse -> toDTO(parse, user)).toList();
    }

    /**
     * Adds a page to parse, and parses it right away so its feed exists.
     *
     * @param url The page's URL
     * @param title Title of the generated feed, the page's domain if empty
     * @param selector CSS selector of the elements that hold the posts
     * @param frequency Minutes between two parses, the default if null
     * @return The parsed page
     * @throws Exception if a value is invalid, or the user already parses the page
     */
    public ParseDTO addParse(String url, String title, String selector, Integer frequency, User user) throws Exception {
        url = normalizeUrl(url);
        selector = validateSelector(selector);
        int minutes = validateFrequency(frequency);

        if (parseRepository.existsByUrlAndUser(url, user)) {
            throw new Exception(Language.getActionString("This page is already parsed"));
        }

        String host = URI.create(url).getHost();
        String feedTitle = title == null || title.isBlank() ? host : title.trim();
        Parse parse = parseRepository.save(new Parse(url, feedTitle, selector, minutes, uniqueFeedName(host), user));

        return toDTO(parse(parse), user);
    }

    /**
     * Changes the title, selector and frequency of a parsed page, and parses it again with them.
     */
    public ParseDTO updateParse(String parseId, String title, String selector, Integer frequency, User user) throws Exception {
        Parse parse = findParse(parseId, user);
        selector = validateSelector(selector);
        int minutes = validateFrequency(frequency);

        if (title != null && !title.isBlank()) {
            parse.setTitle(title.trim());
        }
        parse.setSelector(selector);
        parse.setFrequency(minutes);
        parse = parseRepository.save(parse);

        return toDTO(parse(parse), user);
    }

    /**
     * Stops parsing a page and deletes its feed file. A feed added from it stays in the user's feeds.
     */
    public String deleteParse(String parseId, User user) throws Exception {
        Parse parse = findParse(parseId, user);
        deleteFeedFile(parse);
        parseRepository.delete(parse);
        return Language.getActionString("Page deleted successfully");
    }

    /**
     * Deletes all the parsed pages of a user, with their feed files
     */
    public void deleteAllByUser(User user) {
        List<Parse> parses = parseRepository.findAllByUser(user);
        parses.forEach(this::deleteFeedFile);
        parseRepository.deleteAll(parses);
    }

    /**
     * Parses a page now, regardless of its frequency
     */
    public ParseDTO parseNow(String parseId, User user) throws Exception {
        return toDTO(parse(findParse(parseId, user)), user);
    }

    /**
     * Adds the generated feed of a page to the user's feeds
     *
     * @param folderId The folder of the feed, null or empty for the default folder
     */
    public String addToFeeds(String parseId, String folderId, User user) throws Exception {
        Parse parse = findParse(parseId, user);
        return feedService.addSingleFeed(feedUrl(parse), folderId, parse.getTitle(), user);
    }

    /**
     * Parses the pages of every user whose frequency has passed since their last parse.
     * A failure is kept in the page's lastError, so one page never stops the others.
     */
    public void parseDuePages() {
        LocalDateTime now = LocalDateTime.now();
        for (Parse parse : parseRepository.findAll()) {
            if (parse.isDue(now)) {
                try {
                    parse(parse);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Downloads the page, reads its posts with the page's selector and writes them to its feed file.
     * Synchronized, so the scheduled and the manual parses never write the same file together.
     *
     * @return The saved page, with the result of the parse
     */
    public synchronized Parse parse(Parse parse) {
        try {
            Document doc = fetch(parse.getUrl());
            List<PageParser.PageItem> items = PageParser.extractItems(doc, parse.getSelector());
            if (items.isEmpty()) {
                throw new Exception(Language.getActionString("No posts found with the selected element"));
            }
            writeFeedFile(parse, items);
            parse.setLastItemCount(items.size());
            parse.setLastError(null);
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            parse.setLastError(message.length() > 2000 ? message.substring(0, 2000) : message);
        }
        parse.setLastParsedAt(LocalDateTime.now());
        return parseRepository.save(parse);
    }

    /**
     * The generated feed file with this name, or null if there's none
     *
     * @param fileName The file's name, e.g. example.com.xml
     */
    public Path getFeedFile(String fileName) {
        if (fileName == null || !FEED_FILE_NAME.matcher(fileName).matches()) {
            return null;
        }
        Path file = feedsPath().resolve(fileName).normalize();
        return file.startsWith(feedsPath()) && Files.isRegularFile(file) ? file : null;
    }

    /**
     * The URL of a page's generated feed, as the feed parser downloads it
     */
    public String feedUrl(Parse parse) {
        return feedBaseUrl + "/api/parser/feed/" + parse.getFeedName() + ".xml";
    }

    private ParseDTO toDTO(Parse parse, User user) {
        ParseDTO dto = ParseDTO.fromParse(parse);
        dto.inFeeds = feedRepository.existsByXmlUrlAndUser(feedUrl(parse), user);
        return dto;
    }

    private Document fetch(String url) throws Exception {
        try {
            return PageParser.fetch(url);
        } catch (PageBlockedException e) {
            throw new PageBlockedException(Language.getActionString("The page blocks the access") + ": " + e.getMessage());
        }
    }

    private void writeFeedFile(Parse parse, List<PageParser.PageItem> items) throws Exception {
        Path file = feedsPath().resolve(parse.getFeedName() + ".xml");
        String rss = PageParser.toRss(parse.getTitle(), parse.getUrl(), items, knownDates(file));

        Files.createDirectories(file.getParent());
        // Written aside and moved in place, so the feed is never read half written
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, rss, StandardCharsets.UTF_8);
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Dates of the posts already in a feed file by their link, empty if the file doesn't exist or can't be read
     */
    private Map<String, Date> knownDates(Path file) {
        Map<String, Date> dates = new HashMap<>();
        if (!Files.isRegularFile(file)) {
            return dates;
        }
        try (XmlReader reader = new XmlReader(file.toFile())) {
            for (SyndEntry entry : new SyndFeedInput().build(reader).getEntries()) {
                if (entry.getLink() != null && entry.getPublishedDate() != null) {
                    dates.put(entry.getLink(), entry.getPublishedDate());
                }
            }
        } catch (Exception e) {
            // A broken file is replaced, its posts get new dates
        }
        return dates;
    }

    private void deleteFeedFile(Parse parse) {
        try {
            Files.deleteIfExists(feedsPath().resolve(parse.getFeedName() + ".xml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Path feedsPath() {
        return Path.of(feedsDir).toAbsolutePath().normalize();
    }

    private Parse findParse(String parseId, User user) throws Exception {
        UUID id;
        try {
            id = UUID.fromString(parseId);
        } catch (Exception e) {
            throw new Exception(Language.getActionString("Invalid page ID"));
        }

        Parse parse = parseRepository.findById(id)
                .orElseThrow(() -> new Exception(Language.getActionString("Page not found")));
        if (!parse.getUser().getId().equals(user.getId())) {
            throw new Exception(Language.getActionString("Page not found"));
        }
        return parse;
    }

    /**
     * The page's main domain (without www.) as the feed's file name, numbered when another page already uses it
     */
    private String uniqueFeedName(String host) {
        String base = host.toLowerCase().replaceFirst("^www\\.", "").replaceAll("[^a-z0-9.-]", "-");
        String name = base;
        for (int i = 2; parseRepository.existsByFeedName(name); i++) {
            name = base + "-" + i;
        }
        return name;
    }

    static String normalizeUrl(String url) throws Exception {
        if (url == null || url.isBlank()) {
            throw new Exception(Language.getActionString("Please enter a valid URL"));
        }
        url = url.trim();
        if (!url.matches("(?i)^https?://.*")) {
            url = "https://" + url;
        }
        try {
            URI uri = URI.create(url);
            if (uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new Exception(Language.getActionString("Please enter a valid URL"));
        }
        return url;
    }

    private static String validateSelector(String selector) throws Exception {
        if (selector == null || selector.isBlank()) {
            throw new Exception(Language.getActionString("Please select the element of the posts"));
        }
        try {
            QueryParser.parse(selector.trim());
        } catch (Selector.SelectorParseException e) {
            throw new Exception(Language.getActionString("Invalid CSS selector") + ": " + e.getMessage());
        }
        return selector.trim();
    }

    private static int validateFrequency(Integer frequency) throws Exception {
        if (frequency == null) {
            return Parse.DEFAULT_FREQUENCY_MINUTES;
        }
        if (frequency < Parse.MIN_FREQUENCY_MINUTES || frequency > Parse.MAX_FREQUENCY_MINUTES) {
            throw new Exception(Language.getActionString("The frequency must be between {0} and {1} minutes")
                    .replace("{0}", String.valueOf(Parse.MIN_FREQUENCY_MINUTES))
                    .replace("{1}", String.valueOf(Parse.MAX_FREQUENCY_MINUTES)));
        }
        return frequency;
    }
}
