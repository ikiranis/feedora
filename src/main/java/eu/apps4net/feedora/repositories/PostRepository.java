package eu.apps4net.feedora.repositories;

import eu.apps4net.feedora.models.Post;
import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    List<Post> findByFeedAndUser(Feed feed, User user);
    Optional<Post> findByFeedAndUserAndLink(Feed feed, User user, String link);
    List<Post> findByUser(User user);
    List<Post> findByUser(User user, Pageable pageable);
    List<Post> findByUserAndReadFalse(User user);
    List<Post> findByUserAndReadFalse(User user, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.read = false AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CAST(p.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Post> findByUserAndReadFalseAndTitleOrDescriptionContaining(@Param("user") User user, @Param("searchTerm") String searchTerm, Pageable pageable);
    
    long countByUser(User user);
    boolean existsByFeedAndUserAndLink(Feed feed, User user, String link);
    
    @Modifying
    @Transactional
    void deleteByUser(User user);
    
    @Modifying
    @Transactional
    void deleteByFeed(Feed feed);
}
