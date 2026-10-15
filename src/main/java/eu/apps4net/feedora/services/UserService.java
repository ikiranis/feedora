package eu.apps4net.feedora.services;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.exceptions.RegistrationDisabledException;
import eu.apps4net.feedora.models.Role;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.models.UserDTO;
import eu.apps4net.feedora.repositories.FeedRepository;
import eu.apps4net.feedora.repositories.FolderRepository;
import eu.apps4net.feedora.repositories.LogEntryRepository;
import eu.apps4net.feedora.repositories.PostRepository;
import eu.apps4net.feedora.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
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

    // The admin user that older versions created automatically, promoted when migrating to roles
    private static final String LEGACY_ADMIN_EMAIL = "rocean@error.gr";

    /**
     * Gives a role to the users created before roles existed.
     * If there's no admin yet, the legacy admin, or else the first user by username, becomes the admin.
     */
    @Transactional
    public void migrateUserRoles() {
        List<User> withoutRole = userRepository.findByRoleIsNull();
        if (!userRepository.existsByRole(Role.ADMIN) && !withoutRole.isEmpty()) {
            User admin = withoutRole.stream()
                    .filter(user -> LEGACY_ADMIN_EMAIL.equals(user.getEmail()))
                    .findFirst()
                    .orElseGet(() -> withoutRole.stream()
                            .min(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                            .orElseThrow());
            admin.setRole(Role.ADMIN);
        }
        for (User user : withoutRole) {
            if (user.getRole() != Role.ADMIN) {
                user.setRole(Role.USER);
            }
        }
        userRepository.saveAll(withoutRole);
    }

    public boolean hasUsers() {
        return userRepository.count() > 0;
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
        
        throw new AuthenticationCredentialsNotFoundException(Language.getString("Please log in"));
    }

    /**
     * Registers a new user. The first user becomes the admin, the next ones simple users.
     * Synchronized, so two simultaneous first registrations can't both become admins.
     *
     * @param registrationEnabled whether new users can register; ignored for the first user
     * @throws RegistrationDisabledException if there are users already and registration is disabled
     */
    public synchronized User registerUser(String username, String email, String password, boolean registrationEnabled) {
        boolean firstUser = !hasUsers();
        if (!firstUser && !registrationEnabled) {
            throw new RegistrationDisabledException(Language.getString("Registration of new users is disabled"));
        }

        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User with email " + email + " already exists");
        }

        User user = new User(username, email, passwordEncoder.encode(password), firstUser ? Role.ADMIN : Role.USER);
        return userRepository.save(user);
    }
    
    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    /**
     * Returns the users the current user can see, together with the number of folders, feeds, posts
     * and log entries each one has. The admin sees every user, a simple user only themselves.
     */
    public List<UserDTO> getVisibleUsersWithStatistics(User currentUser) {
        Map<UUID, Long> folderCounts = toCountMap(folderRepository.countGroupedByUser());
        Map<UUID, Long> feedCounts = toCountMap(feedRepository.countGroupedByUser());
        Map<UUID, Long> logCounts = toCountMap(logEntryRepository.countGroupedByUser());
        Map<UUID, Object[]> postCounts = new HashMap<>();
        for (Object[] row : postRepository.countGroupedByUser()) {
            postCounts.put((UUID) row[0], row);
        }

        List<User> users = currentUser.isAdmin() ? userRepository.findAll() : List.of(currentUser);
        return users.stream()
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
     * A simple user can only update themselves.
     * The current user can't change their own email, because the login token is bound to it.
     */
    public User updateUser(UUID id, String username, String email, String password, User currentUser) {
        validateUsernameAndEmail(username, email);
        if (!currentUser.isAdmin() && !currentUser.getId().equals(id)) {
            throw new IllegalArgumentException(Language.getString("User not found"));
        }
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
     * Changes the role of a user. Only the admin can do it, and not for their own user,
     * so there's always at least one admin.
     */
    public User changeRole(UUID id, Role role, User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new IllegalArgumentException(Language.getString("Only the admin can change user roles"));
        }
        if (role == null) {
            throw new IllegalArgumentException(Language.getString("Role is required"));
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(Language.getString("User not found")));
        if (user.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException(Language.getString("You can't change your own role"));
        }

        user.setRole(role);
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
