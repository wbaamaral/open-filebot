package net.filebot.util.ui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;

/**
 * One pixel separator line at the bottom, painted in the separator color of the current theme.
 */
public class ThemeSeparatorBorder extends AbstractBorder {

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		g.setColor(Tokens.getColor(Tokens.SEPARATOR_COLOR));
		g.fillRect(x, y + height - 1, width, 1);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.set(0, 0, 1, 0);
		return insets;
	}

}
