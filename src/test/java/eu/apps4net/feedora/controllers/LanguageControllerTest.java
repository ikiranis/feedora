package eu.apps4net.feedora.controllers;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.support.ApiTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LanguageControllerTest extends ApiTestBase {

    @AfterEach
    void restoreLanguage() {
        Language.setLanguage("en");
    }

    @Test
    void returnsEnglishTranslations() throws Exception {
        mockMvc.perform(get("/api/language/all/en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.text == 'Settings')].translation", hasItem("Settings")));
    }

    @Test
    void returnsGreekTranslations() throws Exception {
        mockMvc.perform(get("/api/language/all/el"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.text == 'Settings')].translation", hasItem("Ρυθμίσεις")));
    }

    @Test
    void unknownLanguageFallsBackToEnglish() throws Exception {
        mockMvc.perform(get("/api/language/all/xx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.text == 'Settings')].translation", hasItem("Settings")));
    }

    @Test
    void setLanguageChangesTheServerMessagesLanguage() throws Exception {
        mockMvc.perform(post("/api/language").contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"el\"}"))
                .andExpect(status().isOk());

        assertThat(Language.getLanguage()).isEqualTo("el");
        assertThat(Language.getString("Settings")).isEqualTo("Ρυθμίσεις");
    }
}
