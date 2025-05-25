package eu.apps4net.feedora.repositories;

import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    List<Post> findByFeedAndUser(Feed feed, User user);
    Optional<Post> findByFeedAndUserAndLink(Feed feed, User user, String link);
    List<Post> findByUser(User user);
    List<Post> findByUser(User user, Pageable pageable);
}
