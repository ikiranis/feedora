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

    // Feeds that have log entries first, then by title
    String LOGS_FIRST_ORDER = " ORDER BY CASE WHEN EXISTS (SELECT 1 FROM LogEntry l WHERE l.user = :user AND l.feedId = f.id) THEN 0 ELSE 1 END, f.title";

    @Query("SELECT f FROM Feed f WHERE f.user = :user" + LOGS_FIRST_ORDER)
    List<Feed> findByUserLogsFirst(@Param("user") User user, Pageable pageable);

    @Query("SELECT f FROM Feed f WHERE f.user = :user AND LOWER(f.title) LIKE LOWER(CONCAT('%', :title, '%'))" + LOGS_FIRST_ORDER)
    List<Feed> findByUserAndTitleContainingLogsFirst(@Param("user") User user, @Param("title") String title, Pageable pageable);
    
    @Modifying
    @Transactional
    void deleteByUser(User user);
}
