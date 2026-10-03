package net.filebot.archive;

import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import net.filebot.vfs.FileInfo;

public class FileMapper implements ExtractOutProvider {

	private File outputDir;
	private boolean flatten;

	public FileMapper(File outputDir) {
		this(outputDir, false);
	};

	public FileMapper(File outputDir, boolean flatten) {
		this.outputDir = outputDir;
		this.flatten = flatten;
	};

	public File getOutputDir() {
		return outputDir;
	}

	/**
	 * @return output path for the given archive entry, or {@code null} if the archive entry path would resolve to a location outside of the output folder
	 */
	public File getOutputFile(File entry) {
		File outputFile = resolve(outputDir, flatten ? new File(entry.getName()) : entry);
		if (outputFile == null) {
			log.warning(message("Ignore illegal archive entry path", entry));
		}
		return outputFile;
	}

	@Override
	public OutputStream getStream(File entry) throws IOException {
		File outputFile = getOutputFile(entry);

		// skip archive entries that would be written outside of the output folder
		if (outputFile == null) {
			return null;
		}

		File outputFolder = outputFile.getParentFile();

		// create parent folder if necessary
		if (!outputFolder.isDirectory() && !outputFolder.mkdirs()) {
			throw new IOException("Failed to create folder: " + outputFolder);
		}

		// make sure that existing symlinks in the output folder don't lead outside of the output folder
		if (!outputFolder.toPath().toRealPath().startsWith(outputDir.toPath().toRealPath())) {
			log.warning(message("Ignore archive entry outside of output folder", entry));
			return null;
		}

		return new FileOutputStream(outputFile);
	}

	public FileFilter newPathFilter(Collection<FileInfo> selection) {
		return newPathFilter(selection.stream().map(FileInfo::getPath).collect(toSet()));
	}

	public FileFilter newPathFilter(Set<String> selection) {
		return f -> {
			File outputFile = resolve(outputDir, flatten ? new File(f.getName()) : f);
			return outputFile != null && selection.contains(outputFile.getPath());
		};
	}

	private static final Pattern DRIVE_LETTER = Pattern.compile("^[A-Za-z]:");

	/**
	 * Resolve archive entry path against the given output folder.
	 *
	 * @return output file, or {@code null} if the archive entry path is absolute or would resolve to a location outside of the output folder (e.g. {@code ../../.bashrc})
	 */
	public static File resolve(File outputDir, File entry) {
		Objects.requireNonNull(outputDir);

		// archives may use either / or \ as separator regardless of the current platform
		String path = entry.getPath().replace('\\', '/');

		// absolute paths, UNC paths and drive letters are never allowed
		if (path.isEmpty() || path.startsWith("/") || DRIVE_LETTER.matcher(path).find()) {
			return null;
		}

		Path base = outputDir.toPath().toAbsolutePath().normalize();
		Path file = base.resolve(path).normalize();

		if (file.equals(base) || !file.startsWith(base)) {
			return null;
		}

		return new File(outputDir, base.relativize(file).toString());
	}

	/**
	 * @return {@code true} if the given archive entry path is safe to extract into any folder
	 */
	public static boolean isSafeEntryPath(String path) {
		return resolve(new File("output"), new File(path)) != null;
	}

}
