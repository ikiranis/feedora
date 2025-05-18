package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.services.FeedService;
import eu.apps4net.feedora.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

import eu.apps4net.feedora.models.FeedDTO;

@RestController
@RequestMapping("/api")
public class FeedController {
    @Autowired
    private FeedService feedService;
    @Autowired
    private UserService userService;

    /**
     * Imports feeds and folders from the OPML file (feeds.xml in the root folder).
     * Creates folders and feeds in the database, relating them to the admin user.
     *
     * @return A message indicating the number of feeds imported or an error message.
     */
    @PostMapping("/importOPML")
    public String importOPML() {
        try {
            User adminUser = userService.getOrCreateAdminUser();
            int feedsAdded = feedService.importOPML(new File("feeds.xml"), adminUser);
            return "Imported " + feedsAdded + " feeds.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error importing OPML: " + e.getMessage();
        }
    }

    /**
     * Returns all feeds in the database as JSON, excluding user and folder objects from the response.
     *
     * @return List of all Feed entities (without user and folder details)
     */
    @GetMapping("/getAllFeeds")
    public List<FeedDTO> getAll() {
        return feedService.getAllFeeds().stream().map(FeedDTO::fromFeed).toList();
    }
}
