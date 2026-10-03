package net.filebot.util.ui;

import static net.filebot.Logging.*;

import java.awt.Font;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Stream;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.formdev.flatlaf.util.FontUtils;

import net.filebot.Settings;

/**
 * User interface appearance: color theme, font family and font size.
 *
 * The embedded fonts (Inter and JetBrains Mono) are used by default so that the application looks the same on every operating system. Settings are stored in the user preferences and may be overridden via {@code -Dnet.filebot.theme}, {@code -Dnet.filebot.font} and {@code -Dnet.filebot.font.size}.
 */
public final class Appearance {

	public enum Theme {

		SYSTEM("system", "Automático", "Segue o tema claro ou escuro do sistema"), LIGHT("light", "Claro", "Fundo claro e texto escuro"), DARK("dark", "Escuro", "Fundo escuro, mais confortável à noite"), NIMBUS("nimbus", "Clássico", "Visual Nimbus das versões antigas");

		public final String key;
		public final String label;
		public final String description;

		Theme(String key, String label, String description) {
			this.key = key;
			this.label = label;
			this.description = description;
		}

		public boolean isDark() {
			return this == DARK || (this == SYSTEM && SwingUI.isSystemInDarkMode());
		}

		public static Theme forKey(String key) {
			return Stream.of(values()).filter(t -> t.key.equalsIgnoreCase(key == null ? "" : key.trim())).findFirst().orElse(SYSTEM);
		}
	}

	public enum FontFamily {

		EMBEDDED("embedded", "Inter (embutida)", "Mesma aparência em todos os sistemas"), SYSTEM("system", "Fonte do sistema", "Usa a fonte configurada no sistema operacional");

		public final String key;
		public final String label;
		public final String description;

		FontFamily(String key, String label, String description) {
			this.key = key;
			this.label = label;
			this.description = description;
		}

		public static FontFamily forKey(String key) {
			return Stream.of(values()).filter(f -> f.key.equalsIgnoreCase(key == null ? "" : key.trim())).findFirst().orElse(EMBEDDED);
		}
	}

	public static final int DEFAULT_FONT_SIZE = 14;
	public static final int MIN_FONT_SIZE = 11;
	public static final int MAX_FONT_SIZE = 20;

	public static final Appearance DEFAULT = new Appearance(Theme.SYSTEM, FontFamily.EMBEDDED, DEFAULT_FONT_SIZE);

	private static final String THEME_KEY = "ui.theme";
	private static final String FONT_KEY = "ui.font";
	private static final String FONT_SIZE_KEY = "ui.font.size";

	public final Theme theme;
	public final FontFamily fontFamily;
	public final int fontSize;

