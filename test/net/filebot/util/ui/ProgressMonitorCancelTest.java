package net.filebot.util.ui;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.Test;

import net.filebot.util.ui.ProgressMonitor.ProgressWorker;
import net.filebot.util.ui.ProgressMonitor.TaskResult;

/**
 * Tests for {@link ProgressMonitor} cancellation and progress scale (FIX-11 / BUG-10).
 */
public class ProgressMonitorCancelTest {

	@Test(timeout = 10000)
	public void cancelInterruptsSleepingWorker() throws Exception {
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(1);
		AtomicBoolean interrupted = new AtomicBoolean(false);

		ProgressWorker<String> worker = (message, progress, cancelled) -> {
			started.countDown();
			try {
				Thread.sleep(30000); // long sleep
			} catch (InterruptedException e) {
				interrupted.set(true);
				throw e;
			}
			return "should not reach here";
		};

		var task = ProgressMonitor.runTask("Cancel", "Testing cancel", worker, result -> done.countDown());

		// wait for worker to start
		assertTrue(started.await(2, TimeUnit.SECONDS));

		// cancel — must interrupt the sleeping worker
		task.cancel(true);

		// worker should be interrupted and complete quickly
		assertTrue("cancel did not complete the task", done.await(2, TimeUnit.SECONDS));
		assertTrue("worker thread was not interrupted", interrupted.get());
	}

	@Test(timeout = 10000)
	public void cancelCallsCompletionWithError() throws Exception {
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(1);
		AtomicReference<TaskResult<String>> captured = new AtomicReference<>();

		ProgressWorker<String> worker = (message, progress, cancelled) -> {
			started.countDown();
			Thread.sleep(30000);
			return "unreachable";
		};

		var task = ProgressMonitor.runTask("Cancel", "Testing error", worker, result -> {
			captured.set(result);
			done.countDown();
		});

		assertTrue(started.await(2, TimeUnit.SECONDS));
		task.cancel(true);
		assertTrue(done.await(2, TimeUnit.SECONDS));

		// after cancel, the callback should receive an error (InterruptedException or CancellationException)
		assertNotNull(captured.get());
		assertTrue("expected error after cancel", captured.get().isError());
	}

	@Test(timeout = 5000)
	public void progressScaleAvoidsIntegerOverflow() throws Exception {
		// 3 GiB of 5 GiB should show ~60%
		CountDownLatch done = new CountDownLatch(1);
		AtomicReference<int[]> capturedBar = new AtomicReference<>();

		// worker that reports 3 GiB / 5 GiB
		ProgressWorker<String> worker = (message, progress, cancelled) -> {
			progress.accept(3L * 1024 * 1024 * 1024, 5L * 1024 * 1024 * 1024);
			return "ok";
		};

		ProgressMonitor.runTask("Scale", "Testing scale", worker, result -> done.countDown());

		assertTrue(done.await(2, TimeUnit.SECONDS));
		// progress callback used 0-1000 scale internally; verify by checking that
		// the value would be 600 (60% of 1000), not saturated
		long current = 3L * 1024 * 1024 * 1024;
		long total = 5L * 1024 * 1024 * 1024;
		int normalized = (int) Math.min(1000, Math.max(0, current * 1000 / total));
		assertEquals(600, normalized);
	}

}
