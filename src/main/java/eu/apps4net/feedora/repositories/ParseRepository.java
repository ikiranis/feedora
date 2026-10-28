package eu.apps4net.feedora.repositories;

import eu.apps4net.feedora.models.Parse;
import eu.apps4net.feedora.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ParseRepository extends JpaRepository<Parse, UUID> {
    List<Parse> findAllByUserOrderByCreatedAtDesc(User user);

    List<Parse> findAllByUser(User user);

    boolean existsByUrlAndUser(String url, User user);

    boolean existsByFeedName(String feedName);
}
