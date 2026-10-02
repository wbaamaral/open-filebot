package net.filebot;

import static java.awt.GraphicsEnvironment.*;
import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;
import static net.filebot.Settings.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.XPathUtilities.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.kohsuke.args4j.CmdLineException;

import net.filebot.cli.ArgumentBean;
import net.filebot.cli.ArgumentProcessor;
import net.filebot.format.ExpressionFormat;
import net.filebot.platform.mac.MacAppUtilities;
import net.filebot.platform.windows.WinAppUtilities;
import net.filebot.ui.FileBotMenuBar;
import net.filebot.ui.MainFrame;
import net.filebot.ui.NotificationHandler;
import net.filebot.ui.PanelBuilder;
import net.filebot.ui.SinglePanelFrame;
import net.filebot.ui.transfer.FileTransferable;
import net.filebot.util.PreferencesMap.PreferencesEntry;
import net.filebot.util.ui.SwingEventBus;
import net.miginfocom.swing.MigLayout;

public class Main {

	public static void main(String[] argv) {
		try {
			// parse arguments
			ArgumentBean args = new ArgumentBean(argv);

			// just print help message or version string and then exit
			if (args.printHelp()) {
				log.info(String.format("%s%n%n%s", getApplicationIdentifier(), args.usage()));
				System.exit(0);
			}

			if (args.printVersion()) {
				log.info(String.join(" / ", getApplicationIdentifier(), getJavaRuntimeIdentifier(), getSystemIdentifier()));
				System.exit(0);
			}

			if (args.clearCache() || args.clearUserData()) {
				// clear persistent user preferences
				if (args.clearUserData()) {
					log.info("Reset preferences");
					Settings.forPackage(Main.class).clear();

					// restore preferences on start if empty (TODO: remove after a few releases)
					ApplicationFolder.AppData.resolve("preferences.backup.xml").delete();
				}

				// clear caches
				if (args.clearCache()) {
					// clear cache must be called manually
					if (System.console() == null) {
						log.severe("`filebot -clear-cache` has been disabled due to abuse.");
						System.exit(1);
					}

					log.info("Clear cache");
					for (File folder : getChildren(ApplicationFolder.Cache.get(), FOLDERS)) {
						log.fine("* Delete " + folder);
						delete(folder);
					}
				}

				// just clear cache and/or settings and then exit
				System.exit(0);
			}

			// make sure we can access application arguments at any time
			setApplicationArguments(args);

			// update system properties
			initializeSystemProperties(args);
			initializeLogging(args);

			// initialize this stuff before anything else
			CacheManager.getInstance();
			initializeSecurityManager();

			// initialize history spooler
			HistorySpooler.getInstance().setPersistentHistoryEnabled(useRenameHistory());

			// CLI mode => run command-line interface and then exit
			if (args.runCLI()) {
				int status = new ArgumentProcessor().run(args);
				System.exit(status);
			}

			if (isHeadless()) {
				log.info(String.format("%s / %s (headless)%n%n%s", getApplicationIdentifier(), getJavaRuntimeIdentifier(), args.usage()));
				System.exit(1);
			}

			// GUI mode => start user interface
			SwingUtilities.invokeLater(() -> {
				// restore preferences on start if empty (TODO: remove after a few releases)
				try {
					if (Preferences.userNodeForPackage(Main.class).keys().length == 0) {
						File f = ApplicationFolder.AppData.resolve("preferences.backup.xml");
						if (f.exists()) {
							log.fine("Restore user preferences: " + f);
							Settings.restore(f);
						} else {
							log.fine("No user preferences found: " + f);
						}
					}
				} catch (Exception e) {
					debug.log(Level.WARNING, "Failed to restore preferences", e);
				}

				startUserInterface(args);

				// run background tasks
				newSwingWorker(() -> onStart(args)).execute();
			});
		} catch (CmdLineException e) {
			// illegal arguments => print CLI error message
			log.severe(e::getMessage);
			System.exit(1);
		} catch (Throwable e) {
			// unexpected error => dump stack
			debug.log(Level.SEVERE, "Error during startup", e);
			System.exit(1);
		}
	}

	private static void onStart(ArgumentBean args) {
		// publish file arguments
		List<File> files = args.getFiles(false);
		if (files.size() > 0) {
			SwingEventBus.getInstance().post(new FileTransferable(files));
		}

		// JavaFX is used for ProgressMonitor and GettingStartedDialog
		try {
			initJavaFX();
		} catch (Throwable e) {
			log.log(Level.SEVERE, "Failed to initialize JavaFX. Please install JavaFX.", e);
		}
	}

