package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.services.FolderService;
import eu.apps4net.feedora.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FolderController {
    @Autowired
    private FolderService folderService;
    
    @Autowired
    private UserService userService;

    @GetMapping("/folders")
    public List<Folder> getAllFolders() {
        User user = userService.getCurrentUser();
        return folderService.getAllFoldersByUser(user);
    }
    
    @PostMapping("/addFolder")
    public ResponseEntity<String> addFolder(@RequestBody String name) {
        try {
            User user = userService.getCurrentUser();
            String result = folderService.addFolder(name, user);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error adding folder: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/deleteFolder/{folderId}")
    public ResponseEntity<String> deleteFolder(@PathVariable String folderId) {
        try {
            User user = userService.getCurrentUser();
            String result = folderService.deleteFolder(folderId, user);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting folder: " + e.getMessage());
        }
    }
    
    @PutMapping("/updateFolder/{folderId}")
    public ResponseEntity<String> updateFolder(@PathVariable String folderId, @RequestBody String name) {
        try {
            User user = userService.getCurrentUser();
            String result = folderService.updateFolder(folderId, name, user);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating folder: " + e.getMessage());
        }
    }
}
