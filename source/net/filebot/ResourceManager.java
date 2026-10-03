package net.filebot;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import net.filebot.util.ui.Tokens;

public final class ResourceManager {

	private static final Map<String, Icon> cache = synchronizedMap(new HashMap<String, Icon>(256));

	public static Icon getIcon(String name) {
		return cache.computeIfAbsent(ICON_ALIASES.getOrDefault(name, name), i -> {
			// prefer scalable icons that follow the design tokens of the current theme
			URL svg = ResourceManager.class.getResource("resources/svg/" + i + ".svg");
			if (svg != null) {
				FlatSVGIcon icon = new FlatSVGIcon(svg);
				icon.setColorFilter(ICON_COLOR_FILTER);
				return icon;
			}

			// load image
			URL[] resource = getMultiResolutionImageResource(i);
			if (resource.length > 0) {
				return new ImageIcon(getMultiResolutionImage(resource));
			}

			// default image
			return null;
		});
	}

	private static final Map<String, String> ICON_ALIASES = Map.of("window.icon.small", "window.icon16", "window.icon.medium", "window.icon64", "window.icon.large", "window.icon64");

	/**
	 * Replace placeholder colors of the SVG icons with the design tokens of the current theme at paint time (see resources/svg/colors.properties).
	 */
	private static final FlatSVGIcon.ColorFilter ICON_COLOR_FILTER = new FlatSVGIcon.ColorFilter(createIconColorMapper());

	private static Function<Color, Color> createIconColorMapper() {
		Map<Color, String> tokens = new HashMap<Color, String>();

		Properties roles = new Properties();
		try (InputStream in = ResourceManager.class.getResourceAsStream("resources/svg/colors.properties")) {
			roles.load(in);
		} catch (Exception e) {
			debug.log(Level.WARNING, "Failed to load icon color roles", e);
		}

		roles.forEach((role, value) -> {
			String[] v = value.toString().split(",");
			tokens.put(Color.decode(v[0].trim()), v[1].trim());
		});

		return color -> {
			String token = tokens.get(new Color(color.getRGB() & 0xFFFFFF));
			if (token == null) {
				return color;
			}
			Color c = Tokens.getColor(token);
			return color.getAlpha() == 255 ? c : new Color(c.getRed(), c.getGreen(), c.getBlue(), color.getAlpha());
		};
	}

	public static Stream<URL> getApplicationIconResources() {
		return Stream.of("window.icon16", "window.icon64").map(ResourceManager::getImageResource);
	}

	public static List<Image> getApplicationIconImages() {
		return Stream.of("window.icon16", "window.icon64").map(ResourceManager::getMultiResolutionImageResource).map(ResourceManager::getMultiResolutionImage).collect(toList());
	}

	public static Icon getFlagIcon(String languageCode) {
		return getIcon("flags/" + languageCode);
	}

	private static Image getMultiResolutionImage(URL[] resource) {
		try {
			Image[] image = new Image[resource.length];
			for (int i = 0; i < image.length; i++) {
				image[i] = ImageIO.read(resource[i]);
			}
			return new BaseMultiResolutionImage(image);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static URL[] getMultiResolutionImageResource(String name) {
		return Stream.of(name, name + "@2x").map(ResourceManager::getImageResource).filter(Objects::nonNull).toArray(URL[]::new);
	}

	private static URL getImageResource(String name) {
		return ResourceManager.class.getResource("resources/" + name + ".png");
	}

	private ResourceManager() {
		throw new UnsupportedOperationException();
	}

}
