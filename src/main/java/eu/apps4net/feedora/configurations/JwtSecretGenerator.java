package eu.apps4net.feedora.configurations;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * Gives the app a jwt.secret when none is configured, e.g. with the release jar, which doesn't include
 * application-secrets.properties. The secret is created once, saved to application-secrets.properties
 * in the data folder (next to db/) and read from there on the next starts.
 * <p>
 * The data folder is feedora.data-dir (set by DesktopWindow to the jar folder), or the working folder.
 */
public class JwtSecretGenerator implements EnvironmentPostProcessor, Ordered {
	private static final String FILE_NAME = "application-secrets.properties";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (environment.containsProperty("jwt.secret")) {
			return;
		}

		Path file = Path.of(environment.getProperty("feedora.data-dir", "")).resolve(FILE_NAME).toAbsolutePath();
		Properties properties = new Properties();
		try {
			if (Files.exists(file)) {
				try (InputStream in = Files.newInputStream(file)) {
					properties.load(in);
				}
			}
			if (properties.getProperty("jwt.secret") == null) {
				properties.setProperty("jwt.secret", newSecret());
				Files.writeString(file, """
						# Created by Feedora on the first start. Changing jwt.secret signs out every user.
						# Keep this file private, anyone with the secret can log in as any user.
						jwt.secret=%s
						""".formatted(properties.getProperty("jwt.secret")));
				System.out.println("Created a new JWT secret in " + file);
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Can't read or create the JWT secret file " + file, e);
		}

		// Lowest priority, so it can be overridden like any other property
		environment.getPropertySources().addLast(new PropertiesPropertySource(FILE_NAME, properties));
	}

	private static String newSecret() {
		byte[] bytes = new byte[64];
		new SecureRandom().nextBytes(bytes);
		return Base64.getEncoder().encodeToString(bytes);
	}

	// After the application*.properties files are loaded, to see if they set jwt.secret
	@Override
	public int getOrder() {
		return ConfigDataEnvironmentPostProcessor.ORDER + 1;
	}
}
