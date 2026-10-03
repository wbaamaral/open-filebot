package net.filebot.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;

/**
 * Rounded one pixel line border painted with the design tokens {@link Tokens#BORDER_COLOR} and {@link Tokens#RADIUS_MD} of the current theme.
 *
 * Referenced by the style sheets as {@code FileBot.roundedBorder}.
 */
public class TokenBorder extends AbstractBorder {

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Color color = UIManager.getColor(Tokens.BORDER_COLOR);
		int arc = UIManager.getInt(Tokens.RADIUS_MD);

		Graphics2D g2d = (Graphics2D) g.create();
		try {
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setColor(color != null ? color : Color.GRAY);
			g2d.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, width - 1, height - 1, arc, arc));
		} finally {
			g2d.dispose();
		}
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.set(1, 1, 1, 1);
		return insets;
	}

}
