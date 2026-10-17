package eu.apps4net.feedora.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.apps4net.feedora.models.Setting;
import eu.apps4net.feedora.repositories.SettingRepository;

import java.util.List;

@Service
public class SettingService {

	public static final String REGISTRATION_ENABLED = "registrationEnabled";
	// YouTube Data API key for checking scheduled/live videos; empty to read the videos' pages instead
	public static final String YOUTUBE_API_KEY = "youtubeApiKey";
	// How often the live newsfeed page checks for new posts, in seconds
	public static final String LIVE_NEWSFEED_REFRESH_SECONDS = "liveNewsfeedRefreshSeconds";
	public static final int DEFAULT_LIVE_NEWSFEED_REFRESH_SECONDS = 60;
	public static final int MIN_LIVE_NEWSFEED_REFRESH_SECONDS = 5;

	private final SettingRepository settingRepository;

	@Autowired
	public SettingService(SettingRepository settingRepository) {
		this.settingRepository = settingRepository;
	}

	/**
	 * Sets default settings if there are no settings in the database.
	 */
	public void setDefaultSettings() {
		List<Setting> settings = settingRepository.findAll();

		if (settings.isEmpty() || settingRepository.getBySettingName("uploadDir") == null) {
			settingRepository.save(new Setting("uploadDir", "uploads"));
		}

		if (settings.isEmpty() || settingRepository.getBySettingName("defaultActionsLanguage") == null) {
			settingRepository.save(new Setting("defaultActionsLanguage", "en"));
		}

		if (settings.isEmpty() || settingRepository.getBySettingName(REGISTRATION_ENABLED) == null) {
			settingRepository.save(new Setting(REGISTRATION_ENABLED, "true"));
		}

		if (settings.isEmpty() || settingRepository.getBySettingName(YOUTUBE_API_KEY) == null) {
			settingRepository.save(new Setting(YOUTUBE_API_KEY, ""));
		}

		if (settings.isEmpty() || settingRepository.getBySettingName(LIVE_NEWSFEED_REFRESH_SECONDS) == null) {
			settingRepository.save(new Setting(LIVE_NEWSFEED_REFRESH_SECONDS, String.valueOf(DEFAULT_LIVE_NEWSFEED_REFRESH_SECONDS)));
		}
	}

	/**
	 * Returns the upload directory.
	 *
	 * @return the upload directory
	 */
	public String getUploadDir() {
		return settingRepository.getBySettingName("uploadDir").getSettingValue();
	}

	public String getDefaultActionsLanguage() {
		return settingRepository.getBySettingName("defaultActionsLanguage").getSettingValue();
	}

	/**
	 * Returns whether new users can register. The very first user can always register.
	 *
	 * @return true when the registration of new users is enabled
	 */
	public boolean isRegistrationEnabled() {
		Setting setting = settingRepository.getBySettingName(REGISTRATION_ENABLED);
		return setting == null || !"false".equalsIgnoreCase(setting.getSettingValue());
	}

	/**
	 * Returns the YouTube Data API key.
	 *
	 * @return the API key, or null if none is set
	 */
	public String getYoutubeApiKey() {
		Setting setting = settingRepository.getBySettingName(YOUTUBE_API_KEY);
		return setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()
				? null : setting.getSettingValue().trim();
	}

	/**
	 * Returns how often the live newsfeed page checks for new posts.
	 *
	 * @return the interval in seconds, the default if the setting is missing or invalid
	 */
	public int getLiveNewsfeedRefreshSeconds() {
		Setting setting = settingRepository.getBySettingName(LIVE_NEWSFEED_REFRESH_SECONDS);
		if (setting == null || !isValidRefreshSeconds(setting.getSettingValue())) {
			return DEFAULT_LIVE_NEWSFEED_REFRESH_SECONDS;
		}
		return Integer.parseInt(setting.getSettingValue().trim());
	}

	private static boolean isValidRefreshSeconds(String value) {
		try {
			return value != null && Integer.parseInt(value.trim()) >= MIN_LIVE_NEWSFEED_REFRESH_SECONDS;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Get all settings.
	 *
	 * @return List<Setting>
	 */
	public List<Setting> getSettings() {
		return settingRepository.findAll();
	}

	/**
	 * Get a setting by id
	 *
	 * @param id
	 * @return Setting
	 */
	public Setting getSetting(Long id) {
		return settingRepository.findById(id).orElse(null);
	}

	/**
	 * Update a setting
	 *
	 * @param setting
	 */
	public void updateSetting(Setting setting) {
		settingRepository.save(setting);
	}

	public Boolean fieldValidation(String settingName, String settingValue) {
		if (REGISTRATION_ENABLED.equals(settingName)) {
			return "true".equals(settingValue) || "false".equals(settingValue);
		}
		if (YOUTUBE_API_KEY.equals(settingName)) {
			return settingValue == null || settingValue.isBlank() || settingValue.trim().matches("[\\w-]{20,100}");
		}
		if (LIVE_NEWSFEED_REFRESH_SECONDS.equals(settingName)) {
			return isValidRefreshSeconds(settingValue);
		}
		return true;
	}
}
