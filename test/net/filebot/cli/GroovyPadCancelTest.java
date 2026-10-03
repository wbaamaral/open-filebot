package net.filebot.cli;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

/**
 * Tests for GroovyPad script cancellation (FIX-13 / BUG-17).
 *
 * <p>Verifies that cancelling a running script terminates it via interruption,
 * without using the deprecated {@code Thread.stop()} (which throws
 * {@code UnsupportedOperationException} on Java 20+).
 */
public class GroovyPadCancelTest {

	@Test(timeout = 5000)
	public void interruptTerminatesSleepingScript() throws Exception {
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch finished = new CountDownLatch(1);
		AtomicBoolean interrupted = new AtomicBoolean(false);

		Thread worker = new Thread(() -> {
			started.countDown();
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				interrupted.set(true);
			} finally {
				finished.countDown();
			}
		}, "GroovyPadCancelTest");
		worker.setDaemon(true);
		worker.start();

		assertTrue(started.await(1, TimeUnit.SECONDS));

		// cancel via interruption (the new GroovyPad approach)
		worker.interrupt();

		assertTrue("worker did not terminate after interrupt", finished.await(2, TimeUnit.SECONDS));
		assertTrue("worker was not interrupted", interrupted.get());
	}

	@Test
	public void sourceCodeHasNoThreadStop() throws Exception {
		String source = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("source/net/filebot/cli/GroovyPad.java")));
		assertFalse("GroovyPad must not use Thread.stop()", source.contains(".stop()"));
		assertFalse("GroovyPad must not use @SuppressWarnings(\"deprecation\") for Thread.stop", source.contains("@SuppressWarnings(\"deprecation\")"));
	}

	@Test
	public void sourceCodeUsesInterruption() throws Exception {
		String source = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("source/net/filebot/cli/GroovyPad.java")));
		assertTrue("GroovyPad cancel must use interrupt()", source.contains(".interrupt()"));
	}

}
