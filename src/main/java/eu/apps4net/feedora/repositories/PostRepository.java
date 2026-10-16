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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    List<Post> findByFeedAndUser(Feed feed, User user);
    Optional<Post> findByFeedAndUserAndLink(Feed feed, User user, String link);
    List<Post> findByUser(User user);
    List<Post> findByUser(User user, Pageable pageable);
    List<Post> findByUserAndReadFalse(User user);
    // Posts of the user, only the unread ones unless includeRead is true
    @Query("SELECT p FROM Post p WHERE p.user = :user AND (p.read = false OR :includeRead = true)")
    List<Post> findByUser(@Param("user") User user, @Param("includeRead") boolean includeRead, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND (p.read = false OR :includeRead = true) AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CAST(p.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Post> findByUserAndTitleOrDescriptionContaining(@Param("user") User user, @Param("includeRead") boolean includeRead, @Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND (p.read = false OR :includeRead = true) AND p.feed.folder.id = :folderId")
    List<Post> findByUserAndFolder(@Param("user") User user, @Param("includeRead") boolean includeRead, @Param("folderId") UUID folderId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND (p.read = false OR :includeRead = true) AND p.feed.folder.id = :folderId AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CAST(p.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Post> findByUserAndFolderAndTitleOrDescriptionContaining(@Param("user") User user, @Param("includeRead") boolean includeRead, @Param("folderId") UUID folderId, @Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND (p.read = false OR :includeRead = true) AND p.feed.id = :feedId")
    List<Post> findByUserAndFeed(@Param("user") User user, @Param("includeRead") boolean includeRead, @Param("feedId") UUID feedId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND (p.read = false OR :includeRead = true) AND p.feed.id = :feedId AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CAST(p.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Post> findByUserAndFeedAndTitleOrDescriptionContaining(@Param("user") User user, @Param("includeRead") boolean includeRead, @Param("feedId") UUID feedId, @Param("searchTerm") String searchTerm, Pageable pageable);

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

    // Rows of [user id, number of posts, number of unread posts, number of saved posts] for every user that has posts
    @Query("SELECT p.user.id, COUNT(p), SUM(CASE WHEN p.read = false THEN 1 ELSE 0 END), SUM(CASE WHEN p.saved = true THEN 1 ELSE 0 END) " +
           "FROM Post p GROUP BY p.user.id")
    List<Object[]> countGroupedByUser();

    long countByUser(User user);

    // Links of the posts of a feed, to skip the entries already added in one query instead of one per entry
    @Query("SELECT p.link FROM Post p WHERE p.feed = :feed AND p.user = :user AND p.link IS NOT NULL")
    List<String> findLinksByFeedAndUser(@Param("feed") Feed feed, @Param("user") User user);
    
    @Modifying
    @Transactional
    void deleteByUser(User user);
    
    @Modifying
    @Transactional
    void deleteByFeed(Feed feed);

    // Read posts from before the read date was kept get the given date, so they are deleted a full retention period later
    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.readAt = :readAt WHERE p.read = true AND p.readAt IS NULL")
    int setMissingReadAt(@Param("readAt") LocalDateTime readAt);

    // Saved posts are kept, even if read
    @Modifying
    @Transactional
    @Query("DELETE FROM Post p WHERE p.read = true AND p.saved = false AND p.readAt < :before")
    int deleteReadBefore(@Param("before") LocalDateTime before);
}
