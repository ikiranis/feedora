package eu.apps4net.feedora.repositories;

import eu.apps4net.feedora.models.FeedStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeedStatisticsRepository extends JpaRepository<FeedStatistics, UUID> {
}