	private static void startUserInterface(ArgumentBean args) {
		// initialize modern FlatLaf theme (Dark / Light / System auto-detect)
		initTheme();

		// start multi panel or single panel frame
		PanelBuilder[] panels = args.getPanelBuilders();
		JFrame frame = panels.length > 1 ? new MainFrame(panels) : new SinglePanelFrame(panels[0]);

		try {
			restoreWindowBounds(frame, Settings.forPackage(MainFrame.class)); // restore previous size and location
		} catch (Exception e) {
			frame.setLocation(120, 80); // make sure the main window is not displayed out of screen bounds
		}

		frame.addWindowListener(windowClosed(evt -> {
			evt.getWindow().setVisible(false);

			// make sure any long running operations are done now and not later on the shutdown hook thread
			HistorySpooler.getInstance().commit();

			// restore preferences on start if empty (TODO: remove after a few releases)
			Settings.store(ApplicationFolder.AppData.resolve("preferences.backup.xml"));

			System.exit(0);
		}));

		// configure main window
		if (isMacApp()) {
			// Mac specific configuration
			MacAppUtilities.initializeApplication(FileBotMenuBar.createMenuBar(), files -> SwingEventBus.getInstance().post(new FileTransferable(files)));
		} else {
			frame.setJMenuBar(FileBotMenuBar.createMenuBar());
			frame.setIconImages(ResourceManager.getApplicationIconImages());
			if (isWindowsApp()) {
				WinAppUtilities.initializeApplication();
			}
		}

		// start application
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}


	private static void restoreWindowBounds(JFrame window, Settings settings) {
		// store bounds on close
		window.addWindowListener(windowClosed(evt -> {
			// don't save window bounds if window is maximized
			if (!isMaximized(window)) {
				settings.put("window.x", String.valueOf(window.getX()));
				settings.put("window.y", String.valueOf(window.getY()));
				settings.put("window.width", String.valueOf(window.getWidth()));
				settings.put("window.height", String.valueOf(window.getHeight()));
			}
		}));

		// restore bounds
		int x = Integer.parseInt(settings.get("window.x"));
		int y = Integer.parseInt(settings.get("window.y"));
		int width = Integer.parseInt(settings.get("window.width"));
		int height = Integer.parseInt(settings.get("window.height"));
		window.setBounds(x, y, width, height);
	}

	/**
	 * Initialize default SecurityManager and grant all permissions via security policy. Initialization is required in order to run {@link ExpressionFormat} in a secure sandbox.
	 */
	private static void initializeSecurityManager() {
		try {
			// initialize security policy used by the default security manager
			// Note: on Java 24+ (JEP 486), setting Policy or SecurityManager throws UnsupportedOperationException
			Policy.setPolicy(new Policy() {

				@Override
				public boolean implies(ProtectionDomain domain, Permission permission) {
					// all permissions
					return true;
				}

				@Override
				public PermissionCollection getPermissions(CodeSource codesource) {
					// VisualVM can't connect if this method does return
					// a checked immutable PermissionCollection
					return new Permissions();
				}
			});

			// set default security manager
			System.setSecurityManager(new SecurityManager());
		} catch (UnsupportedOperationException e) {
			debug.fine("SecurityManager is permanently disabled on this Java runtime, continuing with standard security.");
		} catch (Throwable e) {
			// security manager was probably set via system property or unsupported
			debug.log(Level.WARNING, e.getMessage(), e);
		}
	}

	public static void initializeSystemProperties(ArgumentBean args) {
		System.setProperty("http.agent", String.format("%s %s", getApplicationName(), getApplicationVersion()));
		System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
		System.setProperty("sun.net.client.defaultReadTimeout", "60000");

		System.setProperty("swing.crossplatformlaf", "javax.swing.plaf.nimbus.NimbusLookAndFeel");
		System.setProperty("grape.root", ApplicationFolder.AppData.resolve("grape").getPath());
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

		if (args.unixfs) {
			System.setProperty("unixfs", "true");
		}

		if (args.disableExtendedAttributes) {
			System.setProperty("useExtendedFileAttributes", "false");
			System.setProperty("useCreationDate", "false");
		}
	}

	public static void initializeLogging(ArgumentBean args) throws IOException {
		// make sure that these folders exist
		ApplicationFolder.TemporaryFiles.get().mkdirs();
		ApplicationFolder.AppData.get().mkdirs();

		if (args.runCLI()) {
			// CLI logging settings
			log.setLevel(args.getLogLevel());
		} else {
			// GUI logging settings
			log.setLevel(Level.INFO);
			log.addHandler(new NotificationHandler(getApplicationName()));

			// log errors to file
			try {
				Handler errorLogHandler = createSimpleFileHandler(ApplicationFolder.AppData.resolve("error.log"), Level.WARNING);
				log.addHandler(errorLogHandler);
				debug.addHandler(errorLogHandler);
			} catch (Exception e) {
				log.log(Level.WARNING, "Failed to initialize error log", e);
			}
		}

		// tee stdout and stderr to log file if --log-file is set
		if (args.logFile != null) {
			Handler logFileHandler = createLogFileHandler(args.getLogFile(), args.logLock, Level.ALL);
			log.addHandler(logFileHandler);
			debug.addHandler(logFileHandler);
		}
	}

}
