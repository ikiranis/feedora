package eu.apps4net.feedora.repositories;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedRepository extends JpaRepository<Feed, UUID> {
    boolean existsByXmlUrlAndUser(String xmlUrl, User user);
    List<Feed> findByUser(User user);
}
