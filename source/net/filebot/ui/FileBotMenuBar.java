package net.filebot.ui;

import static net.filebot.Settings.*;
import static net.filebot.util.ui.SwingUI.*;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

import net.filebot.ResourceManager;
import net.filebot.util.ui.SwingUI;

public class FileBotMenuBar {

	public static JMenuBar createMenuBar() {
		JMenu themeMenu = new JMenu("Aparência");
		ButtonGroup themeGroup = new ButtonGroup();

		JRadioButtonMenuItem systemItem = new JRadioButtonMenuItem("Automático (Sistema)");
		JRadioButtonMenuItem darkItem = new JRadioButtonMenuItem("Tema Escuro (Dark)");
		JRadioButtonMenuItem lightItem = new JRadioButtonMenuItem("Tema Claro (Light)");
		JRadioButtonMenuItem nimbusItem = new JRadioButtonMenuItem("Tema Legado (Nimbus)");

		themeGroup.add(systemItem);
		themeGroup.add(darkItem);
		themeGroup.add(lightItem);
		themeGroup.add(nimbusItem);

		String current = SwingUI.getThemePreference();
		if ("dark".equalsIgnoreCase(current)) {
			darkItem.setSelected(true);
		} else if ("light".equalsIgnoreCase(current)) {
			lightItem.setSelected(true);
		} else if ("nimbus".equalsIgnoreCase(current)) {
			nimbusItem.setSelected(true);
		} else {
			systemItem.setSelected(true);
		}

		systemItem.addActionListener(evt -> SwingUI.setThemePreference("system"));
		darkItem.addActionListener(evt -> SwingUI.setThemePreference("dark"));
		lightItem.addActionListener(evt -> SwingUI.setThemePreference("light"));
		nimbusItem.addActionListener(evt -> SwingUI.setThemePreference("nimbus"));

		themeMenu.add(systemItem);
		themeMenu.add(darkItem);
		themeMenu.add(lightItem);
		themeMenu.addSeparator();
		themeMenu.add(nimbusItem);

		JMenu help = new JMenu("Ajuda");

		help.add(newAction("Sobre o FileBot", ResourceManager.getIcon("window.icon.small"), evt -> {
			String message = String.format(
				"<html><b style='font-size:13pt'>Open FileBot %s</b><br><br>"
				+ "Versão Comunitária Open-Source modernizada para Java 25.<br><br>"
				+ "<b>Criador Original:</b> Reinhard Pointner (rednoah)<br>"
				+ "<i>Nosso sincero agradecimento e reconhecimento ao autor original pelo trabalho pioneiro que nos trouxe até aqui.</i><br><br>"
				+ "<b>Ambiente de Execução:</b> Java %s (%s)<br>"
				+ "<b>Licença:</b> GNU Affero GPL v3 (Software Livre)"
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
				+ "• <b>F5:</b> Executar Renomeação / Ação<br>"
				+ "• <b>Delete:</b> Remover arquivos selecionados da lista<br>"
				+ "• <b>Ctrl + Shift + Delete:</b> Limpar toda a lista<br>"
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
