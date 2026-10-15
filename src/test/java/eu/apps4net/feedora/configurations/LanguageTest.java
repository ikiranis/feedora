package eu.apps4net.feedora.configurations;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageTest {

    @AfterEach
    void restoreLanguage() {
        Language.setLanguage("en");
        Language.setActionsLanguage("en");
    }

    @Test
    void getTranslationsReadsTheRequestedLanguage() throws Exception {
        assertThat(Language.getTranslations("en")).containsEntry("Settings", "Settings");
        assertThat(Language.getTranslations("el")).containsEntry("Settings", "Ρυθμίσεις");
    }

    @Test
    void unknownLanguagesFallBackToEnglish() throws Exception {
        assertThat(Language.getTranslations("xx")).containsEntry("Settings", "Settings");
    }

    @Test
    void setLanguageChangesTheStrings() {
        Language.setLanguage("el");

        assertThat(Language.getLanguage()).isEqualTo("el");
        assertThat(Language.getString("Settings")).isEqualTo("Ρυθμίσεις");
    }

    @Test
    void actionStringsHaveTheirOwnLanguage() {
        Language.setLanguage("en");
        Language.setActionsLanguage("el");

        assertThat(Language.getString("Post not found")).isEqualTo("Post not found");
        assertThat(Language.getActionString("Post not found")).isEqualTo("Το άρθρο δεν βρέθηκε");
    }

    @Test
    void missingKeysReturnTheKey() {
        assertThat(Language.getString("No such key")).isEqualTo("No such key");
        assertThat(Language.getActionString("No such key")).isEqualTo("No such key");
    }
}
