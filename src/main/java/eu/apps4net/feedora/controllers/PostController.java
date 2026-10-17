package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.services.LogService;
import eu.apps4net.feedora.services.PostService;
import eu.apps4net.feedora.services.SettingService;
import eu.apps4net.feedora.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    public static final String LISTED_AT_HEADER = "X-Listed-At";

    @Autowired
    private PostService postService;
    @Autowired
    private UserService userService;
    @Autowired
    private LogService logService;
    @Autowired
    private SettingService settingService;

    // Most posts the live newsfeed gets per check
    private static final int MAX_LIVE_POSTS = 200;
    // The live newsfeed's next check starts this much before its listing, to also get posts whose parse was committed
    // during it (the posts are added with the time they were created, not committed). The page skips posts it already has.
    private static final long LIVE_LISTING_OVERLAP_SECONDS = 30;

    @GetMapping("/getAllPosts")
    public List<Post> getAllPosts() {
        User currentUser = userService.getCurrentUser();
        return postService.getAllPostsForUser(currentUser);
    }

    // With a feedId only that feed is parsed, with a folderId only the feeds of the folder, otherwise all of them
    @PostMapping("/parseFeeds")
    public String parseFeeds(@RequestParam(required = false) UUID folderId, @RequestParam(required = false) UUID feedId) {
        try {
            User currentUser = userService.getCurrentUser();
            postService.parseFeeds(currentUser, folderId, feedId);
            return Language.getActionString("Feeds parsed and posts updated");
        } catch (Exception e) {
            e.printStackTrace();
            return Language.getActionString("Error parsing feeds").replace("{0}", e.getMessage());
        }
    }

    @DeleteMapping("/deleteAll")
    public String deleteAllPosts() {
        try {
            User currentUser = userService.getCurrentUser();
            // Only delete posts for the current user
            postService.deleteAllPostsForUser(currentUser);
            return Language.getActionString("All posts deleted");
        } catch (Exception e) {
            e.printStackTrace();
            return Language.getActionString("Error deleting posts").replace("{0}", e.getMessage());
        }
    }

    // The X-Listed-At header has the server time of the listing, for markAllAsRead to skip the posts added after it
    @GetMapping("/getPosts")
    public ResponseEntity<List<Post>> getPosts(@RequestParam int page, @RequestParam int pageSize, @RequestParam(required = false) String search,
                               @RequestParam(required = false) UUID folderId, @RequestParam(required = false) UUID feedId,
                               @RequestParam(defaultValue = "false") boolean saved,
                               @RequestParam(defaultValue = "false") boolean includeRead,
                               @RequestParam(required = false) List<UUID> hideShortsFolderIds) {
        User currentUser = userService.getCurrentUser();
        LocalDateTime listedAt = LocalDateTime.now();
        return ResponseEntity.ok()
                .header(LISTED_AT_HEADER, listedAt.toString())
                .body(postService.getPostsForUser(currentUser, page, pageSize, search, folderId, feedId, saved, includeRead, hideShortsFolderIds));
    }

    // hideShortsFolderIds: folders whose YouTube Shorts aren't counted
    @GetMapping("/unreadCounts")
    public Map<String, Object> getUnreadCounts(@RequestParam(required = false) List<UUID> hideShortsFolderIds) {
        User currentUser = userService.getCurrentUser();
        return postService.getUnreadCounts(currentUser, hideShortsFolderIds);
    }

    // Unread posts of the view (all, a folder or a feed) added after addedAfter (getPosts' X-Listed-At), e.g. by a parse since the listing
    @GetMapping("/countNew")
    public Map<String, Long> countNewPosts(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime addedAfter,
                                           @RequestParam(required = false) UUID folderId, @RequestParam(required = false) UUID feedId,
                                           @RequestParam(required = false) List<UUID> hideShortsFolderIds) {
        User currentUser = userService.getCurrentUser();
        return Map.of("count", postService.countNewPosts(currentUser, folderId, feedId, addedAfter, hideShortsFolderIds));
    }

    // Live newsfeed: the posts (read or unread) added after addedAfter (a previous X-Listed-At), most recently added first.
    // Its X-Listed-At header is the time to check from next.
    @GetMapping("/live")
    public ResponseEntity<List<Post>> getLivePosts(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime addedAfter) {
        User currentUser = userService.getCurrentUser();
        LocalDateTime listedAt = LocalDateTime.now().minusSeconds(LIVE_LISTING_OVERLAP_SECONDS);
        return ResponseEntity.ok()
                .header(LISTED_AT_HEADER, listedAt.toString())
                .body(postService.getPostsAddedAfter(currentUser, addedAfter, MAX_LIVE_POSTS));
    }

    // How often the live newsfeed checks for new posts, from the settings (which only the admin can read)
    @GetMapping("/live/refreshSeconds")
    public Map<String, Integer> getLiveRefreshSeconds() {
        return Map.of("seconds", settingService.getLiveNewsfeedRefreshSeconds());
    }

    // The content, and the number of log entries of the post (e.g. when its page couldn't be read)
    @GetMapping("/content/{postId}")
    public Map<String, Object> getPostContent(@PathVariable UUID postId) {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> result = new HashMap<>();
        result.put("content", postService.getPostContent(postId, currentUser));
        result.put("logCount", logService.countLogsByPost(currentUser, postId));
        return result;
    }

    @PutMapping("/saved/{postId}")
    public ResponseEntity<Void> setPostSaved(@PathVariable UUID postId, @RequestParam boolean saved) {
        User currentUser = userService.getCurrentUser();
        postService.setPostSaved(postId, currentUser, saved);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/markAllAsRead")
    public Map<String, Integer> markAllPostsAsRead(@RequestParam(required = false) UUID folderId, @RequestParam(required = false) UUID feedId,
                                                   @RequestParam(required = false) Integer olderThanDays,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime addedBefore) {
        User currentUser = userService.getCurrentUser();
        return Map.of("count", postService.markAllPostsAsRead(currentUser, folderId, feedId, olderThanDays, addedBefore));
    }

    @PutMapping("/markAsRead/{postId}")
    public ResponseEntity<String> markPostAsRead(@PathVariable UUID postId) {
        try {
            User currentUser = userService.getCurrentUser();
            boolean success = postService.markPostAsRead(postId, currentUser);
            
            if (success) {
                return ResponseEntity.ok("Post marked as read");
            } else {
                return ResponseEntity.badRequest().body("Failed to mark post as read");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error marking post as read: " + e.getMessage());
        }
    }

    @PutMapping("/markAsUnread/{postId}")
    public ResponseEntity<String> markPostAsUnread(@PathVariable UUID postId) {
        User currentUser = userService.getCurrentUser();
        if (postService.markPostAsUnread(postId, currentUser)) {
            return ResponseEntity.ok("Post marked as unread");
        }
        return ResponseEntity.badRequest().body("Failed to mark post as unread");
    }
}
