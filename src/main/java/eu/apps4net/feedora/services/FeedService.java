package eu.apps4net.feedora.services;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.FolderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

@Service
public class FeedService {
    @Autowired
    private FeedRepository feedRepository;
    @Autowired
    private FolderRepository folderRepository;

    /**
     * Imports feeds and folders from an OPML file and saves them to the database.
     * If a folder or feed already exists for the user, it will not be duplicated.
     *
     * @param opmlFile The OPML file to import (e.g., feeds.xml)
     * @param user The user to relate the feeds and folders to
     * @return The number of feeds added
     * @throws Exception if there is an error reading or parsing the OPML file
     */
    public int importOPML(File opmlFile, User user) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(opmlFile);
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
                            if (!feedRepository.existsByXmlUrlAndUser(xmlUrl, user)) {
                                Feed feed = new Feed(title, xmlUrl, htmlUrl, type, folder, user);
                                feedRepository.save(feed);
                                feedsAdded++;
                            }
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
}
