package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.models.LogEntry;
import eu.apps4net.feedora.models.LogLevel;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.services.LogService;
import eu.apps4net.feedora.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    @Autowired
    private LogService logService;
    @Autowired
    private UserService userService;

    /**
     * Returns the current user's log entries, newest first.
     *
     * @param page The page number (1-indexed)
     * @param pageSize The number of entries per page
     * @param feedId Optional feed id, to only return the entries of that feed and its posts
     * @param postId Optional post id, to only return the entries of that post
     * @param level Optional level (INFO, WARNING, ERROR), to only return the entries of that level
     * @return List of log entries
     */
    @GetMapping
    public List<LogEntry> getLogs(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int pageSize,
                                  @RequestParam(required = false) UUID feedId, @RequestParam(required = false) UUID postId,
                                  @RequestParam(required = false) LogLevel level) {
        User currentUser = userService.getCurrentUser();
        return logService.getLogs(currentUser, page, pageSize, feedId, postId, level);
    }

    /**
     * Deletes the current user's log entries, or only those of a feed.
     *
     * @param feedId Optional feed id, to only delete the entries of that feed and its posts
     * @return The number of entries deleted
     */
    @DeleteMapping
    public Map<String, Integer> deleteLogs(@RequestParam(required = false) UUID feedId) {
        User currentUser = userService.getCurrentUser();
        return Map.of("count", logService.deleteLogs(currentUser, feedId));
    }
}
