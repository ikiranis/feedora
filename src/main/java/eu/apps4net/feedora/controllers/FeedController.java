package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.services.FeedService;
import eu.apps4net.feedora.services.LogService;
import eu.apps4net.feedora.services.PostService;
import eu.apps4net.feedora.services.UserService;
import eu.apps4net.feedora.services.OperationLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.apps4net.feedora.models.FeedDTO;

@RestController
@RequestMapping("/api/feed")
public class FeedController {
    @Autowired
    private FeedService feedService;
    @Autowired
    private UserService userService;
    @Autowired
    private OperationLockService operationLockService;
    @Autowired
    private LogService logService;
    @Autowired
    private PostService postService;

    // Parsing after an OPML import follows the feed parser switch, so it can be disabled (e.g. in tests)
    @Value("${feedora.feed-parser.enabled:true}")
    private boolean feedParserEnabled;

    // Parses the feeds of a newly imported OPML file in the background, so the upload doesn't wait for it
    private final ExecutorService importParseExecutor = Executors.newSingleThreadExecutor();

    /**
     * Imports feeds and folders from an uploaded OPML file.
     * Replaces all existing feeds and folders for the current user with the new ones from the OPML file,
     * then starts parsing the new feeds in the background.
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
            
            User currentUser = userService.getCurrentUser();
            int feedsAdded = feedService.importOPML(file.getInputStream(), currentUser);
            parseImportedFeeds(currentUser);
            return Language.getActionString("Successfully replaced existing feeds").replace("{0}", String.valueOf(feedsAdded));
        } catch (Exception e) {
            e.printStackTrace();
            return Language.getActionString("Error importing OPML").replace("{0}", e.getMessage());
        }
    }

    /**
     * Exports the feeds and folders of the current user as an OPML file download.
     *
     * @return The OPML document as an attachment
     * @throws Exception if the document can't be built
     */
    @GetMapping("/exportOPML")
    public ResponseEntity<String> exportOPML() throws Exception {
        String opml = feedService.exportOPML(userService.getCurrentUser());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"feedora.opml\"")
                .contentType(new MediaType("text", "x-opml", StandardCharsets.UTF_8))
                .body(opml);
    }

    private void parseImportedFeeds(User user) {
        if (!feedParserEnabled) {
            return;
        }

        importParseExecutor.submit(() -> {
            try {
                postService.parseFeeds(user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Returns all feeds of the current user as JSON, excluding user and folder objects from the response.
     *
     * @return List of the user's Feed entities (without user and folder details)
     */
    @GetMapping("/getAllFeeds")
    public List<FeedDTO> getAll() {
        return feedService.getAllFeeds(userService.getCurrentUser()).stream().map(FeedDTO::fromFeed).toList();
    }

    /**
     * Returns paginated feeds for the current user.
     *
     * @param page The page number (1-indexed)
     * @param pageSize The number of feeds per page
     * @param search Optional search term to filter feeds by title (case-insensitive)
     * @return List of Feed entities (without user and folder details), with their number of log entries
     */
    @GetMapping("/getFeeds")
    public List<FeedDTO> getFeeds(@RequestParam int page, @RequestParam int pageSize, @RequestParam(required = false) String search) {
        User currentUser = userService.getCurrentUser();
        List<Feed> feeds = feedService.getFeedsForUser(currentUser, page, pageSize, search);
        Map<UUID, Long> logCounts = logService.countLogsByFeed(currentUser, feeds.stream().map(Feed::getId).toList());

        return feeds.stream().map(feed -> {
            FeedDTO dto = FeedDTO.fromFeed(feed);
            dto.logCount = logCounts.getOrDefault(feed.getId(), 0L);
            return dto;
        }).toList();
    }

    /**
     * Checks if a feed operation (parsing or OPML import) is currently running, and how far it has gone.
     *
     * @return JSON object with status information. total is 0 when the progress isn't known (e.g. OPML import).
     *         runCount is increased by every scan, so a scan that ended between two checks is noticed.
     */
    @GetMapping("/feedOperationStatus")
    public Map<String, Object> getFeedOperationStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isRunning", operationLockService.isFeedOperationRunning());
        status.put("total", operationLockService.getProgressTotal());
        status.put("completed", operationLockService.getProgressCompleted());
        status.put("runCount", operationLockService.getRunCount());
        return status;
    }

    /**
     * Returns when the feeds of the current user were last parsed.
     *
     * @return JSON object with the last parse time, null if the feeds were never parsed
     */
    @GetMapping("/lastParsedAt")
    public Map<String, Object> getLastParsedAt() {
        Map<String, Object> result = new HashMap<>();
        result.put("lastParsedAt", feedService.getLastParsedAt(userService.getCurrentUser()));
        return result;
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
            User currentUser = userService.getCurrentUser();
            String result = feedService.addSingleFeed(feedRequest.url, feedRequest.folderId, feedRequest.title, currentUser);
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
            User currentUser = userService.getCurrentUser();
            String result = feedService.deleteFeed(feedId, currentUser);
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
            User currentUser = userService.getCurrentUser();
            String result = feedService.updateFeed(updateRequest.feedId, updateRequest.title, updateRequest.folderName, currentUser);
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
