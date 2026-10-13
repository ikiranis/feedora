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
    
    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.read = false AND p.feed.folder.id = :folderId")
    List<Post> findByUserAndReadFalseAndFolder(@Param("user") User user, @Param("folderId") UUID folderId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.read = false AND p.feed.folder.id = :folderId AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CAST(p.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Post> findByUserAndReadFalseAndFolderAndTitleOrDescriptionContaining(@Param("user") User user, @Param("folderId") UUID folderId, @Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.read = false AND p.feed.id = :feedId")
    List<Post> findByUserAndReadFalseAndFeed(@Param("user") User user, @Param("feedId") UUID feedId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.read = false AND p.feed.id = :feedId AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CAST(p.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Post> findByUserAndReadFalseAndFeedAndTitleOrDescriptionContaining(@Param("user") User user, @Param("feedId") UUID feedId, @Param("searchTerm") String searchTerm, Pageable pageable);

    // Saved posts are returned whether read or not
    List<Post> findByUserAndSavedTrue(User user, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.saved = true AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CAST(p.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Post> findByUserAndSavedTrueAndTitleOrDescriptionContaining(@Param("user") User user, @Param("searchTerm") String searchTerm, Pageable pageable);

    long countByUserAndSavedTrue(User user);
    List<Post> findByUserAndSavedFalse(User user);

    // Rows of [feed id, feed title, feed website, folder id (nullable), unread count] for every feed of the user,
    // including feeds without unread posts (count 0)
    @Query("SELECT f.id, f.title, f.htmlUrl, fo.id, COUNT(p) FROM Feed f LEFT JOIN f.folder fo " +
           "LEFT JOIN f.posts p ON p.user = :user AND p.read = false " +
           "WHERE f.user = :user GROUP BY f.id, f.title, f.htmlUrl, fo.id")
    List<Object[]> countUnreadByFeed(@Param("user") User user);

    long countByUser(User user);
    boolean existsByFeedAndUserAndLink(Feed feed, User user, String link);
    
    @Modifying
    @Transactional
    void deleteByUser(User user);
    
    @Modifying
    @Transactional
    void deleteByFeed(Feed feed);
}
