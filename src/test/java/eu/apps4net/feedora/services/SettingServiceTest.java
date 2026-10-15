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
        verify(settingRepository, times(3)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Setting::getSettingName, Setting::getSettingValue)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("uploadDir", "uploads"),
                        org.assertj.core.groups.Tuple.tuple("defaultActionsLanguage", "en"),
                        org.assertj.core.groups.Tuple.tuple("registrationEnabled", "true"));
    }

    @Test
    void setDefaultSettingsOnlyAddsMissingSettings() {
        Setting uploadDir = new Setting("uploadDir", "files");
        when(settingRepository.findAll()).thenReturn(List.of(uploadDir));
        when(settingRepository.getBySettingName("uploadDir")).thenReturn(uploadDir);
        when(settingRepository.getBySettingName("defaultActionsLanguage")).thenReturn(null);
        when(settingRepository.getBySettingName("registrationEnabled")).thenReturn(new Setting("registrationEnabled", "true"));

        settingService.setDefaultSettings();

        ArgumentCaptor<Setting> saved = ArgumentCaptor.forClass(Setting.class);
        verify(settingRepository).save(saved.capture());
        assertThat(saved.getValue().getSettingName()).isEqualTo("defaultActionsLanguage");
    }

    @Test
    void setDefaultSettingsKeepsExistingSettings() {
        when(settingRepository.findAll()).thenReturn(List.of(new Setting("uploadDir", "files")));
        when(settingRepository.getBySettingName("uploadDir")).thenReturn(new Setting("uploadDir", "files"));
        when(settingRepository.getBySettingName("defaultActionsLanguage")).thenReturn(new Setting("defaultActionsLanguage", "el"));
        when(settingRepository.getBySettingName("registrationEnabled")).thenReturn(new Setting("registrationEnabled", "false"));

        settingService.setDefaultSettings();

        verify(settingRepository, never()).save(any());
    }

    @Test
    void gettersReadTheSettingValues() {
        when(settingRepository.getBySettingName("uploadDir")).thenReturn(new Setting("uploadDir", "files"));
        when(settingRepository.getBySettingName("defaultActionsLanguage")).thenReturn(new Setting("defaultActionsLanguage", "el"));

        assertThat(settingService.getUploadDir()).isEqualTo("files");
        assertThat(settingService.getDefaultActionsLanguage()).isEqualTo("el");
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
