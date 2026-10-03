package net.filebot.cli;

/**
 * Script bundle does not match the expected checksum and must not be executed.
 */
public class ScriptIntegrityException extends CmdlineException {

	public ScriptIntegrityException(String message) {
		super(message);
	}

}
