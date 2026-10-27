package eu.apps4net.feedora.repositories;

import eu.apps4net.feedora.models.LiveStatus;
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
    // A post of the user with the given link, from any feed or saved from the browser
    Optional<Post> findFirstByUserAndLink(User user, String link);

    // Excludes the YouTube Shorts of the feeds in the given folders, when hideShorts is true
    String NOT_HIDDEN_SHORT = " AND (:hideShorts = false OR p.link IS NULL OR p.link NOT LIKE '%youtube.com/shorts/%' OR " +
            "p.feed.folder IS NULL OR p.feed.folder.id NOT IN :hideShortsFolderIds)";

    // Posts of the user, only the unread ones unless includeRead is true
    @Query("SELECT p FROM Post p WHERE p.user = :user AND (p.read = false OR :includeRead = true)" + NOT_HIDDEN_SHORT)
    List<Post> findByUser(@Param("user") User user, @Param("includeRead") boolean includeRead, @Param("hideShorts") boolean hideShorts,
                            @Param("hideShortsFolderIds") List<UUID> hideShortsFolderIds, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND (p.read = false OR :includeRead = true) AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CAST(p.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))" + NOT_HIDDEN_SHORT)
    List<Post> findByUserAndTitleOrDescriptionContaining(@Param("user") User user, @Param("includeRead") boolean includeRead, @Param("searchTerm") String searchTerm, @Param("hideShorts") boolean hideShorts,
                            @Param("hideShortsFolderIds") List<UUID> hideShortsFolderIds, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND (p.read = false OR :includeRead = true) AND p.feed.folder.id = :folderId" + NOT_HIDDEN_SHORT)
    List<Post> findByUserAndFolder(@Param("user") User user, @Param("includeRead") boolean includeRead, @Param("folderId") UUID folderId, @Param("hideShorts") boolean hideShorts,
                            @Param("hideShortsFolderIds") List<UUID> hideShortsFolderIds, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND (p.read = false OR :includeRead = true) AND p.feed.folder.id = :folderId AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CAST(p.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))" + NOT_HIDDEN_SHORT)
    List<Post> findByUserAndFolderAndTitleOrDescriptionContaining(@Param("user") User user, @Param("includeRead") boolean includeRead, @Param("folderId") UUID folderId, @Param("searchTerm") String searchTerm, @Param("hideShorts") boolean hideShorts,
                            @Param("hideShortsFolderIds") List<UUID> hideShortsFolderIds, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND (p.read = false OR :includeRead = true) AND p.feed.id = :feedId" + NOT_HIDDEN_SHORT)
    List<Post> findByUserAndFeed(@Param("user") User user, @Param("includeRead") boolean includeRead, @Param("feedId") UUID feedId, @Param("hideShorts") boolean hideShorts,
                            @Param("hideShortsFolderIds") List<UUID> hideShortsFolderIds, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND (p.read = false OR :includeRead = true) AND p.feed.id = :feedId AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CAST(p.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))" + NOT_HIDDEN_SHORT)
    List<Post> findByUserAndFeedAndTitleOrDescriptionContaining(@Param("user") User user, @Param("includeRead") boolean includeRead, @Param("feedId") UUID feedId, @Param("searchTerm") String searchTerm, @Param("hideShorts") boolean hideShorts,
                            @Param("hideShortsFolderIds") List<UUID> hideShortsFolderIds, Pageable pageable);

    // Saved posts are returned whether read or not
    List<Post> findByUserAndSavedTrue(User user, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.saved = true AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CAST(p.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Post> findByUserAndSavedTrueAndTitleOrDescriptionContaining(@Param("user") User user, @Param("searchTerm") String searchTerm, Pageable pageable);

    long countByUserAndSavedTrue(User user);

    // Favorite posts are returned whether read or not
    List<Post> findByUserAndFavoriteTrue(User user, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.favorite = true AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CAST(p.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Post> findByUserAndFavoriteTrueAndTitleOrDescriptionContaining(@Param("user") User user, @Param("searchTerm") String searchTerm, Pageable pageable);

    long countByUserAndFavoriteTrue(User user);

    // Posts that the cleanups may delete: neither saved nor favorite
    List<Post> findByUserAndSavedFalseAndFavoriteFalse(User user);

    // Rows of [feed id, feed title, feed website, folder id (nullable), unread count] for every feed of the user,
    // including feeds without unread posts (count 0). The YouTube Shorts of the given folders aren't counted when hideShorts is true.
    @Query("SELECT f.id, f.title, f.htmlUrl, fo.id, COUNT(p) FROM Feed f LEFT JOIN f.folder fo " +
           "LEFT JOIN f.posts p ON p.user = :user AND p.read = false AND (:hideShorts = false OR p.link IS NULL OR " +
           "p.link NOT LIKE '%youtube.com/shorts/%' OR fo IS NULL OR fo.id NOT IN :hideShortsFolderIds) " +
           "WHERE f.user = :user GROUP BY f.id, f.title, f.htmlUrl, fo.id")
    List<Object[]> countUnreadByFeed(@Param("user") User user, @Param("hideShorts") boolean hideShorts,
                                      @Param("hideShortsFolderIds") List<UUID> hideShortsFolderIds);

    // Rows of [user id, number of posts, number of unread posts, number of saved posts, number of favorite posts] for every user that has posts
    @Query("SELECT p.user.id, COUNT(p), SUM(CASE WHEN p.read = false THEN 1 ELSE 0 END), SUM(CASE WHEN p.saved = true THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN p.favorite = true THEN 1 ELSE 0 END) " +
           "FROM Post p GROUP BY p.user.id")
    List<Object[]> countGroupedByUser();

    long countByUser(User user);

    // Links of the posts of a feed, to skip the entries already added in one query instead of one per entry
    @Query("SELECT p.link FROM Post p WHERE p.feed = :feed AND p.user = :user AND p.link IS NOT NULL")
    List<String> findLinksByFeedAndUser(@Param("feed") Feed feed, @Param("user") User user);

    // Links of the posts of the user's other feeds published since the given date that contain the given text,
    // to find articles that new tweets link to
    @Query("SELECT p.link FROM Post p WHERE p.user = :user AND p.feed <> :feed AND p.pubDate >= :since AND p.link LIKE :pattern")
    List<String> findOtherFeedLinksLike(@Param("user") User user, @Param("feed") Feed feed, @Param("since") LocalDateTime since,
                                        @Param("pattern") String pattern);

    // Id, link, live status, start and length of the feed's scheduled or live videos, to check whether their state changed,
    // of its YouTube videos published since the given date that were never checked, or have no length and weren't
    // checked since recheckBefore (e.g. checked before lengths existed), and of its saved or favorite YouTube videos (any age)
    // not checked since recheckBefore
    @Query("SELECT p.id, p.link, p.liveStatus, p.liveStartAt, p.duration FROM Post p WHERE p.feed = :feed AND p.user = :user AND " +
           "(p.liveStatus IS NOT NULL OR ((p.link LIKE '%youtube.com/%' OR p.link LIKE '%youtu.be/%') AND " +
           "((p.pubDate > :since AND (p.liveCheckedAt IS NULL OR (p.duration IS NULL AND p.liveCheckedAt < :recheckBefore))) OR " +
           "((p.saved = true OR p.favorite = true) AND (p.liveCheckedAt IS NULL OR p.liveCheckedAt < :recheckBefore)))))")
    List<Object[]> findLiveVideosByFeedAndUser(@Param("feed") Feed feed, @Param("user") User user,
                                               @Param("since") LocalDateTime since, @Param("recheckBefore") LocalDateTime recheckBefore);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.liveStatus = :status, p.liveStartAt = :startAt, p.duration = :duration, p.liveCheckedAt = :checkedAt WHERE p.id = :id")
    int setLiveState(@Param("id") UUID id, @Param("status") LiveStatus status, @Param("startAt") LocalDateTime startAt,
                     @Param("duration") Integer duration, @Param("checkedAt") LocalDateTime checkedAt);
    
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

    // Saved and favorite posts are kept, even if read
    @Modifying
    @Transactional
    @Query("DELETE FROM Post p WHERE p.read = true AND p.saved = false AND p.favorite = false AND p.readAt < :before")
    int deleteReadBefore(@Param("before") LocalDateTime before);
}
