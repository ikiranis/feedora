package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.models.*;
import eu.apps4net.feedora.services.UserService;
import eu.apps4net.feedora.utilities.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // Validate input
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Username is required");
            }
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email is required");
            }
            if (request.getPassword() == null || request.getPassword().length() < 6) {
                return ResponseEntity.badRequest().body("Password must be at least 6 characters long");
            }
            
            // Register user
            User user = userService.registerUser(
                request.getUsername().trim(), 
                request.getEmail().trim().toLowerCase(), 
                request.getPassword()
            );
            
            // Generate token
            String token = jwtUtil.generateToken(user.getEmail(), user.getId());
            
            // Return response
            AuthResponse response = new AuthResponse(token, user.getEmail(), user.getUsername(), user.getId());
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Registration failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email is required");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return ResponseEntity.badRequest().body("Password is required");
            }
            
            // Find user
            Optional<User> userOpt = userService.findByEmail(request.getEmail().trim().toLowerCase());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid email or password");
            }
            
            User user = userOpt.get();
            
            // Check password
            if (!userService.checkPassword(user, request.getPassword())) {
                return ResponseEntity.badRequest().body("Invalid email or password");
            }
            
            // Generate token
            String token = jwtUtil.generateToken(user.getEmail(), user.getId());
            
            // Return response
            AuthResponse response = new AuthResponse(token, user.getEmail(), user.getUsername(), user.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Login failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated() && 
                authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                
                // Return user info (without password)
                AuthResponse response = new AuthResponse(null, user.getEmail(), user.getUsername(), user.getId());
                return ResponseEntity.ok(response);
            }
            
            return ResponseEntity.badRequest().body("User not authenticated");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to get user info: " + e.getMessage());
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Since we're using stateless JWT, logout is handled on the client side
        // by removing the token from localStorage
        return ResponseEntity.ok("Logged out successfully");
    }
}
