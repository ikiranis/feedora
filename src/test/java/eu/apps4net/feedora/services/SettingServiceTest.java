package eu.apps4net.feedora.services;

import eu.apps4net.feedora.models.Setting;
import eu.apps4net.feedora.repositories.SettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingServiceTest {
    @Mock
    private SettingRepository settingRepository;
    @InjectMocks
    private SettingService settingService;

    @Test
    void setDefaultSettingsCreatesThemOnAnEmptyDatabase() {
        when(settingRepository.findAll()).thenReturn(List.of());

        settingService.setDefaultSettings();

        ArgumentCaptor<Setting> saved = ArgumentCaptor.forClass(Setting.class);
        verify(settingRepository, times(4)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Setting::getSettingName, Setting::getSettingValue)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("defaultActionsLanguage", "en"),
                        org.assertj.core.groups.Tuple.tuple("registrationEnabled", "true"),
                        org.assertj.core.groups.Tuple.tuple("youtubeApiKey", ""),
                        org.assertj.core.groups.Tuple.tuple("liveNewsfeedRefreshSeconds", "60"));
    }

    @Test
    void setDefaultSettingsOnlyAddsMissingSettings() {
        Setting registration = new Setting("registrationEnabled", "true");
        when(settingRepository.findAll()).thenReturn(List.of(registration));
        when(settingRepository.getBySettingName("uploadDir")).thenReturn(null);
        when(settingRepository.getBySettingName("defaultActionsLanguage")).thenReturn(null);
        when(settingRepository.getBySettingName("registrationEnabled")).thenReturn(registration);
        when(settingRepository.getBySettingName("youtubeApiKey")).thenReturn(new Setting("youtubeApiKey", ""));
        when(settingRepository.getBySettingName("liveNewsfeedRefreshSeconds")).thenReturn(new Setting("liveNewsfeedRefreshSeconds", "60"));

        settingService.setDefaultSettings();

        ArgumentCaptor<Setting> saved = ArgumentCaptor.forClass(Setting.class);
        verify(settingRepository).save(saved.capture());
        assertThat(saved.getValue().getSettingName()).isEqualTo("defaultActionsLanguage");
    }

    @Test
    void setDefaultSettingsKeepsExistingSettings() {
        when(settingRepository.findAll()).thenReturn(List.of(new Setting("defaultActionsLanguage", "el")));
        when(settingRepository.getBySettingName("uploadDir")).thenReturn(null);
        when(settingRepository.getBySettingName("defaultActionsLanguage")).thenReturn(new Setting("defaultActionsLanguage", "el"));
        when(settingRepository.getBySettingName("registrationEnabled")).thenReturn(new Setting("registrationEnabled", "false"));
        when(settingRepository.getBySettingName("youtubeApiKey")).thenReturn(new Setting("youtubeApiKey", "key"));
        when(settingRepository.getBySettingName("liveNewsfeedRefreshSeconds")).thenReturn(new Setting("liveNewsfeedRefreshSeconds", "30"));

        settingService.setDefaultSettings();

        verify(settingRepository, never()).save(any());
        verify(settingRepository, never()).delete(any());
    }

    @Test
    void setDefaultSettingsRemovesTheUploadDirSetting() {
        Setting uploadDir = new Setting("uploadDir", "uploads");
        when(settingRepository.findAll()).thenReturn(List.of(uploadDir));
        when(settingRepository.getBySettingName("uploadDir")).thenReturn(uploadDir);

        settingService.setDefaultSettings();

        verify(settingRepository).delete(uploadDir);
    }

    @Test
    void gettersReadTheSettingValues() {
        when(settingRepository.getBySettingName("defaultActionsLanguage")).thenReturn(new Setting("defaultActionsLanguage", "el"));

        assertThat(settingService.getDefaultActionsLanguage()).isEqualTo("el");
    }

    @Test
    void youtubeApiKeyIsNullWhenEmpty() {
        when(settingRepository.getBySettingName("youtubeApiKey")).thenReturn(new Setting("youtubeApiKey", " "));
        assertThat(settingService.getYoutubeApiKey()).isNull();

        when(settingRepository.getBySettingName("youtubeApiKey")).thenReturn(new Setting("youtubeApiKey", " AIzaSyExample_key-123456789 "));
        assertThat(settingService.getYoutubeApiKey()).isEqualTo("AIzaSyExample_key-123456789");
        assertThat(settingService.fieldValidation("youtubeApiKey", "")).isTrue();
        assertThat(settingService.fieldValidation("youtubeApiKey", "AIzaSyExample_key-123456789")).isTrue();
        assertThat(settingService.fieldValidation("youtubeApiKey", "not a key")).isFalse();
    }

    @Test
    void getSettingReturnsNullWhenMissing() {
        Setting setting = new Setting("a", "b");
        when(settingRepository.findById(1L)).thenReturn(Optional.of(setting));
        when(settingRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(settingService.getSetting(1L)).isSameAs(setting);
        assertThat(settingService.getSetting(2L)).isNull();
    }

    @Test
    void updateSettingSavesIt() {
        Setting setting = new Setting("a", "b");
        settingService.updateSetting(setting);
        verify(settingRepository).save(setting);
    }

    @Test
    void everyValueIsValid() {
        assertThat(settingService.fieldValidation("anything", "anything")).isTrue();
    }

    @Test
    void liveNewsfeedRefreshSecondsMustBeAtLeastTheMinimum() {
        assertThat(settingService.fieldValidation("liveNewsfeedRefreshSeconds", "60")).isTrue();
        assertThat(settingService.fieldValidation("liveNewsfeedRefreshSeconds", "5")).isTrue();
        assertThat(settingService.fieldValidation("liveNewsfeedRefreshSeconds", "4")).isFalse();
        assertThat(settingService.fieldValidation("liveNewsfeedRefreshSeconds", "1.5")).isFalse();
        assertThat(settingService.fieldValidation("liveNewsfeedRefreshSeconds", "")).isFalse();
    }

    @Test
    void liveNewsfeedRefreshSecondsFallsBackToTheDefault() {
        when(settingRepository.getBySettingName("liveNewsfeedRefreshSeconds")).thenReturn(new Setting("liveNewsfeedRefreshSeconds", " 30 "));
        assertThat(settingService.getLiveNewsfeedRefreshSeconds()).isEqualTo(30);

        when(settingRepository.getBySettingName("liveNewsfeedRefreshSeconds")).thenReturn(new Setting("liveNewsfeedRefreshSeconds", "abc"));
        assertThat(settingService.getLiveNewsfeedRefreshSeconds()).isEqualTo(60);

        when(settingRepository.getBySettingName("liveNewsfeedRefreshSeconds")).thenReturn(null);
        assertThat(settingService.getLiveNewsfeedRefreshSeconds()).isEqualTo(60);
    }

    @Test
    void registrationEnabledMustBeABoolean() {
        assertThat(settingService.fieldValidation("registrationEnabled", "true")).isTrue();
        assertThat(settingService.fieldValidation("registrationEnabled", "false")).isTrue();
        assertThat(settingService.fieldValidation("registrationEnabled", "yes")).isFalse();
    }

    @Test
    void registrationIsEnabledUnlessSetToFalse() {
        when(settingRepository.getBySettingName("registrationEnabled")).thenReturn(null);
        assertThat(settingService.isRegistrationEnabled()).isTrue();

        when(settingRepository.getBySettingName("registrationEnabled")).thenReturn(new Setting("registrationEnabled", "false"));
        assertThat(settingService.isRegistrationEnabled()).isFalse();
    }
}
