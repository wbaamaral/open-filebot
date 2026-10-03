package net.filebot.ui;

import static javax.swing.BorderFactory.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.filebot.util.ui.Appearance.Typography;
import net.filebot.util.ui.ThemeSeparatorBorder;
import net.filebot.util.ui.Tokens;

public class HeaderPanel extends JComponent {

	private JLabel titleLabel = new JLabel();

	private float[] gradientFractions = { 0.0f, 0.5f, 1.0f };

	public HeaderPanel() {
		setLayout(new BorderLayout());

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setOpaque(false);

		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		titleLabel.setVerticalAlignment(SwingConstants.CENTER);
		titleLabel.setOpaque(false);
		Typography.TITLE.apply(titleLabel);

		centerPanel.setBorder(createEmptyBorder());
		centerPanel.add(titleLabel, BorderLayout.CENTER);

		add(centerPanel, BorderLayout.CENTER);
	}

	@Override
	public void updateUI() {
		super.updateUI();
		setBorder(new ThemeSeparatorBorder());
	}

	public void setTitle(String title) {
		titleLabel.setText(title);
	}

	public JLabel getTitleLabel() {
		return titleLabel;
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;

		// subtle horizontal gradient defined by the theme style sheets
		Color edge = Tokens.getColor(Tokens.HEADER_EDGE_COLOR);
		Color center = Tokens.getColor(Tokens.HEADER_CENTER_COLOR);

		titleLabel.setForeground(Tokens.getColor(Tokens.TEXT_COLOR));
		g2d.setPaint(new LinearGradientPaint(0, 0, getWidth(), 0, gradientFractions, new Color[] { edge, center, edge }));
		g2d.fillRect(0, 0, getWidth(), getHeight());
	}

}
