package net.filebot.util.ui;

import static net.filebot.Logging.*;

import java.awt.Color;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

/**
 * Design tokens of the user interface.
 *
 * Values are defined in the theme style sheets ({@code net/filebot/theme/*.properties}) and resolved by FlatLaf for the current theme. Code should use these accessors and style classes instead of literal colors, sizes or fonts.
 */
public final class Tokens {

	/** Package of the theme style sheets */
	public static final String STYLE_SHEETS = "net/filebot/theme";

	// spacing scale
	public static final String SPACE_XS = "FileBot.space.xs";
	public static final String SPACE_SM = "FileBot.space.sm";
	public static final String SPACE_MD = "FileBot.space.md";
	public static final String SPACE_LG = "FileBot.space.lg";
	public static final String SPACE_XL = "FileBot.space.xl";

	// corner radius
	public static final String RADIUS_SM = "FileBot.radius.sm";
	public static final String RADIUS_MD = "FileBot.radius.md";
	public static final String RADIUS_LG = "FileBot.radius.lg";

	// icon sizes
	public static final String ICON_SMALL = "FileBot.icon.small";
	public static final String ICON_MEDIUM = "FileBot.icon.medium";
	public static final String ICON_LARGE = "FileBot.icon.large";

	// semantic colors
	public static final String TEXT_COLOR = "FileBot.textColor";
	public static final String MUTED_COLOR = "FileBot.mutedColor";
	public static final String ACCENT_COLOR = "FileBot.accentColor";
	public static final String LINK_COLOR = "FileBot.linkColor";
	public static final String BORDER_COLOR = "FileBot.borderColor";
	public static final String SEPARATOR_COLOR = "FileBot.separatorColor";
	public static final String SURFACE_COLOR = "FileBot.surfaceColor";
	public static final String SURFACE_ALT_COLOR = "FileBot.surfaceAltColor";
	public static final String SURFACE_RAISED_COLOR = "FileBot.surfaceRaisedColor";
	public static final String SUCCESS_COLOR = "FileBot.successColor";
	public static final String WARNING_COLOR = "FileBot.warningColor";
	public static final String DANGER_COLOR = "FileBot.dangerColor";
	public static final String INFO_COLOR = "FileBot.infoColor";
	public static final String HEADER_EDGE_COLOR = "FileBot.header.edgeColor";
	public static final String HEADER_CENTER_COLOR = "FileBot.header.centerColor";
	public static final String ICON_GLYPH_COLOR = "FileBot.icon.glyphColor";
	public static final String ICON_ON_TILE_COLOR = "FileBot.icon.onTileColor";

	// style classes
	public static final String STYLE_INFO = "fb-info";
	public static final String STYLE_CARD = "fb-card";
	public static final String STYLE_HEADER = "fb-header";
	public static final String STYLE_LIST_SURFACE = "fb-list-surface";

	/**
	 * @return integer token in pixels (e.g. {@link #SPACE_MD})
	 */
	public static int getInt(String key) {
		Object value = UIManager.get(key);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		undefined(key);
		return 0;
	}

	/**
	 * @return color token for the current theme (e.g. {@link #ACCENT_COLOR})
	 */
	public static Color getColor(String key) {
		Color color = UIManager.getColor(key);
		if (color != null) {
			return color;
		}

		// a missing token must never break the user interface
		undefined(key);
		Color text = UIManager.getColor("Label.foreground");
		return text != null ? text : Color.GRAY;
	}

	/**
	 * @return {@code true} if the given token is defined for the current theme
	 */
	public static boolean isDefined(String key) {
		return UIManager.get(key) != null;
	}

	private static final java.util.Set<String> reported = java.util.concurrent.ConcurrentHashMap.newKeySet();

	private static void undefined(String key) {
		if (reported.add(key + "@" + UIManager.getLookAndFeel().getName())) {
			debug.warning("Undefined design token: " + key + " [" + UIManager.getLookAndFeel().getName() + "]");
		}
	}

	/**
	 * @return spacing token formatted for MigLayout constraints (e.g. {@code "12px"})
	 */
	public static String px(String key) {
		return getInt(key) + "px";
	}

