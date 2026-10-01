package net.filebot.ui;

import static net.filebot.Logging.*;
import static net.filebot.Settings.*;

import java.awt.Desktop;
import java.net.URL;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class GettingStartedStage {

	public static void start() {
		SwingUtilities.invokeLater(() -> {
			try {
				String helpUrl = getEmbeddedHelpURL();
				if (helpUrl != null && !helpUrl.isEmpty() && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
					int choice = JOptionPane.showConfirmDialog(
						null,
						"Hello! Do you need help Getting Started?\n\nWould you like to open the video tutorials in your browser?",
						"FileBot - Getting Started",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.INFORMATION_MESSAGE
					);
					if (choice == JOptionPane.YES_OPTION) {
						Desktop.getDesktop().browse(java.net.URI.create(helpUrl));
					}
				}
			} catch (Throwable e) {
				debug.log(Level.WARNING, "Failed to open Getting Started help", e);
			}
		});
	}

	private GettingStartedStage() {
		throw new UnsupportedOperationException();
	}

}
