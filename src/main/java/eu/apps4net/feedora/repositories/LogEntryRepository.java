package eu.apps4net.feedora.repositories;

import eu.apps4net.feedora.models.LogEntry;
import eu.apps4net.feedora.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LogEntryRepository extends JpaRepository<LogEntry, UUID> {
    // Rows of [feed id, number of log entries] for the given feeds that have entries
    @Query("SELECT l.feedId, COUNT(l) FROM LogEntry l WHERE l.user = :user AND l.feedId IN :feedIds GROUP BY l.feedId")
    List<Object[]> countByFeedIds(@Param("user") User user, @Param("feedIds") Collection<UUID> feedIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM LogEntry l WHERE l.user = :user")
    int deleteByUser(@Param("user") User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM LogEntry l WHERE l.user = :user AND l.feedId = :feedId")
    int deleteByUserAndFeedId(@Param("user") User user, @Param("feedId") UUID feedId);

    long countByUserAndPostId(User user, UUID postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM LogEntry l WHERE l.user = :user AND l.postId = :postId")
    int deleteByUserAndPostId(@Param("user") User user, @Param("postId") UUID postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM LogEntry l WHERE l.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
