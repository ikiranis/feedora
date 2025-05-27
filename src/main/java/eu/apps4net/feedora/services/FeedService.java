package eu.apps4net.feedora.services;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.FolderRepository;
import eu.apps4net.feedora.repositories.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    }

    /**
     * Returns all feeds in the database.
     *
     * @return List of all Feed entities
     */
    public List<Feed> getAllFeeds() {
        return feedRepository.findAll();
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
}
