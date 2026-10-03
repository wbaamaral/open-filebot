package net.filebot.util.ui;

import static net.filebot.Logging.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class ProgressMonitor<T> {

	public static <T> FutureTask<T> runTask(String title, String header, ProgressWorker<T> worker) {
		return runTask(title, header, worker, null);
	}

	/**
	 * Run a task in a background thread with a progress dialog. The {@code onComplete}
	 * callback is invoked on the EDT when the task finishes (with the result or exception).
	 * Do <b>not</b> call {@code get()} on the EDT — use {@code onComplete} instead.
	 */
	public static <T> FutureTask<T> runTask(String title, String header, ProgressWorker<T> worker, Consumer<TaskResult<T>> onComplete) {
		SwingProgressTask<T> task = new SwingProgressTask<>(worker, onComplete);

		SwingUtilities.invokeLater(() -> {
			SwingProgressDialog dialog = new SwingProgressDialog(title, header, task);
			task.setDialog(dialog);
			dialog.startTimer();
		});

		Thread thread = new Thread(task, "ProgressMonitor-" + title);
		thread.setDaemon(true);
		thread.start();

		return task;
	}

	/** Result of a background task: either a value or an exception. */
	public static class TaskResult<T> {
		private final T value;
		private final Throwable error;

		TaskResult(T value, Throwable error) {
			this.value = value;
			this.error = error;
		}

		public T getValue() {
			return value;
		}

		public Throwable getError() {
			return error;
		}

		public boolean isError() {
			return error != null;
		}
	}

	@FunctionalInterface
	public interface ProgressWorker<T> {
		T call(Consumer<String> message, BiConsumer<Long, Long> progress, Supplier<Boolean> cancelled) throws Exception;
	}

	private static class SwingProgressTask<T> extends FutureTask<T> {

		private final ProgressWorker<T> worker;
		private final Consumer<TaskResult<T>> onComplete;
		private volatile SwingProgressDialog dialog;
		private volatile Thread workerThread;

		public SwingProgressTask(ProgressWorker<T> worker, Consumer<TaskResult<T>> onComplete) {
			super(() -> null);
			this.worker = worker;
			this.onComplete = onComplete;
		}

		public void setDialog(SwingProgressDialog dialog) {
			this.dialog = dialog;
		}

		@Override
		public void run() {
			workerThread = Thread.currentThread();
			T result = null;
			Throwable error = null;
			try {
				result = worker.call(
					msg -> {
						if (dialog != null) dialog.updateMessage(msg);
					},
					(current, total) -> {
						if (dialog != null) dialog.updateProgress(current, total);
					},
					this::isCancelled
				);
				set(result);
			} catch (Throwable t) {
				error = t;
				setException(t);
			} finally {
				workerThread = null;
				if (dialog != null) {
					dialog.close();
				}
				if (onComplete != null) {
					TaskResult<T> taskResult = new TaskResult<T>(result, error);
					SwingUtilities.invokeLater(() -> onComplete.accept(taskResult));
				}
			}
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			boolean cancelled = super.cancel(mayInterruptIfRunning);
			// FutureTask.cancel() cannot interrupt because we override run();
			// interrupt the worker thread directly
			if (cancelled && mayInterruptIfRunning && workerThread != null) {
				workerThread.interrupt();
			}
			if (dialog != null) {
				dialog.close();
			}
			return cancelled;
		}
	}

	private static class SwingProgressDialog {

		private final String title;
		private final String header;
		private final FutureTask<?> task;

		private JDialog dialog;
		private JLabel messageLabel;
		private JProgressBar progressBar;
		private Timer displayTimer;
		private volatile boolean closed = false;

		public SwingProgressDialog(String title, String header, FutureTask<?> task) {
			this.title = title;
			this.header = header;
			this.task = task;
		}

		public synchronized void startTimer() {
			if (closed || task.isDone()) {
				return;
			}
			// Show progress monitor only if operation takes more than 500ms
			displayTimer = new Timer(500, e -> {
				synchronized (SwingProgressDialog.this) {
					if (!closed && !task.isDone()) {
						createAndShowDialog();
					}
				}
			});
			displayTimer.setRepeats(false);
			displayTimer.start();
		}

		private void createAndShowDialog() {
			Window owner = null;
			for (Frame frame : Frame.getFrames()) {
				if (frame.isVisible() && frame.isActive()) {
					owner = frame;
					break;
				}
			}

			dialog = new JDialog(owner, title, java.awt.Dialog.ModalityType.MODELESS);
			dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			dialog.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					task.cancel(true);
					close();
				}
			});

			JPanel mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
			mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

			JLabel headerLabel = new JLabel(header);
			Appearance.Typography.HEADING.apply(headerLabel);
			headerLabel.setAlignmentX(JPanel.LEFT_ALIGNMENT);

			messageLabel = new JLabel(" ");
			messageLabel.setAlignmentX(JPanel.LEFT_ALIGNMENT);

			progressBar = new JProgressBar();
			progressBar.setIndeterminate(true);
			progressBar.setPreferredSize(new Dimension(360, 20));
			progressBar.setAlignmentX(JPanel.LEFT_ALIGNMENT);

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
			buttonPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(e -> {
				task.cancel(true);
				close();
			});
			buttonPanel.add(cancelButton);

			mainPanel.add(headerLabel);
			mainPanel.add(Box.createVerticalStrut(8));
			mainPanel.add(messageLabel);
			mainPanel.add(Box.createVerticalStrut(10));
			mainPanel.add(progressBar);
			mainPanel.add(Box.createVerticalStrut(12));
			mainPanel.add(buttonPanel);

			dialog.getContentPane().add(mainPanel, BorderLayout.CENTER);
			dialog.pack();
			dialog.setResizable(false);
			dialog.setLocationRelativeTo(owner);
			dialog.setAlwaysOnTop(true);
			dialog.setVisible(true);
		}

		public void updateMessage(String message) {
			SwingUtilities.invokeLater(() -> {
				if (messageLabel != null) {
					messageLabel.setText(message != null ? message : " ");
				}
			});
		}

		public void updateProgress(long current, long total) {
			SwingUtilities.invokeLater(() -> {
				if (progressBar != null) {
					if (total > 0) {
						// normalize to 0–1000 to avoid Integer.MAX_VALUE saturation (BUG-10)
						progressBar.setIndeterminate(false);
						progressBar.setMinimum(0);
						progressBar.setMaximum(1000);
						progressBar.setValue((int) Math.min(1000, Math.max(0, current * 1000 / total)));
					} else {
						progressBar.setIndeterminate(true);
					}
				}
			});
		}

		public synchronized void close() {
			closed = true;
			if (displayTimer != null && displayTimer.isRunning()) {
				displayTimer.stop();
			}
			SwingUtilities.invokeLater(() -> {
				if (dialog != null) {
					dialog.setVisible(false);
					dialog.dispose();
					dialog = null;
				}
			});
		}
	}

	private ProgressMonitor() {
		throw new UnsupportedOperationException();
	}

}