	public Appearance(Theme theme, FontFamily fontFamily, int fontSize) {
		this.theme = Objects.requireNonNull(theme);
		this.fontFamily = Objects.requireNonNull(fontFamily);
		this.fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontSize));
	}

	public Appearance withTheme(Theme theme) {
		return new Appearance(theme, fontFamily, fontSize);
	}

	public Appearance withFontFamily(FontFamily fontFamily) {
		return new Appearance(theme, fontFamily, fontSize);
	}

	public Appearance withFontSize(int fontSize) {
		return new Appearance(theme, fontFamily, fontSize);
	}

	/**
	 * @return stored appearance settings, with system property overrides
	 */
	public static Appearance load() {
		Settings settings = Settings.forPackage(SwingUI.class);

		Theme theme = Theme.forKey(System.getProperty("net.filebot.theme", settings.entry(THEME_KEY).defaultValue(DEFAULT.theme.key).getValue()));
		FontFamily font = FontFamily.forKey(System.getProperty("net.filebot.font", settings.entry(FONT_KEY).defaultValue(DEFAULT.fontFamily.key).getValue()));

		int size = DEFAULT_FONT_SIZE;
		try {
			size = Integer.parseInt(System.getProperty("net.filebot.font.size", settings.entry(FONT_SIZE_KEY).defaultValue(String.valueOf(DEFAULT_FONT_SIZE)).getValue()).trim());
		} catch (NumberFormatException e) {
			debug.warning(e::toString);
		}

		return new Appearance(theme, font, size);
	}

	public void store() {
		Settings settings = Settings.forPackage(SwingUI.class);
		settings.entry(THEME_KEY).setValue(theme.key);
		settings.entry(FONT_KEY).setValue(fontFamily.key);
		settings.entry(FONT_SIZE_KEY).setValue(String.valueOf(fontSize));
	}

	/**
	 * Set up look and feel and fonts. Must be called on the EDT. Call {@link #refresh()} afterwards to update windows that are already showing.
	 */
	public void apply() {
		try {
			// design tokens and style classes
			registerStyleSheets();

			if (fontFamily == FontFamily.EMBEDDED) {
				FlatInterFont.installLazy();
				FlatJetBrainsMonoFont.installLazy();
				FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
				FlatLaf.setPreferredLightFontFamily(FlatInterFont.FAMILY_LIGHT);
				FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);
				FlatLaf.setPreferredMonospacedFontFamily(FlatJetBrainsMonoFont.FAMILY);
			} else {
				FlatLaf.setPreferredFontFamily(null);
				FlatLaf.setPreferredLightFontFamily(null);
				FlatLaf.setPreferredSemiboldFontFamily(null);
				FlatLaf.setPreferredMonospacedFontFamily(null);
			}

			switch (theme) {
			case NIMBUS:
				SwingUI.setNimbusLookAndFeel();
				break;
			case DARK:
				FlatDarkLaf.setup();
				break;
			case LIGHT:
				FlatLightLaf.setup();
				break;
			default:
				if (SwingUI.isSystemInDarkMode()) {
					FlatDarkLaf.setup();
				} else {
					FlatLightLaf.setup();
				}
				break;
			}

			UIManager.getLookAndFeelDefaults().put("defaultFont", getDefaultFont());

			// Nimbus doesn't support FlatLaf style sheets
			if (theme == Theme.NIMBUS) {
				Tokens.installFallback(UIManager.getLookAndFeelDefaults());
			}
		} catch (Throwable e) {
			debug.log(Level.WARNING, "Failed to apply appearance: " + this, e);
			SwingUI.setNimbusLookAndFeel();
		}
	}

	private static boolean styleSheetsRegistered = false;

	private static synchronized void registerStyleSheets() {
		if (!styleSheetsRegistered) {
			FlatLaf.registerCustomDefaultsSource(Tokens.STYLE_SHEETS);
			styleSheetsRegistered = true;
		}
	}

	/**
	 * Update all windows after {@link #apply()}.
	 */
	public static void refresh() {
		FlatLaf.updateUI();
	}

	private FontUIResource getDefaultFont() {
		Font font = UIManager.getFont("defaultFont");

		// Nimbus doesn't know about the preferred font families of FlatLaf
		if (theme == Theme.NIMBUS && fontFamily == FontFamily.EMBEDDED) {
			FlatInterFont.installBasic();
			font = FontUtils.getCompositeFont(FlatInterFont.FAMILY, Font.PLAIN, fontSize);
		}

		if (font == null) {
			font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
		}

		return new FontUIResource(font.deriveFont((float) fontSize));
	}

	/**
	 * @return monospaced font of the current appearance, relative to the default font size
	 */
	public static Font getCodeFont(int sizeDelta) {
		Font base = UIManager.getFont("defaultFont");
		int size = (base != null ? base.getSize() : DEFAULT_FONT_SIZE) + sizeDelta;

		Font mono = UIManager.getFont("monospaced.font");
		if (mono == null) {
			// Nimbus doesn't define a monospaced font
			mono = FlatLaf.getPreferredMonospacedFontFamily() != null ? FontUtils.getCompositeFont(FlatLaf.getPreferredMonospacedFontFamily(), Font.PLAIN, size) : new Font(Font.MONOSPACED, Font.PLAIN, size);
		}

		return mono.deriveFont(Font.PLAIN, (float) size);
	}

	/**
	 * Typography roles defined as style classes in the theme style sheets, so that they follow the current font family and size, also when the appearance is changed at runtime.
	 */
	public enum Typography {

		/** Large panel title */
		TITLE("fb-title"),
		/** Bold section heading */
		HEADING("fb-heading"),
		/** Secondary text and captions */
		CAPTION("fb-caption"),
		/** Very small badges and labels */
		BADGE("fb-badge"),
		/** Code and format expressions */
		CODE("fb-code"),
		/** Small code snippets */
		CODE_SMALL("fb-code-small");

		public final String styleClass;

		Typography(String styleClass) {
			this.styleClass = styleClass;
		}

		public <T extends JComponent> T apply(T component) {
			Tokens.styleClass(component, styleClass);

			// fallback for non-FlatLaf themes (e.g. Nimbus)
			Font base = UIManager.getFont("defaultFont");
			if (base != null && !(UIManager.getLookAndFeel() instanceof FlatLaf)) {
				component.setFont(deriveFallback(base));
				if (this == CAPTION) {
					component.setForeground(UIManager.getColor(Tokens.MUTED_COLOR));
				}
			}
			return component;
		}

		private Font deriveFallback(Font base) {
			float size = base.getSize2D();
			switch (this) {
			case TITLE:
				return base.deriveFont(size + 10);
			case HEADING:
				return base.deriveFont(Font.BOLD);
			case CAPTION:
				return base.deriveFont(size - 2);
			case BADGE:
				return base.deriveFont(size - 3);
			case CODE:
				return getCodeFont(0);
			default:
				return getCodeFont(-3);
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Appearance) {
			Appearance other = (Appearance) obj;
			return theme == other.theme && fontFamily == other.fontFamily && fontSize == other.fontSize;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(theme, fontFamily, fontSize);
	}

	@Override
	public String toString() {
		return String.format("%s / %s / %dpx", theme.key, fontFamily.key, fontSize);
	}

}
