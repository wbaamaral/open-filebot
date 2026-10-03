package net.filebot.platform.xdg;

import static java.nio.charset.StandardCharsets.*;
import static net.filebot.Logging.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Trash can as specified by the freedesktop.org Trash specification (GNOME, KDE, XFCE, etc).
 *
 * Files on the same file system as the home trash are moved to {@code $XDG_DATA_HOME/Trash}. Files on other file systems are moved to {@code $topdir/.Trash-$uid} so that large files never need to be copied across file systems.
 *
 * @see <a href="https://specifications.freedesktop.org/trash-spec/latest/">Trash specification</a>
 */
public class XdgTrash {

	private final Path homeTrash;

	public XdgTrash(Path homeTrash) {
		this.homeTrash = homeTrash.toAbsolutePath().normalize();
	}

	/**
	 * Home trash of the current user. The location can be overridden via {@code -Dnet.filebot.trash.home} (e.g. for tests).
	 */
	public static XdgTrash getDefault() {
		String override = System.getProperty("net.filebot.trash.home");
		if (override != null) {
			return new XdgTrash(Paths.get(override));
		}

		// HOME and XDG_DATA_HOME of the environment (user.home may point to an application-specific folder)
		String dataHome = System.getenv("XDG_DATA_HOME");
		if (dataHome == null || dataHome.isEmpty()) {
			String home = System.getenv("HOME");
			dataHome = Paths.get(home != null ? home : System.getProperty("user.home"), ".local", "share").toString();
		}
		return new XdgTrash(Paths.get(dataHome, "Trash"));
	}

	public static boolean isSupported() {
		String os = System.getProperty("os.name", "").toLowerCase();
		return !os.startsWith("windows") && !os.startsWith("mac");
	}

	/**
	 * Move the given file or folder into the trash.
	 *
	 * @return new location of the file in the trash
	 */
	public File moveToTrash(File file) throws IOException {
		Path source = file.toPath().toAbsolutePath().normalize();

		if (Files.notExists(source, LinkOption.NOFOLLOW_LINKS)) {
			throw new IOException("File not found: " + source);
		}

		// use home trash if possible, or the trash of the file system of the file otherwise
		Path trash = homeTrash;
		Path topdir = null;

		if (!isSameFileStore(source, homeTrash)) {
			try {
				topdir = getTopDirectory(source);
				trash = createTopDirectoryTrash(topdir);
			} catch (IOException e) {
				// fall back to home trash (requires copying the file) if the file system doesn't allow a trash folder
				debug.warning(format("Use home trash for %s: %s", source, e));
				topdir = null;
				trash = homeTrash;
			}
		}

		Path files = Files.createDirectories(trash.resolve("files"));
		Path info = Files.createDirectories(trash.resolve("info"));

		// reserve a unique name by creating the info file first (as required by the specification)
		String name = source.getFileName().toString();
		for (int i = 1; i < 10000; i++) {
			String trashName = i == 1 ? name : name + '.' + i;
			Path infoFile = info.resolve(trashName + ".trashinfo");
			Path trashFile = files.resolve(trashName);

			if (Files.exists(trashFile, LinkOption.NOFOLLOW_LINKS)) {
				continue;
			}

			try (OutputStream out = Files.newOutputStream(infoFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
				out.write(getTrashInfo(topdir == null ? source.toString() : topdir.relativize(source).toString()).getBytes(UTF_8));
			} catch (FileAlreadyExistsException e) {
				continue;
			}

			try {
				move(source, trashFile);
			} catch (IOException e) {
				Files.deleteIfExists(infoFile);
				throw e;
			}

			debug.fine(format("Move to trash: %s => %s", source, trashFile));
			return trashFile.toFile();
		}

		throw new IOException("Failed to find a unique name in trash: " + name);
	}

	protected static String getTrashInfo(String path) {
		String date = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		return "[Trash Info]\nPath=" + encodePath(path) + "\nDeletionDate=" + date + "\n";
	}

	/**
	 * Percent-encode path as URI path (RFC 2396) with UTF-8 escapes for non-ASCII characters, keeping the {@code /} separators.
	 */
	protected static String encodePath(String path) {
		try {
			return new URI(null, null, path, null).toASCIIString();
		} catch (Exception e) {
			throw new IllegalArgumentException(path, e);
		}
	}

	private static void move(Path source, Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(source, target);
		}
	}

	private static boolean isSameFileStore(Path file, Path trash) throws IOException {
		// the trash folder may not exist yet
		Path existing = trash;
		while (existing != null && Files.notExists(existing)) {
			existing = existing.getParent();
		}
		if (existing == null) {
			return false;
		}

		Path parent = file.getParent() != null ? file.getParent() : file;
		return Files.getFileStore(parent).equals(Files.getFileStore(existing));
	}

	private static Path createTopDirectoryTrash(Path topdir) throws IOException {
		Path trash = topdir.resolve(".Trash-" + getUserId());
		if (Files.notExists(trash, LinkOption.NOFOLLOW_LINKS)) {
			try {
				Files.createDirectory(trash, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
			} catch (UnsupportedOperationException e) {
				Files.createDirectory(trash);
			}
		}

		// trash folder must be a real folder owned by the user
		if (!Files.isDirectory(trash, LinkOption.NOFOLLOW_LINKS) || !Files.isWritable(trash)) {
			throw new IOException("Invalid trash folder: " + trash);
		}
		return trash;
	}

	private static Path getTopDirectory(Path file) throws IOException {
		// walk up until we reach the mount point of the file system that contains the given file
		Path dir = file.getParent();
		FileStore store = Files.getFileStore(dir);
		while (dir.getParent() != null && Files.getFileStore(dir.getParent()).equals(store)) {
			dir = dir.getParent();
		}
		return dir;
	}

	private static String getUserId() {
		try {
			return String.valueOf(Files.getAttribute(Paths.get("/proc/self"), "unix:uid"));
		} catch (Exception e) {
			return String.valueOf(new com.sun.security.auth.module.UnixSystem().getUid());
		}
	}

}
