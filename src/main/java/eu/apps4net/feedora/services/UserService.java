package eu.apps4net.feedora.services;

import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    public User getOrCreateAdminUser() {
        String adminEmail = "rocean@error.gr";
        String adminUsername = "admin";
        String adminPassword = "111111";
        Optional<User> userOpt = userRepository.findByEmail(adminEmail);
        if (userOpt.isPresent()) {
            return userOpt.get();
        } else {
            // Encode password when creating admin user
            String encodedPassword = passwordEncoder.encode(adminPassword);
            return userRepository.save(new User(adminUsername, adminEmail, encodedPassword));
        }
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        
        // Fallback to admin user if no authenticated user (for backward compatibility)
        return getOrCreateAdminUser();
    }
    
    public User registerUser(String username, String email, String password) {
        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User with email " + email + " already exists");
        }
        
        // Encode password
        String encodedPassword = passwordEncoder.encode(password);
        
        // Create and save user
        User user = new User(username, email, encodedPassword);
        return userRepository.save(user);
    }
    
    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
}
