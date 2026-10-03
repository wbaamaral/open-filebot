package net.filebot.cli;

import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;
import static net.filebot.util.FileUtilities.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Delete folders that have been left empty after files have been moved away.
 *
 * Only the parent folders of files that have actually been moved are considered, walking up towards the input folder that contains them. Input folders themselves are never deleted, folders are never searched for other empty folders, and symlinks are never followed. Folders that only contain thumbnail stores (e.g. Thumbs.db or .DS_Store) are considered empty.
 */
public class PruneEmptyFolders {

	private final List<Path> roots;

	/**
	 * @param roots
	 *            input paths given by the user; only folders inside input folders may be deleted
	 */
	public PruneEmptyFolders(Collection<File> roots) {
		this.roots = roots.stream().map(f -> f.toPath().toAbsolutePath().normalize()).filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)).collect(toList());
	}

	/**
	 * @param movedFiles
	 *            original paths of files that have been moved away
	 * @return deleted folders
	 */
	public List<File> prune(Collection<File> movedFiles) {
		List<File> deleted = new ArrayList<File>();

		for (Path folder : getCandidateFolders(movedFiles)) {
			Path root = getRoot(folder).orElse(null);

			// walk up until we reach the input folder or a folder that is not empty
			for (Path f = folder; root != null && f != null && !f.equals(root) && f.startsWith(root); f = f.getParent()) {
				try {
					if (!isEmptyFolder(f)) {
						break;
					}

					deleteEmptyFolder(f);
					deleted.add(f.toFile());
				} catch (IOException e) {
					log.warning(message("[PRUNE] Failed to delete folder", f, e));
					break;
				}
			}
		}

		return deleted;
	}

	private Set<Path> getCandidateFolders(Collection<File> movedFiles) {
		// process deepest folders first so that parent folders are checked after their children
		return movedFiles.stream().map(f -> f.toPath().toAbsolutePath().normalize()).filter(p -> Files.notExists(p, LinkOption.NOFOLLOW_LINKS)).map(Path::getParent).filter(p -> p != null).sorted(comparingInt(Path::getNameCount).reversed()).collect(toCollection(LinkedHashSet::new));
	}

	private Optional<Path> getRoot(Path folder) {
		// select the most specific input folder that contains the given folder
		return roots.stream().filter(folder::startsWith).max(comparingInt(Path::getNameCount));
	}

	protected static boolean isEmptyFolder(Path folder) throws IOException {
		// never follow symlinks
		if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) {
			return false;
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
			for (Path it : stream) {
				if (!isThumbnailStore(it.toFile()) || !Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS)) {
					return false;
				}
			}
		}

		return true;
	}

	private static void deleteEmptyFolder(Path folder) throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
			for (Path it : stream) {
				log.fine(message("[PRUNE] Delete", it));
				Files.delete(it);
			}
		}

		log.info(message("[PRUNE] Delete empty folder", folder));
		Files.delete(folder);
	}

}
