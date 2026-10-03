package net.filebot;

import static net.filebot.Logging.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import net.filebot.History.Element;
import net.filebot.History.HistoryFormatException;

public class HistorySpooler {

	private static final HistorySpooler instance = new HistorySpooler(ApplicationFolder.AppData.resolve("history.xml"));

	public static HistorySpooler getInstance() {
		return instance;
	}

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(HistorySpooler.getInstance()::commit, "HistorySpoolerShutdownHook")); // commit session history on shutdown
	}

	private final Path persistentHistoryFile;
	private final Path lockFile;
	private final Path tempFile;

	private int sessionHistoryTotalSize = 0;
	private int persistentHistoryTotalSize = -1;
	private boolean persistentHistoryEnabled = true;

	private final History sessionHistory = new History();

	protected HistorySpooler(File persistentHistoryFile) {
		this.persistentHistoryFile = persistentHistoryFile.toPath();
		this.lockFile = this.persistentHistoryFile.resolveSibling(this.persistentHistoryFile.getFileName() + ".lock");
		this.tempFile = this.persistentHistoryFile.resolveSibling(this.persistentHistoryFile.getFileName() + ".tmp");
	}

	/**
	 * @throws HistoryFormatException
	 *             if the persistent history file is corrupted
	 */
	public synchronized History getCompleteHistory() throws IOException {
		if (!Files.exists(persistentHistoryFile)) {
			return new History(sessionHistory.sequences());
		}

		try (HistoryLock lock = lock()) {
			History history = read();
			history.addAll(sessionHistory.sequences());
			return history;
		}
	}

	public synchronized void commit() {
		if (sessionHistory.sequences().isEmpty() || !persistentHistoryEnabled) {
			return;
		}

		try (HistoryLock lock = lock()) {
			History history = new History();

			// load existing history from previous sessions
			if (Files.exists(persistentHistoryFile)) {
				try {
					history = read();
				} catch (HistoryFormatException e) {
					// never overwrite a history file we can't read, keep a backup instead
					Path backup = backup();
					log.warning(format("History file is corrupted and has been moved to [%s]: %s", backup, e.getMessage()));
				}
			}

			// write new combined history
			history.addAll(sessionHistory.sequences());
			write(history);

			sessionHistory.clear();
			persistentHistoryTotalSize = history.totalSize();
		} catch (Exception e) {
			// keep session history so that we may try again later
			log.log(Level.SEVERE, "Failed to write history file: " + persistentHistoryFile, e);
		}
	}

	private HistoryLock lock() throws IOException {
		Files.createDirectories(lockFile.getParent());

		// lock separate file so that the history file itself can be replaced atomically
		return new HistoryLock(FileChannel.open(lockFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE));
	}

	private static class HistoryLock implements AutoCloseable {

		private final FileChannel channel;

		public HistoryLock(FileChannel channel) throws IOException {
			this.channel = channel;
			try {
				channel.lock(); // released when the channel is closed
			} catch (IOException | RuntimeException e) {
				channel.close();
				throw e;
			}
		}

		@Override
		public void close() throws IOException {
			channel.close();
		}
	}

	private History read() throws IOException {
		try (InputStream in = Files.newInputStream(persistentHistoryFile)) {
			return History.importHistory(in);
		}
	}

	private Path backup() throws IOException {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
		Path backup = persistentHistoryFile.resolveSibling(persistentHistoryFile.getFileName() + ".corrupt-" + timestamp);
		return Files.move(persistentHistoryFile, backup);
	}

	private void write(History history) throws IOException {
		try {
			// write complete file first and then replace the existing file in one go
			try (OutputStream out = Files.newOutputStream(tempFile)) {
				writeHistory(history, out);
			}

			try {
				Files.move(tempFile, persistentHistoryFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tempFile, persistentHistoryFile, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}

	protected void writeHistory(History history, OutputStream out) throws IOException {
		History.exportHistory(history, out);
	}

	public synchronized void append(Map<File, File> elements) {
		append(elements.entrySet());
	}

	public synchronized void append(Iterable<Entry<File, File>> elements) {
		List<Element> sequence = new ArrayList<Element>();

		for (Entry<File, File> element : elements) {
			File k = element.getKey();
			File v = element.getValue();

			if (k != null && v != null) {
				sequence.add(new Element(k.getName(), v.getPath(), k.getParentFile()));
			}
		}

		if (sequence.size() > 0) {
			sessionHistory.add(sequence); // append to session history
			sessionHistoryTotalSize += sequence.size();
		}
	}

	public synchronized void append(History importHistory) {
		sessionHistory.merge(importHistory);
	}

	public synchronized History getSessionHistory() {
		return new History(sessionHistory.sequences());
	}

	public synchronized int getSessionHistoryTotalSize() {
		return sessionHistoryTotalSize;
	}

	public synchronized int getPersistentHistoryTotalSize() {
		return persistentHistoryTotalSize;
	}

	public synchronized void setPersistentHistoryEnabled(boolean persistentHistoryEnabled) {
		this.persistentHistoryEnabled = persistentHistoryEnabled;
	}

}
