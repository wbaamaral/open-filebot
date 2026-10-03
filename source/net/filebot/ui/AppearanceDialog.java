package net.filebot.ui;

import static javax.swing.BorderFactory.*;
import static net.filebot.util.ui.SwingUI.*;
import static net.filebot.util.ui.Tokens.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import net.filebot.util.ui.Appearance;
import net.filebot.util.ui.Appearance.FontFamily;
import net.filebot.util.ui.Appearance.Theme;
import net.filebot.util.ui.Appearance.Typography;
import net.filebot.util.ui.Tokens;
import net.miginfocom.swing.MigLayout;

/**
 * Configure color theme, font family and font size. Changes are applied immediately to all windows, so the application itself serves as preview.
 */
public class AppearanceDialog extends JDialog {

	private final Appearance initial;
	private Appearance current;

	private final Map<Theme, JToggleButton> themeButtons = new EnumMap<Theme, JToggleButton>(Theme.class);
	private final Map<FontFamily, JRadioButton> fontButtons = new EnumMap<FontFamily, JRadioButton>(FontFamily.class);

	private final JSlider fontSizeSlider = new JSlider(Appearance.MIN_FONT_SIZE, Appearance.MAX_FONT_SIZE);
	private final JLabel fontSizeLabel = new JLabel();

	private boolean updating = false;

