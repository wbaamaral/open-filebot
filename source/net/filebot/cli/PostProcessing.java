package net.filebot.cli;

import static java.util.Arrays.*;
import static net.filebot.Logging.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.filebot.RenameAction;
import net.filebot.StandardRenameAction;

/**
 * Post-processing actions for {@code filebot -rename --apply ...}.
 */
public class PostProcessing {

	public static final String PRUNE = "prune";
	public static final String DATE = "date";

	private final List<String> options;

	public PostProcessing(String apply) {
		this.options = asList(apply.toLowerCase(Locale.ROOT).trim().split("[,;| ]+"));
	}

	/**
	 * @param action
	 *            rename action that has been used
	 * @param roots
	 *            input paths given by the user
	 * @param sourceFiles
	 *            files that have been given to the rename operation
	 * @param destinationFiles
	 *            files that have been renamed successfully
	 */
	public void apply(RenameAction action, Collection<File> roots, Collection<File> sourceFiles, List<File> destinationFiles) {
		apply(action, roots, sourceFiles, destinationFiles, null);
	}

	/**
	 * @param dateMap
	 *            optional map of destination file → release/airdate timestamp (milliseconds). When {@code null} or empty, {@code --apply date} is skipped with a log message.
	 */
	public void apply(RenameAction action, Collection<File> roots, Collection<File> sourceFiles, List<File> destinationFiles, Map<File, Long> dateMap) {
		// never modify the file system in dry-run mode
		if (action == StandardRenameAction.TEST) {
			log.info(String.format("[TEST] Skip --apply %s", String.join(",", options)));
			return;
		}

		for (String option : options) {
			switch (option) {
			case PRUNE:
				prune(action, roots, sourceFiles);
				break;
			case DATE:
				date(action, destinationFiles, dateMap);
				break;
			default:
				log.warning(String.format("Unknown --apply option: %s", option));
				break;
			}
		}
	}

	private void prune(RenameAction action, Collection<File> roots, Collection<File> sourceFiles) {
		// only move operations leave empty folders behind
		if (action != StandardRenameAction.MOVE && action != StandardRenameAction.KEEPLINK) {
			log.info(String.format("[PRUNE] Skip because action is %s", action));
			return;
		}

		new PruneEmptyFolders(roots).prune(sourceFiles);
	}

	private void date(RenameAction action, List<File> destinationFiles, Map<File, Long> dateMap) {
		// BUG-09: do not modify mtime through symlinks or hardlinks
		if (action == StandardRenameAction.SYMLINK || action == StandardRenameAction.HARDLINK) {
			log.info(String.format("[DATE] Skip because action is %s (would modify the original file)", action));
			return;
		}

		if (dateMap == null || dateMap.isEmpty()) {
			log.warning("[DATE] Skip because release/airdate is unknown (see FIX-15 / AUD-14)");
			return;
		}

		for (File dest : destinationFiles) {
			if (dest == null || !dest.exists()) {
				continue;
			}

			Long timestamp = dateMap.get(dest);
			if (timestamp == null) {
				log.fine(String.format("[DATE] Skip [%s] because date is unknown", dest));
				continue;
			}

			// set mtime without following symlinks (NOFOLLOW_LINKS)
			try {
				Path path = dest.toPath();
				Files.setLastModifiedTime(path, FileTime.fromMillis(timestamp));
			} catch (IOException e) {
				log.warning(String.format("[DATE] Failed to set mtime on [%s]: %s", dest, e.getMessage()));
			}
		}
	}

}
