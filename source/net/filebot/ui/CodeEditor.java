package net.filebot.ui;

import static net.filebot.Logging.*;

import java.awt.Color;
import java.io.InputStream;
import java.util.logging.Level;

import javax.swing.UIManager;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;

import net.filebot.util.ui.Appearance;
import net.filebot.util.ui.SwingUI;
import net.filebot.util.ui.Tokens;

/**
 * Groovy expression editor that follows the current theme and code font.
 */
public class CodeEditor extends RSyntaxTextArea {

	private boolean invalid = false;

	public CodeEditor() {
		super(new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_GROOVY), "", 1, 80);

		setAntiAliasingEnabled(true);
		setAnimateBracketMatching(false);
		setAutoIndentEnabled(false);
		setClearWhitespaceLinesEnabled(false);
		setBracketMatchingEnabled(true);
		setCloseCurlyBraces(false);
		setCodeFoldingEnabled(false);
		setHyperlinksEnabled(false);
		setUseFocusableTips(false);
		setHighlightCurrentLine(false);
		setLineWrap(false);
		setPaintMarkOccurrencesBorder(false);
		setPaintTabLines(false);
		setMarkOccurrences(false);

		applyAppearance();
	}

	@Override
	public void updateUI() {
		super.updateUI();

		// updateUI is called by the super constructor before this instance is fully initialized
		if (getDocument() instanceof RSyntaxDocument) {
			applyAppearance();
		}
	}

	/**
	 * Highlight the whole expression if it can't be compiled.
	 */
	public void setInvalid(boolean invalid) {
		this.invalid = invalid;
		setForeground(invalid ? getErrorColor() : Tokens.getColor(Tokens.TEXT_COLOR));
	}

	private void applyAppearance() {
		String theme = SwingUI.isDarkTheme() ? "dark.xml" : "default.xml";
		try (InputStream in = RSyntaxTextArea.class.getResourceAsStream("themes/" + theme)) {
			Theme.load(in).apply(this);
		} catch (Exception e) {
			debug.log(Level.WARNING, "Failed to apply editor theme: " + theme, e);
		}

		// use editor colors and code font of the application theme
		setBackground(UIManager.getColor("TextArea.background"));
		setCaretColor(UIManager.getColor("TextArea.caretForeground"));
		setFont(Appearance.getCodeFont(0));
		setInvalid(invalid);
	}

	private static Color getErrorColor() {
		return Tokens.getColor(Tokens.DANGER_COLOR);
	}

}
