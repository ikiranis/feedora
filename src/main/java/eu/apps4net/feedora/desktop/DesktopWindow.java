package eu.apps4net.feedora.desktop;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Small window shown when the jar is started without a terminal (e.g. with a double click), so the
 * app is visible and can be opened in the browser or stopped. Spring starts in a background thread.
 * <p>
 * Force it on or off with the {@code --gui} / {@code --no-gui} arguments.
 */
public class DesktopWindow extends Application {
	private static Class<?> appClass;
	private static String[] appArgs;
	private static volatile boolean windowStarted;
	private static Path dataDir = Path.of("").toAbsolutePath();

	private ConfigurableApplicationContext context;
	private String appUrl;

	private final Label statusLabel = new Label("Starting...");
	private final Label versionLabel = new Label("-");
	private final Label dataLabel = new Label(dataDir.toString());
	private final Hyperlink urlLink = new Hyperlink("-");
	private final Hyperlink networkLink = new Hyperlink("-");
	private final Button openButton = new Button("Open in browser");

	/**
	 * True when the app should show the window: not started from a terminal, and a display exists.
	 */
	public static boolean shouldShow(String[] args) {
		if (Arrays.asList(args).contains("--no-gui")) {
			return false;
		}
		if (Arrays.asList(args).contains("--gui")) {
			return true;
		}
		return !startedFromTerminal() && hasDisplay();
	}

