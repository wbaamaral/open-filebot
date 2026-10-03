package net.filebot;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

/**
 * Tests for JavaFX residue cleanup (FIX-22 / BUG-24).
 *
 * <p>Verifies that JavaFX init calls, misleading messages, and the
 * {@code -clear-cache} console restriction are removed.
 */
public class JavaFxResidueTest {

	@Test
	public void mainHasNoJavaFxInit() throws Exception {
		String source = read("Main.java");
		assertFalse("Main must not call initJavaFX", source.contains("initJavaFX"));
		assertFalse("Main must not have JavaFX error message", source.contains("Please install JavaFX"));
	}

	@Test
	public void swingUIHasNoJavaFxMethods() throws Exception {
		String source = read("util/ui/SwingUI.java");
		assertFalse("SwingUI must not have initJavaFX", source.contains("initJavaFX"));
		assertFalse("SwingUI must not have invokeJavaFX", source.contains("invokeJavaFX"));
	}

	@Test
	public void userFilesHasNoJavaFxChooser() throws Exception {
		String source = read("UserFiles.java");
		assertFalse("UserFiles must not have JavaFX FileChooser", source.contains("JavaFX {"));
		assertFalse("UserFiles must not reference JavaFX chooser", source.contains("FileChooser.JavaFX"));
	}

	@Test
	public void clearCacheWorksWithoutConsole() throws Exception {
		String source = read("Main.java");
		assertFalse("must not block -clear-cache without console", source.contains("disabled due to abuse"));
	}

	@Test
	public void noJavaFxImportsRemain() throws Exception {
		String source = read("Main.java") + read("util/ui/SwingUI.java") + read("UserFiles.java");
		assertFalse("must not import JavaFX", source.contains("import javafx."));
	}

	private String read(String relative) throws Exception {
		return new String(Files.readAllBytes(Path.of("source/net/filebot", relative)));
	}

}
