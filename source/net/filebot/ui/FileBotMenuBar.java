package net.filebot.ui;

import static net.filebot.Settings.*;
import static net.filebot.util.ui.SwingUI.*;

import java.util.EnumMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import net.filebot.ResourceManager;
import net.filebot.util.ui.Appearance;
import net.filebot.util.ui.Appearance.Theme;
import net.filebot.util.ui.SwingUI;

public class FileBotMenuBar {

	public static JMenuBar createMenuBar() {
		JMenu themeMenu = new JMenu("Aparência");
		ButtonGroup themeGroup = new ButtonGroup();
		Map<Theme, JRadioButtonMenuItem> themeItems = new EnumMap<Theme, JRadioButtonMenuItem>(Theme.class);

		for (Theme theme : Theme.values()) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(theme.label);
			item.setToolTipText(theme.description);
			item.addActionListener(evt -> SwingUI.setThemePreference(theme.key));
			themeGroup.add(item);
			themeItems.put(theme, item);
			themeMenu.add(item);
		}

		themeMenu.addSeparator();
		themeMenu.add(newAction("Configurar aparência…", evt -> AppearanceDialog.showDialog(themeMenu)));

		// theme may also be changed in the appearance dialog
		themeMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				Theme current = Appearance.load().theme;
				themeItems.forEach((theme, item) -> item.setSelected(theme == current));
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuCanceled(MenuEvent e) {
			}
		});

		JMenu help = new JMenu("Ajuda");

		help.add(newAction("Sobre o FileBot", ResourceManager.getIcon("window.icon.small"), evt -> {
			String message = String.format(
				"<html><b style='font-size:13pt'>Open FileBot %s</b><br><br>"
				+ "Versão Comunitária Open-Source modernizada para Java 25.<br><br>"
				+ "<b>Criador Original:</b> Reinhard Pointner (rednoah)<br>"
				+ "<i>Nosso sincero agradecimento e reconhecimento ao autor original pelo trabalho pioneiro que nos trouxe até aqui.</i><br><br>"
				+ "<b>Ambiente de Execução:</b> Java %s (%s)<br>"
				+ "<b>Licença:</b> GNU Affero GPL v3 (Software Livre)<br>"
				+ "<b>GitHub:</b> @wbaamaral"
				+ "</html>",
				getApplicationVersion(),
				System.getProperty("java.version"),
				System.getProperty("os.name")
			);
			JOptionPane.showMessageDialog(
				null,
				message,
				"Sobre o Open FileBot",
				JOptionPane.INFORMATION_MESSAGE,
				ResourceManager.getIcon("window.icon.medium")
			);
		}));

		help.add(newAction("Atalhos de Teclado", null, evt -> {
			String shortcuts = "<html><b>Principais Atalhos do FileBot:</b><br><br>"
				+ "• <b>F5:</b> Abrir GroovyPad (console de scripts)<br>"
				+ "• <b>Delete:</b> Remover arquivos selecionados da lista<br>"
				+ "• <b>Ctrl + Shift + Delete:</b> Limpar todo o cache de rede e dados<br>"
				+ "• <b>F1:</b> Exibir esta tela de Ajuda<br>"
				+ "</html>";
			JOptionPane.showMessageDialog(null, shortcuts, "Atalhos do FileBot", JOptionPane.INFORMATION_MESSAGE);
		}));

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(themeMenu);
		menuBar.add(help);
		return menuBar;
	}

	public static JMenuBar createHelp() {
		return createMenuBar();
	}

}