	public AppearanceDialog(Window owner) {
		super(owner, "Aparência", ModalityType.APPLICATION_MODAL);

		initial = Appearance.load();
		current = initial;

		JComponent content = (JComponent) getContentPane();
		String sm = px(SPACE_SM), md = px(SPACE_MD), lg = px(SPACE_LG), xl = px(SPACE_XL);
		content.setLayout(new MigLayout(String.format("insets %s %s %s %s, gap %s %s, fill, wrap 1", lg, xl, lg, xl, sm, md), "[grow, fill]", String.format("[][][]%s[][][]%s[][grow, fill]%s[]", sm, sm, lg)));

		// Theme
		content.add(Typography.HEADING.apply(new JLabel("Tema")));
		content.add(createThemeSelector());
		content.add(new JSeparator());

		// Font
		content.add(Typography.HEADING.apply(new JLabel("Fonte")));
		content.add(createFontSelector());
		content.add(new JSeparator());

		// Preview
		content.add(Typography.HEADING.apply(new JLabel("Pré-visualização")));
		content.add(createPreview());

		// Buttons
		content.add(createButtonBar());

		installAction(content, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), newAction("Cancelar", evt -> cancel()));
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				cancel();
			}
		});

		updateControls();

		pack();
		setMinimumSize(getPreferredSize());
		setLocationRelativeTo(owner);
	}

	private JComponent createThemeSelector() {
		JPanel panel = new JPanel(new MigLayout("insets 0, gap " + px(SPACE_MD), "[sg theme, fill][sg theme, fill][sg theme, fill][sg theme, fill]"));
		ButtonGroup group = new ButtonGroup();

		for (Theme theme : Theme.values()) {
			JToggleButton button = new JToggleButton(theme.label, new ThemePreviewIcon(theme));
			button.setVerticalTextPosition(SwingConstants.BOTTOM);
			button.setHorizontalTextPosition(SwingConstants.CENTER);
			button.setIconTextGap(getInt(SPACE_SM));
			button.setFocusPainted(false);
			button.setToolTipText(theme.description);
			button.putClientProperty("JButton.buttonType", "toolBarButton");
			button.addActionListener(evt -> select(current.withTheme(theme)));

			group.add(button);
			themeButtons.put(theme, button);
			panel.add(button);
		}

		return panel;
	}

	private JComponent createFontSelector() {
		JPanel panel = new JPanel(new MigLayout(String.format("insets 0, gap %s %s, wrap 2", px(SPACE_SM), px(SPACE_XS)), "[][grow]"));
		ButtonGroup group = new ButtonGroup();

		for (FontFamily font : FontFamily.values()) {
			JRadioButton button = new JRadioButton(font.label);
			button.addActionListener(evt -> select(current.withFontFamily(font)));

			group.add(button);
			fontButtons.put(font, button);

			panel.add(button, "span 2, gaptop " + px(SPACE_XS));
			panel.add(createCaption(font.description), "span 2, gapleft " + getTextIndent(button) + "px");
		}

		fontSizeSlider.setMajorTickSpacing(1);
		fontSizeSlider.setSnapToTicks(true);
		fontSizeSlider.addChangeListener(evt -> {
			fontSizeLabel.setText(fontSizeSlider.getValue() + " px");
			if (!fontSizeSlider.getValueIsAdjusting()) {
				select(current.withFontSize(fontSizeSlider.getValue()));
			}
		});

		JPanel sizePanel = new JPanel(new MigLayout("insets 0, gap " + px(SPACE_SM), "[][grow, fill][]"));
		sizePanel.add(new JLabel("Tamanho"));
		sizePanel.add(fontSizeSlider);
		sizePanel.add(fontSizeLabel, "w 48!");
		panel.add(sizePanel, "span 2, growx, gaptop " + px(SPACE_MD));

		return panel;
	}

	private JComponent createPreview() {
		JPanel panel = new JPanel(new MigLayout(String.format("insets %s %s %s %s, gap %s %s, wrap 1, fill", px(SPACE_MD), px(SPACE_LG), px(SPACE_MD), px(SPACE_LG), px(SPACE_SM), px(SPACE_XS)), "[grow, fill]"));
		Tokens.styleClass(panel, STYLE_CARD);

		panel.add(Typography.TITLE.apply(new JLabel("Rename")));
		panel.add(new JLabel("Shingeki no Kyojin - 進撃の巨人 - S01E01 - Até Você.mkv"));
		panel.add(createCaption("Legenda e textos auxiliares"));
		panel.add(Typography.CODE.apply(new JLabel("{n} - {s00e00} - {t}")));

		JPanel row = new JPanel(new MigLayout("insets 0, gap " + px(SPACE_SM), "[grow, fill][]"));
		row.setOpaque(false);
		row.add(new JTextField("Campo de texto"));
		row.add(new JButton("Botão"));
		panel.add(row, "gaptop " + px(SPACE_XS));

		return panel;
	}

	private JComponent createButtonBar() {
		JPanel panel = new JPanel(new MigLayout("insets 0, gap " + px(SPACE_SM), "[]push[sg button][sg button]"));

		panel.add(new JButton(newAction("Restaurar padrão", evt -> select(Appearance.DEFAULT))));
		panel.add(new JButton(newAction("Cancelar", evt -> cancel())));

		JButton ok = new JButton(newAction("OK", evt -> accept()));
		panel.add(ok);
		getRootPane().setDefaultButton(ok);

		return panel;
	}

	/**
	 * @return horizontal offset of the radio button text, so that captions line up with it
	 */
	private static int getTextIndent(JRadioButton button) {
		Icon icon = UIManager.getIcon("RadioButton.icon");
		return button.getInsets().left + (icon != null ? icon.getIconWidth() : 0) + button.getIconTextGap();
	}

	private static JLabel createCaption(String text) {
		return Typography.CAPTION.apply(new JLabel(text));
	}

	private void select(Appearance appearance) {
		if (updating || appearance.equals(current)) {
			return;
		}

		current = appearance;
		current.apply();
		Appearance.refresh();

		updateControls();
		pack();
	}

	private void updateControls() {
		updating = true;
		try {
			themeButtons.forEach((theme, button) -> button.setSelected(theme == current.theme));
			fontButtons.forEach((font, button) -> button.setSelected(font == current.fontFamily));
			fontSizeSlider.setValue(current.fontSize);
			fontSizeLabel.setText(current.fontSize + " px");

			// show which theme is used in automatic mode
			JToggleButton auto = themeButtons.get(Theme.SYSTEM);
			auto.setToolTipText(String.format("%s (atualmente: %s)", Theme.SYSTEM.description, Theme.SYSTEM.isDark() ? Theme.DARK.label : Theme.LIGHT.label));
		} finally {
			updating = false;
		}
	}

	private void accept() {
		current.store();
		dispose();
	}

	private void cancel() {
		if (!current.equals(initial)) {
			initial.apply();
			Appearance.refresh();
		}
		dispose();
	}

	public static void showDialog(Component parent) {
		AppearanceDialog dialog = new AppearanceDialog(parent == null ? null : getWindow(parent));
		dialog.setVisible(true);
	}

	/**
	 * Miniature window drawn in the colors of the given theme.
	 */
	private static class ThemePreviewIcon implements Icon {

		private static final int WIDTH = 112;
		private static final int HEIGHT = 72;

		private final Theme theme;

		public ThemePreviewIcon(Theme theme) {
			this.theme = theme;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2d = (Graphics2D) g.create();
			try {
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.translate(x, y);

				RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, WIDTH - 1, HEIGHT - 1, 10, 10);
				g2d.clip(shape);

				switch (theme) {
				case SYSTEM:
					paintWindow(g2d, Palette.LIGHT);
					g2d.clip(new java.awt.Polygon(new int[] { WIDTH, WIDTH, 0 }, new int[] { 0, HEIGHT, HEIGHT }, 3));
					paintWindow(g2d, Palette.DARK);
					break;
				case DARK:
					paintWindow(g2d, Palette.DARK);
					break;
				case NIMBUS:
					paintWindow(g2d, Palette.NIMBUS);
					break;
				default:
					paintWindow(g2d, Palette.LIGHT);
					break;
				}

				g2d.setClip(null);
				g2d.setStroke(new BasicStroke(1f));
				g2d.setColor(Tokens.getColor(BORDER_COLOR));
				g2d.draw(shape);
			} finally {
				g2d.dispose();
			}
		}

		private void paintWindow(Graphics2D g, Palette p) {
			// window background and title bar
			g.setColor(p.background);
			g.fillRect(0, 0, WIDTH, HEIGHT);
			g.setColor(p.header);
			g.fillRect(0, 0, WIDTH, 14);

			// side bar with selected item
			g.setColor(p.panel);
			g.fillRoundRect(6, 20, 22, HEIGHT - 26, 4, 4);
			g.setColor(p.accent);
			g.fillRoundRect(9, 24, 16, 10, 3, 3);

			// content with text lines
			g.setColor(p.panel);
			g.fillRoundRect(34, 20, WIDTH - 40, HEIGHT - 26, 4, 4);
			g.setColor(p.text);
			g.fillRoundRect(40, 27, 46, 4, 2, 2);
			g.fillRoundRect(40, 36, 60, 4, 2, 2);
			g.fillRoundRect(40, 45, 38, 4, 2, 2);
			g.setColor(p.accent);
			g.fillRoundRect(WIDTH - 32, HEIGHT - 16, 22, 8, 4, 4);
		}

		@Override
		public int getIconWidth() {
			return WIDTH;
		}

		@Override
		public int getIconHeight() {
			return HEIGHT;
		}
	}

	/**
	 * Representative colors of each theme, used only for the theme previews.
	 */
	private enum Palette {

		LIGHT(0xF2F2F2, 0xE6E6E6, 0xFFFFFF, 0x8C8C8C, 0x2675BF), DARK(0x3C3F41, 0x2F3133, 0x46494B, 0xA9ABAD, 0x4A88C7), NIMBUS(0xD6D9DF, 0xB9C2CC, 0xF4F6F9, 0x5A6470, 0x39698A);

		final Color background;
		final Color header;
		final Color panel;
		final Color text;
		final Color accent;

		Palette(int background, int header, int panel, int text, int accent) {
			this.background = new Color(background);
			this.header = new Color(header);
			this.panel = new Color(panel);
			this.text = new Color(text);
			this.accent = new Color(accent);
		}
	}

}
