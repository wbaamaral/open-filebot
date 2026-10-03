package net.filebot.ui;

import static net.filebot.Logging.*;

import java.util.logging.Level;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.filebot.ResourceManager;

public class GettingStartedStage {

	public static void start() {
		SwingUtilities.invokeLater(() -> {
			try {
				String helpText = "<html><b style='font-size:13pt'>Primeiros Passos com o FileBot</b><br><br>"
					+ "1. <b>Carregar Arquivos:</b> Arraste e solte episódios ou filmes na lista da esquerda (Original Files).<br>"
					+ "2. <b>Buscar Metadados:</b> Clique no botão <b>Fetch Data</b> (TheMovieDB, TheTVDB, AniDB).<br>"
					+ "3. <b>Verificar Nomes:</b> Confira os novos nomes na lista da direita (New Names).<br>"
					+ "4. <b>Renomear:</b> Clique em <b>Rename</b> para aplicar as alterações.<br><br>"
					+ "<b>Atalhos Rápidos:</b><br>"
					+ "• <i>F5:</i> Abrir GroovyPad (console de scripts)<br>"
					+ "• <i>Delete:</i> Remover arquivo da lista<br>"
					+ "• <i>Ctrl+Shift+Delete:</i> Limpar cache<br>"
					+ "• <i>F1:</i> Esta tela de ajuda"
					+ "</html>";

				JOptionPane.showMessageDialog(
					null,
					helpText,
					"FileBot - Como Usar",
					JOptionPane.INFORMATION_MESSAGE,
					ResourceManager.getIcon("window.icon.medium")
				);
			} catch (Throwable e) {
				debug.log(Level.WARNING, "Falha ao exibir ajuda interna", e);
			}
		});
	}

	private GettingStartedStage() {
		throw new UnsupportedOperationException();
	}

}
