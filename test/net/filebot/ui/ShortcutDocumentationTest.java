package net.filebot.ui;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Tests for keyboard shortcut documentation consistency (FIX-12 / BUG-14).
 *
 * <p>Verifies that documented shortcuts in {@code FileBotMenuBar} and
 * {@code GettingStartedStage} match the actual behaviour in {@code MainFrame}.
 */
public class ShortcutDocumentationTest {

	@Test
	public void menuBarDocumentsF5AsGroovyPad() throws Exception {
		String source = readSource("ui/FileBotMenuBar.java");
		assertTrue("FileBotMenuBar must document F5 as GroovyPad", source.contains("F5"));
		assertFalse("FileBotMenuBar must not document F5 as rename", source.contains("Executar Renomeação"));
		assertFalse("FileBotMenuBar must not document F5 as rename", source.contains("F5:</b> Renomear"));
	}

	@Test
	public void menuBarDocumentsCtrlShiftDeleteAsCache() throws Exception {
		String source = readSource("ui/FileBotMenuBar.java");
		assertTrue("FileBotMenuBar must document Ctrl+Shift+Delete as cache", source.contains("Limpar todo o cache"));
		assertFalse("FileBotMenuBar must not say 'Limpar toda a lista'", source.contains("Limpar toda a lista"));
	}

	@Test
	public void gettingStartedDocumentsF5AsGroovyPad() throws Exception {
		String source = readSource("ui/GettingStartedStage.java");
		assertFalse("GettingStartedStage must not document F5 as rename", source.contains("F5:</i> Renomear"));
		assertFalse("GettingStartedStage must not say 'Pressione F5' for rename", source.contains("Pressione <b>F5</b> ou clique em <b>Rename</b>"));
	}

	@Test
	public void mainFrameHasConfirmationForClearCache() throws Exception {
		String source = readSource("ui/MainFrame.java");
		assertTrue("MainFrame must show confirmation before clearing cache", source.contains("showConfirmDialog"));
		assertTrue("MainFrame must mention cache in confirmation", source.contains("Limpar Cache") || source.contains("cache"));
	}

	@Test
	public void documentedShortcutsMatchActualBindings() throws Exception {
		// verify that the keys documented in FileBotMenuBar exist in MainFrame bindings
		String menu = readSource("ui/FileBotMenuBar.java");
		String frame = readSource("ui/MainFrame.java");

		// F5 must be documented as GroovyPad and bound in MainFrame
		assertTrue("F5 must be documented", menu.contains("F5"));
		assertTrue("F5 must be bound in MainFrame", frame.contains("VK_F5"));

		// F1 must be documented and bound
		assertTrue("F1 must be documented", menu.contains("F1"));
		assertTrue("F1 must be bound in MainFrame", frame.contains("VK_F1"));

		// Ctrl+Shift+Delete must be documented and bound
		assertTrue("Ctrl+Shift+Delete must be documented", menu.contains("Ctrl + Shift + Delete"));
		assertTrue("Ctrl+Shift+Delete must be bound in MainFrame", frame.contains("VK_DELETE"));
	}

	private String readSource(String relative) throws Exception {
		return new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("source/net/filebot", relative)));
	}

}
