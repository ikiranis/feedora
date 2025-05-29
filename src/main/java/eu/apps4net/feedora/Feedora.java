package eu.apps4net.feedora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.services.SettingService;
import eu.apps4net.feedora.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication
public class Feedora {
    private static SettingService settingService;
    private static UserService userService;

    @Autowired
    public Feedora(SettingService settingService, UserService userService) {
        Feedora.settingService = settingService;
        Feedora.userService = userService;
    }

    public static void main(String[] args)
    {
        SpringApplication.run(Feedora.class, args);
        System.out.println("Run app on http://localhost:9999");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initAdminUser() {
        userService.getOrCreateAdminUser();
        
        // Initialize Language configuration after Spring context is ready
        try {
            String defaultLanguage = settingService.getDefaultActionsLanguage();
            Language.setLanguage(defaultLanguage); // This initializes languageStrings
            Language.setActionsLanguage(defaultLanguage); // This initializes actionStrings
            System.out.println("Language configuration initialized with: " + defaultLanguage);
        } catch (Exception e) {
            System.err.println("Failed to initialize language configuration, using default 'en': " + e.getMessage());
            // Fallback to English if there's an issue
            Language.setLanguage("en");
            Language.setActionsLanguage("en");
        }
    }
}