	/**
	 * Shows the window and starts the Spring app. Blocks until the window is closed.
	 *
	 * @return false when the window couldn't be opened (e.g. no access to the display), so the app
	 * should start without it
	 */
	public static boolean start(Class<?> appClass, String[] args) {
		DesktopWindow.appClass = appClass;
		DesktopWindow.appArgs = Arrays.stream(args)
				.filter(arg -> !arg.equals("--gui") && !arg.equals("--no-gui"))
				.toArray(String[]::new);
		useJarFolderForData(appClass);
		try {
			Application.launch(DesktopWindow.class);
			return true;
		} catch (RuntimeException e) {
			// Once the window is open Spring is started too, so starting again would fail
			if (windowStarted) {
				throw e;
			}
			System.err.println("Can't open the Feedora window, running without it: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Keeps the database and the generated feeds next to the jar. With a double click the working
	 * folder is up to the file manager (often the home folder), so the relative paths can't be used.
	 * Does nothing when the app doesn't run from a jar (e.g. from an IDE).
	 */
	private static void useJarFolderForData(Class<?> appClass) {
		File jar = new ApplicationHome(appClass).getSource();
		if (jar == null || !jar.isFile()) {
			return;
		}
		dataDir = jar.getAbsoluteFile().getParentFile().toPath();

		// Where JwtSecretGenerator keeps the generated secret
		if (System.getProperty("feedora.data-dir") == null) {
			System.setProperty("feedora.data-dir", dataDir.toString());
		}
		// Derby resolves the relative database name of spring.datasource.url (and derby.log) against it
		if (System.getProperty("derby.system.home") == null) {
			System.setProperty("derby.system.home", dataDir.toString());
		}
		// System properties override application.properties, command line arguments still override this
		if (System.getProperty("feedora.parser.feeds-dir") == null) {
			System.setProperty("feedora.parser.feeds-dir", dataDir.resolve("feeds").toString());
		}
	}

	private static boolean startedFromTerminal() {
		Console console = System.console();
		if (console == null) {
			return false;
		}
		// From JDK 22 System.console() is never null, Console.isTerminal() tells if it's a real terminal
		try {
			return (boolean) Console.class.getMethod("isTerminal").invoke(console);
		} catch (ReflectiveOperationException e) {
			return true;
		}
	}

	// On Linux there is no display on servers / Docker, so don't try to open a window there
	private static boolean hasDisplay() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (os.contains("win") || os.contains("mac")) {
			return true;
		}
		return System.getenv("DISPLAY") != null || System.getenv("WAYLAND_DISPLAY") != null;
	}

	@Override
	public void start(Stage stage) {
		windowStarted = true;
		Label titleLabel = new Label("Feedora");
		titleLabel.setFont(Font.font(null, FontWeight.BOLD, 24));
		Label subtitleLabel = new Label("RSS reader");

		GridPane info = new GridPane();
		info.setHgap(12);
		info.setVgap(6);
		info.addRow(0, new Label("Status:"), statusLabel);
		info.addRow(1, new Label("Version:"), versionLabel);
		info.addRow(2, new Label("Address:"), urlLink);
		info.addRow(3, new Label("Network:"), networkLink);
		info.addRow(4, new Label("Data folder:"), dataLabel);
		// Keep the labels column from being cut off ("Data f...")
		ColumnConstraints labelsColumn = new ColumnConstraints();
		labelsColumn.setMinWidth(Region.USE_PREF_SIZE);
		info.getColumnConstraints().add(labelsColumn);
		statusLabel.setWrapText(true);
		statusLabel.setMaxWidth(380);
		dataLabel.setWrapText(true);
		dataLabel.setMaxWidth(380);
		urlLink.setDisable(true);
		urlLink.setPadding(Insets.EMPTY);
		urlLink.setOnAction(e -> openInBrowser(urlLink.getText()));
		networkLink.setDisable(true);
		networkLink.setPadding(Insets.EMPTY);
		networkLink.setOnAction(e -> openInBrowser(networkLink.getText()));

		openButton.setDisable(true);
		openButton.setDefaultButton(true);
		openButton.setOnAction(e -> openInBrowser(appUrl));
		Button quitButton = new Button("Quit");
		quitButton.setOnAction(e -> quit());
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox buttons = new HBox(8, spacer, openButton, quitButton);
		buttons.setAlignment(Pos.CENTER_RIGHT);

		VBox root = new VBox(12, new VBox(titleLabel, subtitleLabel), info, buttons);
		root.setPadding(new Insets(20));
		root.setPrefWidth(500);

		stage.setTitle("Feedora");
		stage.setScene(new Scene(root));
		stage.setResizable(false);
		// Closing the window stops the app, otherwise it would keep running with no way to stop it
		stage.setOnCloseRequest(e -> quit());
		stage.show();

		Thread springThread = new Thread(this::startSpring, "spring-main");
		springThread.setContextClassLoader(DesktopWindow.class.getClassLoader());
		springThread.start();
	}

	private void startSpring() {
		try {
			context = SpringApplication.run(appClass, appArgs);
			String port = context.getEnvironment().getProperty("local.server.port", "9990");
			BuildProperties buildProperties = context.getBeanProvider(BuildProperties.class).getIfAvailable();
			String version = buildProperties != null ? buildProperties.getVersion() : "dev";
			appUrl = "http://localhost:" + port;
			String hostIp = findHostIp();

			Platform.runLater(() -> {
				statusLabel.setText("Running");
				versionLabel.setText(version);
				urlLink.setText(appUrl);
				urlLink.setDisable(false);
				if (hostIp != null) {
					networkLink.setText("http://" + hostIp + ":" + port);
					networkLink.setDisable(false);
				} else {
					networkLink.setText("Not connected to a network");
				}
				openButton.setDisable(false);
			});
		} catch (Throwable e) {
			Throwable cause = e;
			while (cause.getCause() != null) {
				cause = cause.getCause();
			}
			String message = "Failed to start: " + cause.getMessage();
			Platform.runLater(() -> statusLabel.setText(message));
		}
	}

	/**
	 * The IP other machines of the local network can use to open the app, or null when there is none.
	 */
	private static String findHostIp() {
		// Connecting a UDP socket sends nothing, it only picks the interface of the default route, so
		// virtual interfaces (Docker, VPNs etc) are skipped
		try (DatagramSocket socket = new DatagramSocket()) {
			socket.connect(InetAddress.getByName("8.8.8.8"), 53);
			InetAddress address = socket.getLocalAddress();
			if (!address.isAnyLocalAddress() && !address.isLoopbackAddress()) {
				return address.getHostAddress();
			}
		} catch (IOException e) {
			// No default route (e.g. offline), look at the interfaces instead
		}

		try {
			for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
					continue;
				}
				for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
					if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
						return address.getHostAddress();
					}
				}
			}
		} catch (SocketException e) {
			// No network info available
		}
		return null;
	}

	private void openInBrowser(String url) {
		if (url != null) {
			getHostServices().showDocument(url);
		}
	}

	private void quit() {
		int exitCode = context != null ? SpringApplication.exit(context) : 0;
		System.exit(exitCode);
	}
}
