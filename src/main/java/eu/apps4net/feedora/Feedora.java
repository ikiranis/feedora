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
        Language.setActionsLanguage(settingService.getDefaultActionsLanguage());
        System.out.println("Run app on http://localhost:9999");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initAdminUser() {
        userService.getOrCreateAdminUser();
    }
}
