package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.exceptions.PageBlockedException;
import eu.apps4net.feedora.models.ParseDTO;
import eu.apps4net.feedora.services.ParseService;
import eu.apps4net.feedora.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parser")
public class ParserController {
    @Autowired
    private ParseService parseService;
    @Autowired
    private UserService userService;

    /**
     * Returns the pages the current user parses, the newest first.
     */
    @GetMapping("/pages")
    public List<ParseDTO> getPages() {
        return parseService.getParses(userService.getCurrentUser());
    }

    /**
     * Downloads a page and finds its repeated elements, to choose the one that holds the posts.
     *
     * @param request The page's URL
     * @return The page's URL, title and candidate elements, or the error with blocked set when the page blocks the access
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(@RequestBody ParseRequest request) {
        try {
            return ResponseEntity.ok(parseService.analyze(request.url));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("blocked", e instanceof PageBlockedException);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Adds a page to parse, and parses it right away.
     *
     * @param request The page's URL, the selector of its posts, the feed's title and the frequency in minutes
     * @return The added page, or the error message
     */
    @PostMapping("/pages")
    public ResponseEntity<?> addPage(@RequestBody ParseRequest request) {
        try {
            return ResponseEntity.ok(parseService.addParse(request.url, request.title, request.selector,
                    request.frequency, userService.getCurrentUser()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Changes the title, selector and frequency of a page, and parses it again.
     */
    @PutMapping("/pages/{parseId}")
    public ResponseEntity<?> updatePage(@PathVariable String parseId, @RequestBody ParseRequest request) {
        try {
            return ResponseEntity.ok(parseService.updateParse(parseId, request.title, request.selector,
                    request.frequency, userService.getCurrentUser()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Stops parsing a page and deletes its feed file.
     */
    @DeleteMapping("/pages/{parseId}")
    public ResponseEntity<String> deletePage(@PathVariable String parseId) {
        try {
            return ResponseEntity.ok(parseService.deleteParse(parseId, userService.getCurrentUser()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Parses a page now, regardless of its frequency.
     *
     * @return The page with the result of the parse (lastError is set when it failed)
     */
    @PostMapping("/pages/{parseId}/parse")
    public ResponseEntity<?> parsePage(@PathVariable String parseId) {
        try {
            return ResponseEntity.ok(parseService.parseNow(parseId, userService.getCurrentUser()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Adds the generated feed of a page to the current user's feeds.
     *
     * @param request The folder of the feed, the default folder when it's missing
     */
    @PostMapping("/pages/{parseId}/addFeed")
    public ResponseEntity<String> addFeed(@PathVariable String parseId, @RequestBody(required = false) AddFeedRequest request) {
        try {
            String folderId = request != null ? request.folderId : null;
            return ResponseEntity.ok(parseService.addToFeeds(parseId, folderId, userService.getCurrentUser()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Returns a generated RSS feed. Public, so the feed parser and other readers can download it without a token.
     *
     * @param fileName The feed's file name, e.g. example.com.xml
     */
    @GetMapping("/feed/{fileName}")
    public ResponseEntity<String> getFeed(@PathVariable String fileName) throws Exception {
        Path file = parseService.getFeedFile(fileName);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(new MediaType("application", "rss+xml", StandardCharsets.UTF_8))
                .body(Files.readString(file, StandardCharsets.UTF_8));
    }

    /**
     * Request object for adding the feed of a page to the feeds
     */
    public static class AddFeedRequest {
        // Null or empty for the default folder
        public String folderId;
    }

    /**
     * Request object for analyzing, adding and updating a page
     */
    public static class ParseRequest {
        public String url;
        public String title;
        public String selector;
        // Minutes between two parses, the default when null
        public Integer frequency;
    }
}
