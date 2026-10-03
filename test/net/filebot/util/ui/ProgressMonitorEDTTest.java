package net.filebot.util.ui;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.junit.Test;

import net.filebot.util.ui.ProgressMonitor.ProgressWorker;
import net.filebot.util.ui.ProgressMonitor.TaskResult;

/**
 * Tests that {@link ProgressMonitor.runTask} does not block the EDT (FIX-10 / BUG-04).
 *
 * <p>Watchdog: if the EDT were blocked by {@code get()}, the {@code invokeAndWait}
 * call below would time out and fail the test.
 */
public class ProgressMonitorEDTTest {

	@Test(timeout = 10000)
	public void edtRemainsResponsiveWhileTaskRuns() throws Exception {
		CountDownLatch taskDone = new CountDownLatch(1);
		AtomicBoolean callbackOnEDT = new AtomicBoolean(false);

		// slow worker (500ms)
		ProgressWorker<String> worker = (message, progress, cancelled) -> {
			Thread.sleep(500);
			return "done";
		};

		// start task — must NOT block the EDT
		ProgressMonitor.runTask("Test", "Testing EDT", worker, result -> {
			callbackOnEDT.set(SwingUtilities.isEventDispatchThread());
			taskDone.countDown();
		});

		// watchdog: EDT must respond within 2 seconds even while task is running
		CountDownLatch edtResponded = new CountDownLatch(1);
		SwingUtilities.invokeLater(edtResponded::countDown);
		assertTrue("EDT is blocked — ProgressMonitor.runTask must not block the EDT", edtResponded.await(2, TimeUnit.SECONDS));

		// wait for task completion callback
		assertTrue("task did not complete", taskDone.await(5, TimeUnit.SECONDS));
		assertTrue("completion callback must run on EDT", callbackOnEDT.get());
	}

	@Test(timeout = 10000)
	public void completionCallbackReceivesResult() throws Exception {
		CountDownLatch done = new CountDownLatch(1);
		TaskResult<String>[] captured = new TaskResult[1];

		ProgressWorker<String> worker = (message, progress, cancelled) -> "hello";

		ProgressMonitor.runTask("Test", "Testing result", worker, result -> {
			captured[0] = result;
			done.countDown();
		});

		assertTrue(done.await(5, TimeUnit.SECONDS));
		assertNotNull(captured[0]);
		assertFalse(captured[0].isError());
		assertEquals("hello", captured[0].getValue());
	}

	@Test(timeout = 10000)
	public void completionCallbackReceivesException() throws Exception {
		CountDownLatch done = new CountDownLatch(1);
		TaskResult<String>[] captured = new TaskResult[1];

		ProgressWorker<String> worker = (message, progress, cancelled) -> {
			throw new IllegalStateException("test error");
		};

		ProgressMonitor.runTask("Test", "Testing error", worker, result -> {
			captured[0] = result;
			done.countDown();
		});

		assertTrue(done.await(5, TimeUnit.SECONDS));
		assertNotNull(captured[0]);
		assertTrue(captured[0].isError());
		assertEquals("test error", captured[0].getError().getMessage());
	}

}
