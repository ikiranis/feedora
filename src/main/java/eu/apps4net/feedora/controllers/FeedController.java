package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.services.FeedService;
import eu.apps4net.feedora.services.UserService;
import eu.apps4net.feedora.services.OperationLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import eu.apps4net.feedora.models.FeedDTO;

@RestController
@RequestMapping("/api")
public class FeedController {
    @Autowired
    private FeedService feedService;
    @Autowired
    private UserService userService;
    @Autowired
    private OperationLockService operationLockService;

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
                return Language.getActionString("No file uploaded");
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".opml") && 
                !file.getOriginalFilename().toLowerCase().endsWith(".xml")) {
                return Language.getActionString("File must be OPML or XML");
            }
            
            User adminUser = userService.getOrCreateAdminUser();
            int feedsAdded = feedService.importOPML(file.getInputStream(), adminUser);
            return Language.getActionString("Successfully replaced existing feeds").replace("{0}", String.valueOf(feedsAdded));
        } catch (Exception e) {
            e.printStackTrace();
            return Language.getActionString("Error importing OPML").replace("{0}", e.getMessage());
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

    /**
     * Returns paginated feeds for the admin user.
     *
     * @param page The page number (1-indexed)
     * @param pageSize The number of feeds per page
     * @param search Optional search term to filter feeds by title (case-insensitive)
     * @return List of Feed entities (without user and folder details)
     */
    @GetMapping("/getFeeds")
    public List<FeedDTO> getFeeds(@RequestParam int page, @RequestParam int pageSize, @RequestParam(required = false) String search) {
        User adminUser = userService.getOrCreateAdminUser();
        return feedService.getFeedsForUser(adminUser, page, pageSize, search).stream().map(FeedDTO::fromFeed).toList();
    }

    /**
     * Checks if a feed operation (parsing or OPML import) is currently running.
     *
     * @return JSON object with status information
     */
    @GetMapping("/feedOperationStatus")
    public Map<String, Object> getFeedOperationStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isRunning", operationLockService.isFeedOperationRunning());
        return status;
    }

    /**
     * Adds a single feed to the database.
     *
     * @param feedRequest The feed data containing URL, folderId, and optional title
     * @return Success message or error message
     */
    @PostMapping("/addFeed")
    public ResponseEntity<String> addFeed(@RequestBody AddFeedRequest feedRequest) {
        try {
            User adminUser = userService.getOrCreateAdminUser();
            String result = feedService.addSingleFeed(feedRequest.url, feedRequest.folderId, feedRequest.title, adminUser);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            String errorMessage = Language.getActionString("Error adding feed").replace("{0}", e.getMessage());
            return ResponseEntity.badRequest().body(errorMessage);
        }
    }

    /**
     * Deletes a single feed from the database.
     *
     * @param feedId The ID of the feed to delete
     * @return Success message or error message
     */
    @PostMapping("/deleteFeed")
    public ResponseEntity<String> deleteFeed(@RequestParam String feedId) {
        try {
            User adminUser = userService.getOrCreateAdminUser();
            String result = feedService.deleteFeed(feedId, adminUser);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            String errorMessage = Language.getActionString("Error deleting feed").replace("{0}", e.getMessage());
            return ResponseEntity.badRequest().body(errorMessage);
        }
    }

    /**
     * Fetches feed information from a URL to get title and metadata.
     *
     * @param url The RSS feed URL
     * @return Feed information including title
     */
    @GetMapping("/fetchFeedInfo")
    public ResponseEntity<Map<String, Object>> fetchFeedInfo(@RequestParam String url) {
        try {
            Map<String, Object> feedInfo = feedService.fetchFeedInfo(url);
            return ResponseEntity.ok(feedInfo);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Updates a single feed in the database.
     *
     * @param updateRequest The feed update data containing feedId, title, and folderName
     * @return Success message or error message
     */
    @PostMapping("/updateFeed")
    public ResponseEntity<String> updateFeed(@RequestBody UpdateFeedRequest updateRequest) {
        try {
            User adminUser = userService.getOrCreateAdminUser();
            String result = feedService.updateFeed(updateRequest.feedId, updateRequest.title, updateRequest.folderName, adminUser);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            String errorMessage = Language.getActionString("Error updating feed").replace("{0}", e.getMessage());
            return ResponseEntity.badRequest().body(errorMessage);
        }
    }

    /**
     * Request object for adding a feed
     */
    public static class AddFeedRequest {
        public String url;
        public String folderId;
        public String title;
    }

    /**
     * Request object for updating a feed
     */
    public static class UpdateFeedRequest {
        public String feedId;
        public String title;
        public String folderName;
    }
}
