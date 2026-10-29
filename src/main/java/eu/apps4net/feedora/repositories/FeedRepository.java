package eu.apps4net.feedora.repositories;

import eu.apps4net.feedora.models.Feed;
import eu.apps4net.feedora.models.Folder;
import eu.apps4net.feedora.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedRepository extends JpaRepository<Feed, UUID> {
    boolean existsByXmlUrlAndUser(String xmlUrl, User user);
    Optional<Feed> findByXmlUrlAndUser(String xmlUrl, User user);
    List<Feed> findByUser(User user);
    boolean existsByUser(User user);
    List<Feed> findByUser(User user, Pageable pageable);
    List<Feed> findByUserAndTitleContainingIgnoreCase(User user, String title, Pageable pageable);
    List<Feed> findByFolder(Folder folder);
    List<Feed> findByUserAndFolder_Id(User user, UUID folderId);
    List<Feed> findByIdAndUser(UUID id, User user);
    List<Feed> findByXmlUrlContaining(String part);

    // Problematic feeds first (with log entries of their own, not of their posts, or failing checks), then the most frequent ones, then by title.
    // Feeds not checked yet have no statistics and count as posting nothing.
    String PROBLEMS_FIRST_ORDER = " ORDER BY CASE WHEN EXISTS (SELECT 1 FROM LogEntry l WHERE l.user = :user AND l.feedId = f.id AND l.postId IS NULL)"
            + " OR COALESCE(s.consecutiveErrors, 0) > 0 THEN 0 ELSE 1 END,"
            + " COALESCE(s.postsPerDay, 0) DESC, f.title, f.id";

    @Query("SELECT f FROM Feed f LEFT JOIN f.statistics s WHERE f.user = :user" + PROBLEMS_FIRST_ORDER)
    List<Feed> findByUserProblemsFirst(@Param("user") User user, Pageable pageable);

    @Query("SELECT f FROM Feed f LEFT JOIN f.statistics s WHERE f.user = :user AND LOWER(f.title) LIKE LOWER(CONCAT('%', :title, '%'))" + PROBLEMS_FIRST_ORDER)
    List<Feed> findByUserAndTitleContainingProblemsFirst(@Param("user") User user, @Param("title") String title, Pageable pageable);
    
    // Rows of [user id, number of feeds] for every user that has feeds
    @Query("SELECT f.user.id, COUNT(f) FROM Feed f GROUP BY f.user.id")
    List<Object[]> countGroupedByUser();

    // Last check of any of the user's feeds, null if none was checked yet
    @Query("SELECT MAX(s.lastCheckedAt) FROM FeedStatistics s WHERE s.feed.user = :user")
    LocalDateTime findLastCheckedAtByUser(@Param("user") User user);

    @Modifying
    @Transactional
    void deleteByUser(User user);

    // Feeds whose favicon color wasn't checked yet
    List<Feed> findByFaviconColorIsNull(Pageable pageable);

    // Only the favicon color, so a parse or an edit of the feed at the same time isn't overwritten
    @Modifying
    @Transactional
    @Query("UPDATE Feed f SET f.faviconColor = :color WHERE f.id = :id")
    void updateFaviconColor(@Param("id") UUID id, @Param("color") String color);
}
