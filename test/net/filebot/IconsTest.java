package net.filebot;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.junit.Test;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import net.filebot.util.ui.Appearance;
import net.filebot.util.ui.Appearance.FontFamily;
import net.filebot.util.ui.Appearance.Theme;
import net.filebot.util.ui.Tokens;

/**
 * The SVG icon set must be complete and use design tokens for every color.
 */
public class IconsTest {

	static final Pattern COLOR = Pattern.compile("#\\p{XDigit}{6}\\b");

	static Path getIconFolder() throws Exception {
		URL colors = ResourceManager.class.getResource("resources/svg/colors.properties");
		return Paths.get(colors.toURI()).getParent();
	}

	static List<Path> getIcons() throws Exception {
		try (Stream<Path> files = Files.list(getIconFolder())) {
			return files.filter(f -> f.toString().endsWith(".svg")).sorted().collect(Collectors.toList());
		}
	}

	static Properties getColorRoles() throws Exception {
		Properties roles = new Properties();
		try (InputStream in = ResourceManager.class.getResourceAsStream("resources/svg/colors.properties")) {
			roles.load(in);
		}
		return roles;
	}

	@Test
	public void iconsLoad() throws Exception {
		List<Path> icons = getIcons();
		assertTrue(icons.size() > 80);

		for (Path f : icons) {
			String name = f.getFileName().toString().replaceFirst("[.]svg$", "");
			Icon icon = ResourceManager.getIcon(name);
			assertTrue(name, icon instanceof FlatSVGIcon && ((FlatSVGIcon) icon).hasFound());
			assertTrue(name, icon.getIconWidth() > 0 && icon.getIconWidth() == icon.getIconHeight());
		}
	}

	@Test
	public void iconsOnlyUsePlaceholderColors() throws Exception {
		Set<String> placeholders = getColorRoles().values().stream().map(v -> v.toString().split(",")[0].trim().toUpperCase()).collect(Collectors.toSet());

		for (Path f : getIcons()) {
			Matcher m = COLOR.matcher(new String(Files.readAllBytes(f), UTF_8));
			while (m.find()) {
				assertTrue(f.getFileName() + ": " + m.group(), placeholders.contains(m.group().toUpperCase()));
			}
		}
	}

	@Test
	public void colorRolesAreDesignTokens() throws Exception {
		for (Theme theme : new Theme[] { Theme.LIGHT, Theme.DARK, Theme.NIMBUS }) {
			SwingUtilities.invokeAndWait(() -> new Appearance(theme, FontFamily.EMBEDDED, Appearance.DEFAULT_FONT_SIZE).apply());
			for (Object value : getColorRoles().values()) {
				String token = value.toString().split(",")[1].trim();
				assertTrue(theme + ": " + token, Tokens.isDefined(token));
			}
		}
		SwingUtilities.invokeAndWait(() -> Appearance.DEFAULT.withTheme(Theme.LIGHT).apply());
	}

	@Test
	public void aliases() {
		assertNotNull(ResourceManager.getIcon("window.icon.medium"));
		assertNotNull(ResourceManager.getIcon("rename.action.test"));
		assertNotNull(ResourceManager.getIcon("action.duplicate"));
	}

}
