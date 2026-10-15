package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.models.RegisterRequest;
import eu.apps4net.feedora.models.UserDTO;
import eu.apps4net.feedora.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsersWithStatistics();
    }

    @PostMapping
    public ResponseEntity<String> addUser(@RequestBody RegisterRequest request) {
        try {
            userService.createUser(request.getUsername(), request.getEmail(), request.getPassword());
            return ResponseEntity.ok(Language.getString("User added successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<String> updateUser(@PathVariable UUID userId, @RequestBody RegisterRequest request) {
        try {
            userService.updateUser(userId, request.getUsername(), request.getEmail(), request.getPassword(), userService.getCurrentUser());
            return ResponseEntity.ok(Language.getString("User updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable UUID userId) {
        try {
            userService.deleteUser(userId, userService.getCurrentUser());
            return ResponseEntity.ok(Language.getString("User deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
