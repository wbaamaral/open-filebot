package net.filebot.ui;

import static org.junit.Assert.*;

import java.awt.Component;
import java.awt.Container;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.junit.AfterClass;
import org.junit.Test;

import net.filebot.util.ui.Appearance;
import net.filebot.util.ui.Appearance.FontFamily;
import net.filebot.util.ui.Appearance.Theme;

/**
 * Switching theme, font or font size at runtime must not break components with custom UI delegates (see Appearance dialog).
 */
public class ThemeSwitchTest {

	static final Appearance[] SEQUENCE = { new Appearance(Theme.LIGHT, FontFamily.EMBEDDED, 14), new Appearance(Theme.DARK, FontFamily.EMBEDDED, 18), new Appearance(Theme.NIMBUS, FontFamily.EMBEDDED, 12), new Appearance(Theme.LIGHT, FontFamily.SYSTEM, 14), new Appearance(Theme.DARK, FontFamily.EMBEDDED, 14) };

	static void switchThemes(JComponent component) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Appearance appearance : SEQUENCE) {
				appearance.apply();
				SwingUtilities.updateComponentTreeUI(component);

				// force layout, which calls the cell renderers that failed before
				component.setSize(1000, 600);
				component.doLayout();
				validateTree(component);
			}
		});
	}

	static void validateTree(Component c) {
		c.getPreferredSize();
		c.getMinimumSize();
		if (c instanceof Container) {
			for (Component child : ((Container) c).getComponents()) {
				validateTree(child);
			}
		}
	}

	static <T> List<T> find(Component c, Class<T> type, List<T> found) {
		if (type.isInstance(c)) {
			found.add(type.cast(c));
		}
		if (c instanceof Container) {
			for (Component child : ((Container) c).getComponents()) {
				find(child, type, found);
			}
		}
		return found;
	}

	@Test
	public void selectButtonTextField() throws Exception {
		SelectButtonTextField<String> field = new SelectButtonTextField<String>();

		// search history with a selected suggestion (renderer is called while the new UI is installed)
		SwingUtilities.invokeAndWait(() -> {
			for (String item : new String[] { "Firefly", "Firefly (2002)", "Serenity" }) {
				field.getEditor().addItem(item);
			}
			field.getEditor().setSelectedIndex(1);
			field.getEditor().getEditor().setItem("Firefly");
		});

		switchThemes(field);

		assertEquals("Firefly", field.getText());
	}

	@AfterClass
	public static void reset() throws Exception {
		SwingUtilities.invokeAndWait(() -> Appearance.DEFAULT.withTheme(Theme.LIGHT).apply());
	}

}
