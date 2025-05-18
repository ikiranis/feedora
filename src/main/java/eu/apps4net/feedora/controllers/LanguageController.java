package eu.apps4net.feedora.controllers;

import org.springframework.web.bind.annotation.*;

import eu.apps4net.feedora.configurations.Language;
import eu.apps4net.feedora.exceptions.NotFoundException;
import eu.apps4net.feedora.models.LanguageDTO;
import eu.apps4net.feedora.services.LanguageService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/language")
public class LanguageController {
	private final LanguageService languageService;

	public LanguageController(LanguageService languageService) {
		this.languageService = languageService;
	}

	@GetMapping(path = "all/{language}")
	public List<LanguageDTO> getLanguages(@PathVariable("language") String language) {
		try {
			return languageService.getTranslations(language);
		} catch (Exception e) {
			throw new NotFoundException(Language.getString("Languages not found"));
		}
	}

	@PostMapping()
	public void setLanguage(@RequestBody Map<String, String> request) {
		Language.setLanguage(request.get("language"));
	}
}
