package net.filebot.archive;

import static java.nio.charset.StandardCharsets.*;
import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;
import static net.filebot.util.RegularExpressions.*;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.filebot.util.ByteBufferOutputStream;
import net.filebot.vfs.FileInfo;
import net.filebot.vfs.SimpleFileInfo;

public class SevenZipExecutable implements ArchiveExtractor {

	private File archive;

	public SevenZipExecutable(File file) throws Exception {
		if (!file.exists()) {
			throw new FileNotFoundException(file.getAbsolutePath());
		}

		this.archive = file.getCanonicalFile();
	}

	protected String get7zCommand() {
		// use 7z executable path as specified by the cmdline or default to "7z" and let the shell figure it out
		return System.getProperty("net.filebot.Archive.7z", "7z");
	}

	protected CharSequence execute(String... command) throws IOException {
		Process process = new ProcessBuilder(command).redirectError(Redirect.INHERIT).start();

		try (ByteBufferOutputStream bb = new ByteBufferOutputStream(8 * 1024)) {
			bb.transferFully(process.getInputStream());

			int returnCode = process.waitFor();
			String output = UTF_8.decode(bb.getByteBuffer()).toString();

			// DEBUG
			debug.fine(format("Execute: %s", asList(command)));
			debug.finest(output);

			if (returnCode == 0) {
				return output;
			} else {
				throw new IOException(String.format("%s failed with exit code %d: %s", get7zCommand(), returnCode, SPACE.matcher(output).replaceAll(" ").trim()));
			}
		} catch (InterruptedException e) {
			throw new IOException(String.format("%s timed out", get7zCommand()), e);
		}
	}

	@Override
	public List<FileInfo> listFiles() throws IOException {
		// e.g. 7z l -y archive.7z
		CharSequence output = execute(get7zCommand(), "l", "-slt", "-y", "--", archive.getPath());

		List<FileInfo> paths = new ArrayList<FileInfo>();

		String path = null;
		long size = -1;

		for (String line : NEWLINE.split(output)) {
			int split = line.indexOf(" = ");

			// ignore empty lines
			if (split < 0) {
				continue;
			}

			String key = line.substring(0, split);
			String value = line.substring(split + 3, line.length());

			// ignore empty lines
			if (key.isEmpty() || value.isEmpty()) {
				continue;
			}

			if ("Path".equals(key)) {
				path = value;
			} else if ("Size".equals(key)) {
				size = Long.parseLong(value);
			}

			if (path != null && size >= 0) {
				paths.add(new SimpleFileInfo(path, size));

				path = null;
				size = -1;
			}
		}

		return paths;
	}

	@Override
	public void extract(File outputDir) throws IOException {
		// never trust the 7z executable to sanitize archive entry paths (e.g. ../../.bashrc)
		if (!listFiles().stream().allMatch(f -> FileMapper.isSafeEntryPath(f.getPath()))) {
			extract(outputDir, f -> true);
			return;
		}

		// e.g. 7z x -y -aos -o/path/to/output -- archive.7z
		execute(get7zCommand(), "x", "-y", "-aos", "-o" + outputDir.getCanonicalPath(), "--", archive.getPath());
	}

	@Override
	public void extract(File outputDir, FileFilter filter) throws IOException {
		// e.g. 7z x -y -aos -o/path/to/output -- archive.7z file.txt image.png info.nfo
		Stream<String> command = Stream.of(get7zCommand(), "x", "-y", "-aos", "-o" + outputDir.getCanonicalPath(), "--", archive.getPath());
		List<String> selection = listFiles().stream().filter(f -> {
			if (!FileMapper.isSafeEntryPath(f.getPath())) {
				log.warning(message("Ignore illegal archive entry path", f.getPath()));
				return false;
			}
			return filter.accept(f.toFile());
		}).map(f -> f.getPath()).collect(toList());

		// 7z extracts all files if no files are selected
		if (selection.isEmpty()) {
			return;
		}

		execute(Stream.concat(command, selection.stream()).toArray(String[]::new));
	}

}