	/**
	 * Set style classes on a component, so that its style follows the current theme.
	 */
	public static <T extends javax.swing.JComponent> T styleClass(T component, String... styleClasses) {
		component.putClientProperty("FlatLaf.styleClass", String.join(" ", styleClasses));

		// themes without style sheet support (e.g. Nimbus) get the surface styles applied directly
		if (!(UIManager.getLookAndFeel() instanceof com.formdev.flatlaf.FlatLaf)) {
			for (String styleClass : styleClasses) {
				applyFallbackStyle(component, styleClass);
			}
		}
		return component;
	}

	private static void applyFallbackStyle(javax.swing.JComponent component, String styleClass) {
		switch (styleClass) {
		case STYLE_INFO:
			component.setBackground(getColor(SURFACE_ALT_COLOR));
			component.setBorder(new TokenBorder());
			break;
		case STYLE_CARD:
			component.setBackground(getColor(SURFACE_RAISED_COLOR));
			component.setBorder(new TokenBorder());
			break;
		case STYLE_HEADER:
			component.setBackground(getColor(SURFACE_RAISED_COLOR));
			break;
		case STYLE_LIST_SURFACE:
			Color table = UIManager.getColor("Table.background");
			if (table != null) {
				component.setBackground(table);
			}
			break;
		}
	}

	private static final Pattern LITERAL_INT = Pattern.compile("\\d+");
	private static final Pattern LITERAL_COLOR = Pattern.compile("#\\p{XDigit}{6}");
	private static final Pattern REFERENCE = Pattern.compile("\\$[\\w.]+");

	/**
	 * Install design tokens for themes that don't support FlatLaf style sheets (e.g. Nimbus). Literal values and simple references are taken from the same style sheets, everything else falls back to the surface color.
	 */
	public static void installFallback(UIDefaults defaults) {
		Properties tokens = new Properties();
		for (String file : new String[] { "FlatLaf.properties", "FlatLightLaf.properties" }) {
			try (InputStream in = Tokens.class.getClassLoader().getResourceAsStream(STYLE_SHEETS + "/" + file)) {
				tokens.load(new InputStreamReader(in, StandardCharsets.UTF_8));
			} catch (Exception e) {
				debug.log(Level.WARNING, "Failed to load design tokens: " + file, e);
			}
		}

		Color surface = defaults.getColor("Panel.background");

		tokens.stringPropertyNames().stream().filter(k -> k.startsWith("FileBot.")).sorted().forEach(k -> {
			String value = tokens.getProperty(k).trim();
			if (LITERAL_INT.matcher(value).matches()) {
				defaults.put(k, Integer.parseInt(value));
			} else if (LITERAL_COLOR.matcher(value).matches()) {
				defaults.put(k, new ColorUIResource(Color.decode(value)));
			} else if (REFERENCE.matcher(value).matches() && defaults.get(value.substring(1)) != null) {
				defaults.put(k, defaults.get(value.substring(1)));
			} else if (k.endsWith("Color") && surface != null) {
				defaults.put(k, new ColorUIResource(surface));
			}
		});

		// tokens that reference keys that only exist in FlatLaf
		putIfAbsent(defaults, TEXT_COLOR, defaults.get("Label.foreground"), defaults.get("text"));
		putIfAbsent(defaults, MUTED_COLOR, defaults.get("Label.disabledForeground"), defaults.get("textInactiveText"));
		putIfAbsent(defaults, BORDER_COLOR, defaults.get("nimbusBorder"), defaults.get("controlShadow"));
		putIfAbsent(defaults, SEPARATOR_COLOR, defaults.get("Separator.foreground"), defaults.get("nimbusBorder"));
		putIfAbsent(defaults, ACCENT_COLOR, defaults.get("nimbusSelectionBackground"), defaults.get("nimbusFocus"));
		putIfAbsent(defaults, LINK_COLOR, defaults.get("nimbusFocus"), defaults.get("nimbusSelectionBackground"));
	}

	private static void putIfAbsent(UIDefaults defaults, String key, Object... candidates) {
		if (defaults.get(key) == null) {
			for (Object value : candidates) {
				if (value != null) {
					defaults.put(key, value);
					return;
				}
			}
		}
	}

	private Tokens() {
		throw new UnsupportedOperationException();
	}

}
