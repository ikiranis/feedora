package eu.apps4net.feedora.services;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.models.LanguageDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageServiceTest {
    private final LanguageService languageService = new LanguageService();

    @AfterEach
    void restoreLanguage() {
        Language.setLanguage("en");
    }

    @Test
    void returnsEveryTranslationAsTextAndTranslation() throws Exception {
        List<LanguageDTO> translations = languageService.getTranslations("el");

        assertThat(translations).isNotEmpty();
        assertThat(translations).filteredOn(t -> t.getText().equals("Settings"))
                .singleElement()
                .extracting(LanguageDTO::getTranslation)
                .isEqualTo("Ρυθμίσεις");
    }
}
