package eu.apps4net.feedora.services;

import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User getOrCreateAdminUser() {
        String adminEmail = "rocean@error.gr";
        String adminUsername = "admin";
        String adminPassword = "111111";
        Optional<User> userOpt = userRepository.findByEmail(adminEmail);
        return userOpt.orElseGet(() -> userRepository.save(new User(adminUsername, adminEmail, adminPassword)));
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
