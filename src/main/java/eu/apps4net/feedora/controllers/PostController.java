package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.services.PostService;
import eu.apps4net.feedora.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
            return "Feeds parsed and posts updated.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing feeds: " + e.getMessage();
        }
    }

    @DeleteMapping("/deleteAll")
    public String deleteAllPosts() {
        try {
            postService.deleteAllPosts();
            return "All posts deleted.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error deleting posts: " + e.getMessage();
        }
    }

    @GetMapping("/getPosts")
    public List<Post> getPosts(@RequestParam int page, @RequestParam int pageSize) {
        User adminUser = userService.getOrCreateAdminUser();
        return postService.getPostsForUser(adminUser, page, pageSize);
    }
}
