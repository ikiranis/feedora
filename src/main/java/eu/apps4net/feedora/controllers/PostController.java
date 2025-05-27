package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.services.PostService;
import eu.apps4net.feedora.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    @Autowired
    private PostService postService;
    @Autowired
    private UserService userService;

    @GetMapping("/getAllPosts")
    public List<Post> getAllPosts() {
        User adminUser = userService.getOrCreateAdminUser();
        return postService.getAllPostsForUser(adminUser);
    }

    @PostMapping("/parseFeeds")
    public String parseFeeds() {
        try {
            User adminUser = userService.getOrCreateAdminUser();
            postService.parseFeeds(adminUser);
            return Language.getActionString("Feeds parsed and posts updated");
        } catch (Exception e) {
            e.printStackTrace();
            return Language.getActionString("Error parsing feeds").replace("{0}", e.getMessage());
        }
    }

    @DeleteMapping("/deleteAll")
    public String deleteAllPosts() {
        try {
            postService.deleteAllPosts();
            return Language.getActionString("All posts deleted");
        } catch (Exception e) {
            e.printStackTrace();
            return Language.getActionString("Error deleting posts").replace("{0}", e.getMessage());
        }
    }

    @GetMapping("/getPosts")
    public List<Post> getPosts(@RequestParam int page, @RequestParam int pageSize) {
        User adminUser = userService.getOrCreateAdminUser();
        return postService.getPostsForUser(adminUser, page, pageSize);
    }

    @PutMapping("/markAsRead/{postId}")
    public ResponseEntity<String> markPostAsRead(@PathVariable UUID postId) {
        try {
            User adminUser = userService.getOrCreateAdminUser();
            boolean success = postService.markPostAsRead(postId, adminUser);
            
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
}
