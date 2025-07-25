package eu.apps4net.feedora.repositories;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedRepository extends JpaRepository<Feed, UUID> {
    boolean existsByXmlUrlAndUser(String xmlUrl, User user);
    Optional<Feed> findByXmlUrlAndUser(String xmlUrl, User user);
    List<Feed> findByUser(User user);
    List<Feed> findByUser(User user, Pageable pageable);
    List<Feed> findByUserAndTitleContainingIgnoreCase(User user, String title, Pageable pageable);
    List<Feed> findByFolder(Folder folder);
    
    @Modifying
    @Transactional
    void deleteByUser(User user);
}
