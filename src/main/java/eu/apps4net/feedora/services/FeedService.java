package eu.apps4net.feedora.services;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.FolderRepository;
import eu.apps4net.feedora.repositories.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

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
     * Returns all feeds in the database.
     *
     * @return List of all Feed entities
     */
    public List<Feed> getAllFeeds() {
        return feedRepository.findAll();
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

    public void removeFeedByXmlUrlAndUser(String xmlUrl, User user) {
        Feed feed = feedRepository.findByUser(user).stream()
            .filter(f -> f.getXmlUrl().equals(xmlUrl))
            .findFirst()
            .orElse(null);
        if (feed != null) {
            feedRepository.delete(feed);
            System.out.println("[Feedora] Removed feed due to invalid (HTML) response: " + xmlUrl);
        }
    }

    public void removeFeedById(UUID id) {
        feedRepository.deleteById(id);
        System.out.println("[Feedora] Removed feed due to invalid (HTML) response: " + id);
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
        // Check if feed already exists for this user
        if (feedRepository.findByXmlUrlAndUser(url, user).isPresent()) {
            throw new Exception(Language.getActionString("Feed already exists"));
        }

        Folder folder = null;
        if (folderId != null && !folderId.isEmpty()) {
            folder = folderRepository.findById(UUID.fromString(folderId)).orElse(null);
            if (folder == null || !folder.getUser().equals(user)) {
                throw new Exception(Language.getActionString("Folder not found or doesn't belong to user"));
            }
        }

        // If no title provided, fetch it from the feed
        if (title == null || title.trim().isEmpty()) {
            try {
                Map<String, Object> feedInfo = fetchFeedInfo(url);
                title = (String) feedInfo.get("title");
            } catch (Exception e) {
                // If we can't fetch title, use URL as fallback
                title = url;
            }
        }

        Feed feed = new Feed(title, url, "", "rss", folder, user);
        feedRepository.save(feed);
        
        return Language.getActionString("Feed added successfully");
    }

    /**
     * Fetches feed information from a URL.
     *
     * @param url The RSS feed URL
     * @return Map containing feed information
     * @throws Exception if there is an error fetching the feed
     */
    public Map<String, Object> fetchFeedInfo(String url) throws Exception {
        Map<String, Object> feedInfo = new HashMap<>();
        
        try {
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
            
        } catch (Exception e) {
            throw new Exception(Language.getActionString("Failed to fetch feed info").replace("{0}", e.getMessage()));
        }
        
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
        if (!feed.getUser().equals(user)) {
            throw new Exception(Language.getActionString("Feed doesn't belong to user"));
        }

        // Delete posts first (due to foreign key constraints)
        postRepository.deleteByFeed(feed);
        
        // Then delete the feed
        feedRepository.delete(feed);
        return Language.getActionString("Feed deleted successfully");
    }
}
