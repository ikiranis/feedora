package eu.apps4net.feedora.repositories;

import eu.apps4net.feedora.models.Role;
import eu.apps4net.feedora.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByRole(Role role);
    List<User> findByRoleIsNull();
}
