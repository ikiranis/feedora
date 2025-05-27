package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.services.FeedService;
import eu.apps4net.feedora.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
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
     * Imports feeds and folders from an uploaded OPML file.
     * Replaces all existing feeds and folders for the admin user with the new ones from the OPML file.
     *
     * @param file The OPML file uploaded by the user
     * @return A message indicating the number of feeds imported or an error message.
     */
    @PostMapping("/importOPML")
    public String importOPML(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return "Error: No file uploaded";
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".opml") && 
                !file.getOriginalFilename().toLowerCase().endsWith(".xml")) {
                return "Error: File must be an OPML (.opml) or XML (.xml) file";
            }
            
            User adminUser = userService.getOrCreateAdminUser();
            int feedsAdded = feedService.importOPML(file.getInputStream(), adminUser);
            return "Successfully replaced existing feeds. Imported " + feedsAdded + " new feeds.";
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
