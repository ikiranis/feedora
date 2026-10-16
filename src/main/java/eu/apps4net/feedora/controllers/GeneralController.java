package eu.apps4net.feedora.controllers;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/general")
public class GeneralController {

	// Missing when the app runs without a Maven build (e.g. straight from an IDE)
	private final BuildProperties buildProperties;

	public GeneralController(ObjectProvider<BuildProperties> buildProperties) {
		this.buildProperties = buildProperties.getIfAvailable();
	}

	@GetMapping(path = "appAlive")
	public boolean getAppAlive() {
		return true;
	}

	/**
	 * The app version from the pom, e.g. to check which version a Docker container runs.
	 */
	@GetMapping(path = "version")
	public String getVersion() {
		return buildProperties != null ? buildProperties.getVersion() : "dev";
	}
}
