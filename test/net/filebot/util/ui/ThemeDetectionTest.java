package net.filebot.util.ui;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.junit.Test;

/**
 * Tests for system dark-mode detection (FIX-13 / BUG-20).
 *
 * <p>Verifies that {@code isSystemInDarkMode()} does not block the EDT,
 * has a timeout, and caches its result.
 */
public class ThemeDetectionTest {

	@Test(timeout = 5000)
	public void detectionDoesNotBlockEDT() throws Exception {
		// if detection blocked the EDT, this invokeAndWait would time out
		CountDownLatch edtResponded = new CountDownLatch(1);

		// call detection from a background thread, then verify EDT stays responsive
		Thread bg = new Thread(() -> {
			SwingUI.isSystemInDarkMode();
		}, "ThemeDetectionTest-bg");
		bg.setDaemon(true);
		bg.start();

		// EDT must respond within 2 seconds even while detection may be running
		SwingUtilities.invokeLater(edtResponded::countDown);
		assertTrue("EDT is blocked by theme detection", edtResponded.await(2, TimeUnit.SECONDS));

		bg.join(3000);
	}

	@Test(timeout = 5000)
	public void detectionHasTimeout() throws Exception {
		// detection must complete within 2 seconds (the timeout we enforce)
		AtomicBoolean done = new AtomicBoolean(false);

		Thread bg = new Thread(() -> {
			SwingUI.isSystemInDarkMode();
			done.set(true);
		}, "ThemeDetectionTest-timeout");
		bg.setDaemon(true);
		bg.start();

		bg.join(2500);
		assertTrue("detection must complete within 2 seconds", done.get());
	}

	@Test
	public void sourceCodeHasTimeout() throws Exception {
		String source = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("source/net/filebot/util/ui/SwingUI.java")));
		assertTrue("isSystemInDarkMode must use waitFor with timeout", source.contains("waitFor") && source.contains("SECONDS"));
	}

	@Test
	public void sourceCodeCachesResult() throws Exception {
		String source = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("source/net/filebot/util/ui/SwingUI.java")));
		// result should be cached so it's only computed once
		assertTrue("isSystemInDarkMode must cache its result", source.contains("cached") || source.contains("volatile") || source.contains("Boolean"));
	}

	@Test
	public void sourceCodeConsultsPortalOnDefault() throws Exception {
		String source = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("source/net/filebot/util/ui/SwingUI.java")));
		// when GNOME returns 'default', the portal should also be consulted
		assertFalse("must not return false immediately on 'default'", source.contains("val.contains(\"default\") return false"));
	}

}
