package eu.apps4net.feedora.services;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.LogLevel;
import eu.apps4net.feedora.models.LogType;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.FolderRepository;
import eu.apps4net.feedora.repositories.PostRepository;
import eu.apps4net.feedora.utilities.FaviconColorExtractor;
import eu.apps4net.feedora.utilities.YouTubeChannelAvatar;
import eu.apps4net.feedora.utilities.FeedDiscovery;
import eu.apps4net.feedora.utilities.YouTubeFeedResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class FeedService {
    @Autowired
    private FeedRepository feedRepository;
    @Autowired
    private FolderRepository folderRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private OperationLockService operationLockService;
    @Autowired
    private LogService logService;
    @Autowired
    private SettingService settingService;

    // Last parse time of the users whose feeds are being scanned, as it was before the scan
    private final Map<UUID, Optional<LocalDateTime>> lastParsedBeforeScan = new ConcurrentHashMap<>();

    /**
     * Imports feeds and folders from an OPML file and saves them to the database.
     * This method replaces all existing feeds and folders for the user with the new ones from the OPML file.
     *
     * @param opmlInputStream The OPML file input stream to import
     * @param user The user to relate the feeds and folders to
     * @return The number of feeds added
     * @throws Exception if there is an error reading or parsing the OPML file
     */
    @Transactional
    public int importOPML(InputStream opmlInputStream, User user) throws Exception {
        // Try to acquire lock to prevent conflicts with feed parsing
        if (!operationLockService.tryLockFeedOperation()) {
            throw new Exception("Cannot import OPML: Feed parsing operation is currently in progress. Please try again in a few moments.");
        }

        try {
            // Clear existing feeds, folders, and posts for the user
            clearUserFeeds(user);
            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(opmlInputStream);
            doc.getDocumentElement().normalize();

            NodeList folderNodes = doc.getElementsByTagName("outline");
            Map<String, Folder> folderMap = new HashMap<>();
            int feedsAdded = 0;

        for (int i = 0; i < folderNodes.getLength(); i++) {
            Node node = folderNodes.item(i);
            if (node.getParentNode().getNodeName().equals("body")) {
                Element folderElem = (Element) node;
                // A feed directly under body has no folder
                if ("rss".equals(folderElem.getAttribute("type"))) {
                    feedRepository.save(new Feed(folderElem.getAttribute("title"), folderElem.getAttribute("xmlUrl"),
                            folderElem.getAttribute("htmlUrl"), "rss", null, user));
                    feedsAdded++;
                    continue;
                }
                String folderNameAttr = folderElem.getAttribute("title");
                String folderName = (folderNameAttr == null || folderNameAttr.isEmpty()) ? folderElem.getAttribute("text") : folderNameAttr;
                final String finalFolderName = folderName;
                Folder folder = folderRepository.findByNameAndUser(finalFolderName, user).orElseGet(() -> {
                    Folder f = new Folder(finalFolderName, user);
                    return folderRepository.save(f);
                });
                folderMap.put(finalFolderName, folder);

                NodeList feedsInFolder = folderElem.getChildNodes();
                for (int j = 0; j < feedsInFolder.getLength(); j++) {
                    Node feedNode = feedsInFolder.item(j);
                    if (feedNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element feedElem = (Element) feedNode;
                        if ("rss".equals(feedElem.getAttribute("type"))) {
                            String title = feedElem.getAttribute("title");
                            String xmlUrl = feedElem.getAttribute("xmlUrl");
                            String htmlUrl = feedElem.getAttribute("htmlUrl");
                            String type = feedElem.getAttribute("type");
                            // Since we cleared all feeds, we can always add new ones
                            Feed feed = new Feed(title, xmlUrl, htmlUrl, type, folder, user);
                            feedRepository.save(feed);
                            feedsAdded++;
                        }
                    }
                }
            }
        }
        return feedsAdded;
        } finally {
            // Always release the lock
            operationLockService.unlockFeedOperation();
        }
    }

    /**
     * Exports the feeds and folders of a user as an OPML document, in the structure that importOPML reads:
     * folders as top-level outlines with their feeds inside, and feeds without a folder directly under body.
     *
     * @param user The user whose feeds are exported
     * @return The OPML document as a string
     * @throws Exception if the document can't be built
     */
    public String exportOPML(User user) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element opml = doc.createElement("opml");
        opml.setAttribute("version", "1.0");
        doc.appendChild(opml);

        Element head = doc.createElement("head");
        Element title = doc.createElement("title");
        title.setTextContent("Feedora subscriptions");
        head.appendChild(title);
        opml.appendChild(head);

        Element body = doc.createElement("body");
        opml.appendChild(body);

        // Group the feeds by folder, keeping empty folders so they survive a re-import
        Map<UUID, Element> folderElements = new HashMap<>();
        List<Folder> folders = new ArrayList<>(folderRepository.findAllByUser(user));
        folders.sort(Comparator.comparing(Folder::getName, String.CASE_INSENSITIVE_ORDER));
        for (Folder folder : folders) {
            Element folderElem = doc.createElement("outline");
            folderElem.setAttribute("text", folder.getName());
            folderElem.setAttribute("title", folder.getName());
            body.appendChild(folderElem);
            folderElements.put(folder.getId(), folderElem);
        }

        List<Feed> feeds = new ArrayList<>(feedRepository.findByUser(user));
        feeds.sort(Comparator.comparing(feed -> Objects.toString(feed.getTitle(), ""), String.CASE_INSENSITIVE_ORDER));
        for (Feed feed : feeds) {
            Element feedElem = doc.createElement("outline");
            String feedTitle = Objects.toString(feed.getTitle(), "");
            feedElem.setAttribute("type", "rss");
            feedElem.setAttribute("text", feedTitle);
            feedElem.setAttribute("title", feedTitle);
            feedElem.setAttribute("xmlUrl", Objects.toString(feed.getXmlUrl(), ""));
            feedElem.setAttribute("htmlUrl", Objects.toString(feed.getHtmlUrl(), ""));

            Element parent = feed.getFolder() == null ? null : folderElements.get(feed.getFolder().getId());
            (parent != null ? parent : body).appendChild(feedElem);
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Returns all feeds in the database.
     *
     * @param user The user who owns the feeds
     * @return List of the user's Feed entities
     */
    public List<Feed> getAllFeeds(User user) {
        return feedRepository.findByUser(user);
    }

    /**
     * Returns paginated feeds for a given user.
     * @param user The user
     * @param page The page number (1-indexed)
     * @param pageSize The number of feeds per page
     * @return List of feeds
     */
    public List<Feed> getFeedsForUser(User user, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.ASC, "title"));
        return feedRepository.findByUser(user, pageable);
    }

    /**
     * Returns paginated feeds for a given user with search functionality.
     * @param user The user
     * @param page The page number (1-indexed)
     * @param pageSize The number of feeds per page
     * @param searchTerm The search term to filter feeds by title (case-insensitive)
     * @return List of feeds
     */
    public List<Feed> getFeedsForUser(User user, int page, int pageSize, String searchTerm) {
        // Sorted by the query: problematic feeds first, then the most frequent ones, then by title
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return feedRepository.findByUserAndTitleContainingProblemsFirst(user, searchTerm.trim(), pageable);
        } else {
            return feedRepository.findByUserProblemsFirst(user, pageable);
        }
    }

    /**
     * Removes a feed whose URL doesn't serve a feed anymore, and logs it.
     *
     * @param feed The feed to remove
     * @param reason Why the feed is removed, for the log
     */
    public void removeInvalidFeed(Feed feed, String reason) {
        try {
            feedRepository.deleteById(feed.getId());
            logService.logFeed(feed, LogLevel.WARNING, LogType.FEED_REMOVED, reason + ". The feed was removed.", null);
        } catch (Exception e) {
            logService.logFeed(feed, LogLevel.ERROR, LogType.FEED_REMOVED, reason + ". The feed couldn't be removed.", LogService.describe(e));
        }
    }

    /**
     * Returns when the feeds of a user were last parsed.
     *
     * @param user The user whose feeds to check
     * @return The last check of any of the user's feeds, or null if none was checked yet
     */
    public LocalDateTime getLastParsedAt(User user) {
        Optional<LocalDateTime> frozen = lastParsedBeforeScan.get(user.getId());
        if (frozen != null) {
            return frozen.orElse(null);
        }
        return feedRepository.findLastCheckedAtByUser(user);
    }

    /**
     * Keeps the last parse time of a user as it was before a scan, until the scan ends,
     * so it doesn't change with every feed checked during the scan.
     *
     * @param user The user whose feeds are scanned
     */
    public void freezeLastParsedAt(User user) {
        lastParsedBeforeScan.put(user.getId(), Optional.ofNullable(feedRepository.findLastCheckedAtByUser(user)));
    }

    /**
     * Makes the last parse time of a user follow the feeds' checks again, after a scan ended.
     *
     * @param user The user whose feeds were scanned
     */
    public void unfreezeLastParsedAt(User user) {
        lastParsedBeforeScan.remove(user.getId());
    }

    /**
     * Clears all feeds, folders, and posts for a specific user.
     * This is used before importing new OPML data to replace existing feeds.
     *
     * @param user The user whose feeds should be cleared
     */
    @Transactional
    public void clearUserFeeds(User user) {
        // Delete posts first (due to foreign key constraints)
        postRepository.deleteByUser(user);
        
        // Delete feeds
        feedRepository.deleteByUser(user);
        
        // Delete folders (after feeds are deleted)
        folderRepository.deleteByUser(user);
        
        System.out.println("[Feedora] Cleared all existing feeds, folders, and posts for user: " + user.getUsername());
    }

    /**
     * Adds a single feed to the database.
     *
     * @param url The RSS feed URL
     * @param folderId The folder ID (can be null for default)
     * @param title The feed title (optional)
     * @param user The user to add the feed for
     * @return Success message
     * @throws Exception if there is an error adding the feed
     */
    @Transactional
    public String addSingleFeed(String url, String folderId, String title, User user) throws Exception {
        url = YouTubeFeedResolver.resolve(url);

        // If no title provided, fetch it from the feed (a web page's URL is replaced by its feed's URL)
        Exception feedInfoError = null;
        if (title == null || title.trim().isEmpty()) {
            try {
                Map<String, Object> feedInfo = fetchFeedInfo(url);
                title = (String) feedInfo.get("title");
                url = (String) feedInfo.get("url");
            } catch (Exception e) {
                // If we can't fetch title, use URL as fallback
                title = url;
                feedInfoError = e;
            }
        }

        // Check if feed already exists for this user
        if (feedRepository.findByXmlUrlAndUser(url, user).isPresent()) {
            throw new Exception(Language.getActionString("Feed already exists"));
        }

        Folder folder = null;
        if (folderId != null && !folderId.isEmpty()) {
            folder = folderRepository.findById(UUID.fromString(folderId)).orElse(null);
            if (folder == null || !folder.getUser().getId().equals(user.getId())) {
                throw new Exception(Language.getActionString("Folder not found or doesn't belong to user"));
            }
        }

        Feed feed = new Feed(title, url, "", "rss", folder, user);
        feedRepository.save(feed);

        if (feedInfoError != null) {
            logService.logFeed(feed, LogLevel.WARNING, LogType.FEED_INFO,
                    "Couldn't read the feed's title, the URL is used instead", LogService.describe(feedInfoError));
        }
        
        return Language.getActionString("Feed added successfully");
    }

    /**
     * Fetches feed information from a URL.
     * If the URL is a web page instead of a feed, the page's feeds are found and the first one is read;
     * all of them are returned in "feeds", for the user to choose from.
     *
     * @param url The RSS feed URL, or the URL of a web page that has feeds
     * @return Map containing feed information
     * @throws Exception if there is an error fetching the feed
     */
    public Map<String, Object> fetchFeedInfo(String url) throws Exception {
        try {
            url = YouTubeFeedResolver.resolve(url);
            try {
                return readFeedInfo(url);
            } catch (Exception feedError) {
                List<FeedDiscovery.DiscoveredFeed> feeds;
                try {
                    feeds = FeedDiscovery.discover(url);
                } catch (Exception e) {
                    throw feedError;
                }
                if (feeds.isEmpty()) {
                    throw feedError;
                }

                Map<String, Object> feedInfo = readFeedInfo(feeds.get(0).url());
                feedInfo.put("feeds", feeds);
                return feedInfo;
            }
        } catch (Exception e) {
            throw new Exception(Language.getActionString("Failed to fetch feed info").replace("{0}", e.getMessage()));
        }
    }

    /**
     * Downloads a feed and reads its title and description.
     *
     * @throws Exception if the URL can't be downloaded or isn't an RSS/Atom feed
     */
    private Map<String, Object> readFeedInfo(String url) throws Exception {
        Map<String, Object> feedInfo = new HashMap<>();

        java.net.URI feedUri = java.net.URI.create(url);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) feedUri.toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Feedora RSS Reader");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        if (connection.getResponseCode() != 200) {
            throw new Exception("HTTP " + connection.getResponseCode() + ": " + connection.getResponseMessage());
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(connection.getInputStream());
        doc.getDocumentElement().normalize();

        // A well-formed XHTML page parses too, so check that it's a feed
        String root = doc.getDocumentElement().getNodeName();
        if (!root.equals("rss") && !root.equals("feed") && !root.endsWith("RDF")) {
            throw new Exception("Not an RSS or Atom feed");
        }

        String title = "";
        String description = "";

        // Try RSS format first
        NodeList titleNodes = doc.getElementsByTagName("title");
        if (titleNodes.getLength() > 0) {
            Element titleElement = (Element) titleNodes.item(0);
            if (titleElement.getParentNode().getNodeName().equals("channel")) {
                title = titleElement.getTextContent();
            }
        }

        // Try Atom format if RSS didn't work
        if (title.isEmpty()) {
            titleNodes = doc.getElementsByTagName("title");
            for (int i = 0; i < titleNodes.getLength(); i++) {
                Element titleElement = (Element) titleNodes.item(i);
                if (titleElement.getParentNode().getNodeName().equals("feed")) {
                    title = titleElement.getTextContent();
                    break;
                }
            }
        }

        // Get description
        NodeList descNodes = doc.getElementsByTagName("description");
        if (descNodes.getLength() > 0) {
            description = descNodes.item(0).getTextContent();
        }

        feedInfo.put("title", title.isEmpty() ? "Untitled Feed" : title);
        feedInfo.put("description", description);
        feedInfo.put("url", url);
        return feedInfo;
    }

    /**
     * Deletes a single feed for a specific user.
     *
     * @param feedId The UUID of the feed to delete
     * @param user The user who owns the feed
     * @return Success message
     * @throws Exception if the feed is not found or doesn't belong to the user
     */
    @Transactional
    public String deleteFeed(String feedId, User user) throws Exception {
        UUID uuid;
        try {
            uuid = UUID.fromString(feedId);
        } catch (IllegalArgumentException e) {
            throw new Exception(Language.getActionString("Invalid feed ID"));
        }

        Optional<Feed> feedOpt = feedRepository.findById(uuid);
        if (!feedOpt.isPresent()) {
            throw new Exception(Language.getActionString("Feed not found"));
        }

        Feed feed = feedOpt.get();
        if (!feed.getUser().getId().equals(user.getId())) {
            throw new Exception(Language.getActionString("Feed doesn't belong to user"));
        }

        // Delete posts first (due to foreign key constraints)
        postRepository.deleteByFeed(feed);
        
        // Then delete the feed and its log entries
        feedRepository.delete(feed);
        logService.deleteLogs(user, feed.getId(), null);
        return Language.getActionString("Feed deleted successfully");
    }

    /**
     * Updates a single feed for a specific user.
     *
     * @param feedId The UUID of the feed to update
     * @param title The new title for the feed
     * @param folderName The new folder name for the feed (can be null or empty for no folder)
     * @param cardColor The color of the feed's cards (#rrggbb), empty to use the favicon's color again, or null to keep the current one
     * @param user The user who owns the feed
     * @return Success message
     * @throws Exception if the feed is not found, doesn't belong to the user or the color is invalid
     */
    @Transactional
    public String updateFeed(String feedId, String title, String folderName, String cardColor, User user) throws Exception {
        UUID uuid;
        try {
            uuid = UUID.fromString(feedId);
        } catch (IllegalArgumentException e) {
            throw new Exception(Language.getActionString("Invalid feed ID"));
        }

        Optional<Feed> feedOpt = feedRepository.findById(uuid);
        if (!feedOpt.isPresent()) {
            throw new Exception(Language.getActionString("Feed not found"));
        }

        Feed feed = feedOpt.get();
        if (!feed.getUser().getId().equals(user.getId())) {
            throw new Exception(Language.getActionString("Feed doesn't belong to user"));
        }

        // Update title
        if (title != null && !title.trim().isEmpty()) {
            feed.setTitle(title.trim());
        }

        // Update folder
        Folder folder = null;
        if (folderName != null && !folderName.trim().isEmpty()) {
            // Try to find existing folder or create a new one
            Optional<Folder> folderOpt = folderRepository.findByNameAndUser(folderName.trim(), user);
            if (folderOpt.isPresent()) {
                folder = folderOpt.get();
            } else {
                // Create new folder
                folder = new Folder(folderName.trim(), user);
                folder = folderRepository.save(folder);
            }
        }
        feed.setFolder(folder);

        // Update the card color
        if (cardColor != null) {
            String color = cardColor.trim();
            if (color.isEmpty()) {
                feed.setCardColor(null);
            } else if (color.matches("#[0-9a-fA-F]{6}")) {
                feed.setCardColor(color.toLowerCase());
            } else {
                throw new Exception(Language.getActionString("Invalid color"));
            }
        }

        feedRepository.save(feed);
        return Language.getActionString("Feed updated successfully");
    }

    /**
     * Finds the main color of the feeds not checked yet (of all users), used as the light background of their cards:
     * the color of their website's favicon, or of the channel's avatar for YouTube channels (which all share
     * youtube.com's favicon). A batch at a time, so the scheduled parser isn't held up; feeds whose image can't be
     * downloaded (e.g. no network, or YouTube refusing its pages) are retried on the next run.
     *
     * @param limit Maximum number of feeds to check
     */
    public void updateFaviconColors(int limit) {
        List<Feed> feeds = feedRepository.findByFaviconColorIsNull(PageRequest.of(0, limit));
        if (feeds.isEmpty()) return;

        // YouTube channel feeds by channel id; the rest use their website's favicon
        Map<String, List<Feed>> channels = new LinkedHashMap<>();
        Map<Feed, String> sites = new LinkedHashMap<>();
        for (Feed feed : feeds) {
            String channelId = YouTubeChannelAvatar.channelId(feed.getHtmlUrl(), feed.getXmlUrl());
            if (channelId != null) {
                channels.computeIfAbsent(channelId, id -> new ArrayList<>()).add(feed);
            } else {
                sites.put(feed, feed.getHtmlUrl() != null && !feed.getHtmlUrl().isBlank() ? feed.getHtmlUrl() : feed.getXmlUrl());
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(8);
        sites.forEach((feed, website) -> executor.submit(() ->
                saveFaviconColor(List.of(feed), website, () -> website != null ? FaviconColorExtractor.mainColor(website) : "")));
        for (Map.Entry<String, String> avatar : channelAvatars(channels.keySet()).entrySet()) {
            String url = avatar.getValue();
            executor.submit(() -> saveFaviconColor(channels.get(avatar.getKey()), url,
                    () -> url.isEmpty() ? "" : FaviconColorExtractor.mainColorOfImage(url)));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Channels whose avatar is read from their page per run without an API key; YouTube refuses fast page reads
    private static final int MAX_CHANNEL_PAGES = 20;

    /**
     * Avatars of YouTube channels: with the YouTube Data API (50 channels per request) when an API key is set,
     * otherwise from the channels' pages, a few per run and one at a time.
     *
     * @return Avatar URL by channel id, empty for channels without one (e.g. removed); channels that couldn't be
     *         checked are missing, so they're retried later
     */
    private Map<String, String> channelAvatars(Collection<String> channelIds) {
        Map<String, String> avatars = new LinkedHashMap<>();
        List<String> ids = new ArrayList<>(channelIds);
        String apiKey = settingService.getYoutubeApiKey();

        if (apiKey != null) {
            for (int i = 0; i < ids.size(); i += YouTubeChannelAvatar.API_MAX_IDS) {
                List<String> batch = ids.subList(i, Math.min(i + YouTubeChannelAvatar.API_MAX_IDS, ids.size()));
                try {
                    Map<String, String> found = YouTubeChannelAvatar.avatarsWithApi(batch, apiKey);
                    batch.forEach(id -> avatars.put(id, found.getOrDefault(id, "")));
                } catch (Exception e) {
                    System.out.println("[Feedora] YouTube channel avatars not found: " + e.getMessage());
                }
            }
            return avatars;
        }

        for (String id : ids.subList(0, Math.min(ids.size(), MAX_CHANNEL_PAGES))) {
            try {
                String avatar = YouTubeChannelAvatar.avatarFromPage(id);
                avatars.put(id, avatar != null ? avatar : "");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Most likely YouTube refusing the pages, so the rest wait for the next run
                System.out.println("[Feedora] YouTube channel avatar not found: " + e.getMessage());
                break;
            }
        }
        return avatars;
    }

    private interface ColorSource {
        String color() throws Exception;
    }

    // Saves the color of the feeds sharing an image; on a download failure they stay unchecked, to be retried
    private void saveFaviconColor(List<Feed> feeds, String source, ColorSource colorSource) {
        try {
            String color = colorSource.color();
            feeds.forEach(feed -> feedRepository.updateFaviconColor(feed.getId(), color));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("[Feedora] Color of " + source + " not found: " + e.getMessage());
        }
    }
}
