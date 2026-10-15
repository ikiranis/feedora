package eu.apps4net.feedora.utilities;

import eu.apps4net.feedora.models.Setting;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReflectionUtilsTest {

    @Test
    void updatesOnlyTheSelectedFields() throws Exception {
        Setting setting = new Setting("name", "old");

        ReflectionUtils.updateFields(setting, Map.of("settingName", "renamed", "settingValue", "new"), new String[]{"settingValue"});

        assertThat(setting.getSettingValue()).isEqualTo("new");
        assertThat(setting.getSettingName()).isEqualTo("name");
    }

    @Test
    void ignoresKeysThatArentFields() throws Exception {
        Setting setting = new Setting("name", "old");

        ReflectionUtils.updateFields(setting, Map.of("unknown", "x"), new String[]{"unknown"});

        assertThat(setting.getSettingName()).isEqualTo("name");
        assertThat(setting.getSettingValue()).isEqualTo("old");
    }

    @Test
    void setsFieldsThatWereNull() throws Exception {
        Setting setting = new Setting("name", null);

        ReflectionUtils.updateFields(setting, Map.of("settingValue", "value"), new String[]{"settingValue"});

        assertThat(setting.getSettingValue()).isEqualTo("value");
    }
}
