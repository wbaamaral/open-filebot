package net.filebot.util.ui;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.AfterClass;
import org.junit.Test;

import com.formdev.flatlaf.FlatLaf;

import net.filebot.util.ui.Appearance.FontFamily;
import net.filebot.util.ui.Appearance.Theme;
import net.filebot.util.ui.Appearance.Typography;

/**
 * Every design token and style class used by the code must be defined by the theme style sheets for every theme.
 */
public class TokensTest {

	static List<String> getTokenKeys() throws Exception {
		List<String> keys = new ArrayList<String>();
		for (Field f : Tokens.class.getFields()) {
			if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
				String value = (String) f.get(null);
				if (value.startsWith("FileBot.")) {
					keys.add(value);
				}
			}
		}
		return keys;
	}

	static void apply(Theme theme) throws Exception {
		SwingUtilities.invokeAndWait(() -> new Appearance(theme, FontFamily.EMBEDDED, Appearance.DEFAULT_FONT_SIZE).apply());
	}

	void assertTokensDefined(Theme theme) throws Exception {
		apply(theme);

		for (String key : getTokenKeys()) {
			assertTrue(theme + ": " + key, Tokens.isDefined(key));
		}
		assertTrue(theme + ": spacing", Tokens.getInt(Tokens.SPACE_SM) > 0);
	}

	void assertStyleClassesDefined(Theme theme) throws Exception {
		apply(theme);
		assertTrue(UIManager.getLookAndFeel() instanceof FlatLaf);

		for (Typography t : Typography.values()) {
			assertNotNull(theme + ": " + t.styleClass, UIManager.get("[style]." + t.styleClass));
		}
		for (String s : new String[] { Tokens.STYLE_INFO, Tokens.STYLE_CARD, Tokens.STYLE_HEADER, Tokens.STYLE_LIST_SURFACE }) {
			assertNotNull(theme + ": " + s, UIManager.get("[style]Panel." + s));
		}
	}

	@Test
	public void lightTheme() throws Exception {
		assertTokensDefined(Theme.LIGHT);
		assertStyleClassesDefined(Theme.LIGHT);
	}

	@Test
	public void darkTheme() throws Exception {
		assertTokensDefined(Theme.DARK);
		assertStyleClassesDefined(Theme.DARK);
	}

	@Test
	public void nimbusFallback() throws Exception {
		assertTokensDefined(Theme.NIMBUS);
	}

	@Test
	public void themeValuesDiffer() throws Exception {
		apply(Theme.LIGHT);
		java.awt.Color light = Tokens.getColor(Tokens.SURFACE_ALT_COLOR);
		apply(Theme.DARK);
		java.awt.Color dark = Tokens.getColor(Tokens.SURFACE_ALT_COLOR);
		assertNotEquals(light, dark);
	}

	@AfterClass
	public static void reset() throws Exception {
		apply(Theme.LIGHT);
	}

}
