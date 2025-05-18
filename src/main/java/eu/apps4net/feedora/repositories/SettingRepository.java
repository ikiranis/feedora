package eu.apps4net.feedora.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.apps4net.feedora.models.Setting;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Long> {
	Setting getBySettingName(String name);
}
