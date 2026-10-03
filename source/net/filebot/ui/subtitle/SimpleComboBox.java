package net.filebot.ui.subtitle;

import static javax.swing.BorderFactory.*;

import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

import net.filebot.util.ui.Tokens;

public class SimpleComboBox extends JComboBox {

	private final Icon dropDownArrowIcon;

	public SimpleComboBox(Icon dropDownArrowIcon) {
		this.dropDownArrowIcon = dropDownArrowIcon;
		updateUI();
		setBorder(createEmptyBorder());
	}

	@Override
	public void updateUI() {
		// keep the custom combo box UI when the look and feel or theme changes at runtime (called by the super constructor before the icon is set)
		if (dropDownArrowIcon != null) {
			setUI(new SimpleComboBoxUI(dropDownArrowIcon));
		} else {
			super.updateUI();
		}
	}

	private static class SimpleComboBoxUI extends BasicComboBoxUI {

		private final Icon dropDownArrowIcon;

		public SimpleComboBoxUI(Icon dropDownArrowIcon) {
			this.dropDownArrowIcon = dropDownArrowIcon;
		}

		@Override
		protected JButton createArrowButton() {
			JButton button = new JButton(dropDownArrowIcon);
			button.setContentAreaFilled(false);
			button.setBorderPainted(false);
			button.setFocusPainted(false);
			button.setOpaque(false);

			return button;
		}

		@Override
		protected ComboPopup createPopup() {
			return new BasicComboPopup(comboBox) {

				@Override
				protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
					Rectangle bounds = super.computePopupBounds(px, py, pw, ph);

					// allow combobox popup to be wider than the combobox itself
					bounds.width = Math.max(bounds.width, list.getPreferredSize().width);

					return bounds;
				}

				@Override
				protected void configurePopup() {
					super.configurePopup();

					setOpaque(true);

					// use theme border color instead of black border for combobox popup
					setBorder(createCompoundBorder(createLineBorder(Tokens.getColor(Tokens.BORDER_COLOR), 1), createEmptyBorder(1, 1, 1, 1)));
				}
			};
		}
	}

}
