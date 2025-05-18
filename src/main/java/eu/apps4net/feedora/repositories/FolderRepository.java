package eu.apps4net.feedora.repositories;

import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<Folder, UUID> {
    Optional<Folder> findByNameAndUser(String name, User user);
}
