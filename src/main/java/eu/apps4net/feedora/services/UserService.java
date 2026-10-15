package eu.apps4net.feedora.services;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.models.UserDTO;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.FolderRepository;
import eu.apps4net.feedora.repositories.LogEntryRepository;
import eu.apps4net.feedora.repositories.PostRepository;
import eu.apps4net.feedora.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private LogEntryRepository logEntryRepository;

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

    /**
     * Returns all users together with the number of folders, feeds, posts and log entries each one has.
     */
    public List<UserDTO> getAllUsersWithStatistics() {
        Map<UUID, Long> folderCounts = toCountMap(folderRepository.countGroupedByUser());
        Map<UUID, Long> feedCounts = toCountMap(feedRepository.countGroupedByUser());
        Map<UUID, Long> logCounts = toCountMap(logEntryRepository.countGroupedByUser());
        Map<UUID, Object[]> postCounts = new HashMap<>();
        for (Object[] row : postRepository.countGroupedByUser()) {
            postCounts.put((UUID) row[0], row);
        }

        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(user -> {
                    UserDTO dto = UserDTO.fromUser(user);
                    dto.folderCount = folderCounts.getOrDefault(user.getId(), 0L);
                    dto.feedCount = feedCounts.getOrDefault(user.getId(), 0L);
                    dto.logCount = logCounts.getOrDefault(user.getId(), 0L);
                    Object[] posts = postCounts.get(user.getId());
                    if (posts != null) {
                        dto.postCount = toLong(posts[1]);
                        dto.unreadCount = toLong(posts[2]);
                        dto.savedCount = toLong(posts[3]);
                    }
                    return dto;
                })
                .toList();
    }

    /**
     * Creates a new user.
     */
    public User createUser(String username, String email, String password) {
        validateUsernameAndEmail(username, email);
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException(Language.getString("Password must be at least 6 characters long"));
        }
        if (userRepository.findByEmail(email.trim().toLowerCase()).isPresent()) {
            throw new IllegalArgumentException(Language.getString("A user with this email already exists"));
        }

        return userRepository.save(new User(username.trim(), email.trim().toLowerCase(), passwordEncoder.encode(password)));
    }

    /**
     * Updates the username, email and, when given, the password of a user.
     * The current user can't change their own email, because the login token is bound to it.
     */
    public User updateUser(UUID id, String username, String email, String password, User currentUser) {
        validateUsernameAndEmail(username, email);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(Language.getString("User not found")));

        String newEmail = email.trim().toLowerCase();
        if (!newEmail.equals(user.getEmail())) {
            if (user.getId().equals(currentUser.getId())) {
                throw new IllegalArgumentException(Language.getString("You can't change your own email"));
            }
            Optional<User> existing = userRepository.findByEmail(newEmail);
            if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
                throw new IllegalArgumentException(Language.getString("A user with this email already exists"));
            }
        }

        // An empty password keeps the current one
        if (password != null && !password.isEmpty()) {
            if (password.length() < 6) {
                throw new IllegalArgumentException(Language.getString("Password must be at least 6 characters long"));
            }
            user.setPassword(passwordEncoder.encode(password));
        }

        user.setUsername(username.trim());
        user.setEmail(newEmail);
        return userRepository.save(user);
    }

    /**
     * Deletes a user together with their log entries, posts, feeds and folders.
     * The current user can't delete themselves.
     */
    @Transactional
    public void deleteUser(UUID id, User currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(Language.getString("User not found")));
        if (user.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException(Language.getString("You can't delete your own user"));
        }

        // Delete in foreign key order; the feed statistics are removed together with their feeds
        logEntryRepository.deleteByUser(user);
        postRepository.deleteByUser(user);
        feedRepository.deleteByUser(user);
        folderRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    private void validateUsernameAndEmail(String username, String email) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException(Language.getString("Username is required"));
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException(Language.getString("Email is required"));
        }
    }

    private static Map<UUID, Long> toCountMap(List<Object[]> rows) {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((UUID) row[0], toLong(row[1]));
        }
        return counts;
    }

    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
