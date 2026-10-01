package net.filebot.ui;

import static net.filebot.Settings.*;
import static net.filebot.util.ui.SwingUI.*;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;

import net.filebot.ResourceManager;

public class FileBotMenuBar {

	public static JMenuBar createHelp() {
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
		menuBar.add(help);
		return menuBar;
	}

}
