package eu.apps4net.feedora.repositories;

import eu.apps4net.feedora.models.LogEntry;
import eu.apps4net.feedora.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public interface LogEntryRepository extends JpaRepository<LogEntry, UUID> {
    @Modifying
    @Transactional
    @Query("DELETE FROM LogEntry l WHERE l.user = :user")
    int deleteByUser(@Param("user") User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM LogEntry l WHERE l.user = :user AND l.feedId = :feedId")
    int deleteByUserAndFeedId(@Param("user") User user, @Param("feedId") UUID feedId);

    @Modifying
    @Transactional
    @Query("DELETE FROM LogEntry l WHERE l.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
