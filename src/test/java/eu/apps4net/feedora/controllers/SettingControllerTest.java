package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.models.Setting;
import eu.apps4net.feedora.repositories.SettingRepository;
import eu.apps4net.feedora.support.ApiTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettingControllerTest extends ApiTestBase {
    @Autowired
    private SettingRepository settingRepository;

    @AfterEach
    void restoreSettings() {
        Setting language = settingRepository.getBySettingName("defaultActionsLanguage");
        language.setSettingValue("en");
        settingRepository.save(language);
        Language.setActionsLanguage("en");
    }

    @Test
    void returnsTheDefaultSettings() throws Exception {
        mockMvc.perform(auth(get("/api/setting/all")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].settingName", hasItems("uploadDir", "defaultActionsLanguage")));
    }

    @Test
    void returnsASettingById() throws Exception {
        Setting setting = settingRepository.getBySettingName("uploadDir");

        mockMvc.perform(auth(get("/api/setting/" + setting.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settingName").value("uploadDir"))
                .andExpect(jsonPath("$.settingValue").value("uploads"));
    }

    @Test
    void updatesOnlyTheSettingValue() throws Exception {
        Setting setting = settingRepository.getBySettingName("uploadDir");
        String body = "{\"id\":\"" + setting.getId() + "\",\"settingName\":\"renamed\",\"settingValue\":\"files\"}";

        mockMvc.perform(auth(put("/api/setting")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        Setting updated = settingRepository.findById(setting.getId()).orElseThrow();
        assertThat(updated.getSettingValue()).isEqualTo("files");
        assertThat(updated.getSettingName()).isEqualTo("uploadDir");

        updated.setSettingValue("uploads");
        settingRepository.save(updated);
    }

    @Test
    void updatingTheActionsLanguageChangesTheActionMessages() throws Exception {
        Setting setting = settingRepository.getBySettingName("defaultActionsLanguage");
        String body = "{\"id\":\"" + setting.getId() + "\",\"settingName\":\"defaultActionsLanguage\",\"settingValue\":\"el\"}";

        mockMvc.perform(auth(put("/api/setting")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        assertThat(settingRepository.getBySettingName("defaultActionsLanguage").getSettingValue()).isEqualTo("el");
        assertThat(Language.getActionString("Post not found")).isEqualTo("Το άρθρο δεν βρέθηκε");
    }
}
