package net.filebot.ui;

import static net.filebot.ui.ThemeSwitchTest.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.AfterClass;
import org.junit.Test;

import net.filebot.util.ui.Appearance;
import net.filebot.util.ui.Appearance.Theme;
import net.filebot.util.ui.SwingUI.DragDropRowTableUI;

/**
 * Create all panels and switch theme, font and font size at runtime. Requires a display (e.g. {@code xvfb-run ant test-gui}).
 */
public class PanelThemeSwitchTest {

	@Test
	public void allPanels() throws Exception {
		assumeFalse("requires a display", GraphicsEnvironment.isHeadless());

		for (PanelBuilder builder : PanelBuilder.defaultSequence()) {
			JComponent[] panel = new JComponent[1];
			SwingUtilities.invokeAndWait(() -> panel[0] = builder.create());

			switchThemes(panel[0]);

			// custom table UI with drag and drop of rows must survive theme changes (SFV)
			for (JTable table : find(panel[0], JTable.class, new ArrayList<JTable>())) {
				if (table.getClass().getSimpleName().equals("ChecksumTable")) {
					assertTrue(table.getUI() instanceof DragDropRowTableUI);
				}
			}
		}
	}

	@AfterClass
	public static void reset() throws Exception {
		SwingUtilities.invokeAndWait(() -> Appearance.DEFAULT.withTheme(Theme.LIGHT).apply());
	}

}
