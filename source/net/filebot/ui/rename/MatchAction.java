package net.filebot.ui.rename;

import static net.filebot.Logging.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;

import javax.swing.AbstractAction;

import net.filebot.ResourceManager;
import net.filebot.similarity.EpisodeMetrics;
import net.filebot.similarity.Match;
import net.filebot.similarity.Matcher;
import net.filebot.util.ui.ProgressMonitor;

class MatchAction extends AbstractAction {

	private final RenameModel model;

	public MatchAction(RenameModel model) {
		this.model = model;

		// initialize with default values
		setMatchMode(false);
	}

	public void setMatchMode(boolean strict) {
		putValue(NAME, "Match");
		putValue(SMALL_ICON, ResourceManager.getIcon(strict ? "action.match.strict" : "action.match"));
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		if (model.names().isEmpty() || model.files().isEmpty()) {
			return;
		}

		// disable while running
		setEnabled(false);

		Matcher<Object, File> matcher = new Matcher<Object, File>(model.values(), model.candidates(), false, EpisodeMetrics.defaultSequence(true));

		// async: do not block the EDT
		ProgressMonitor.runTask("Match", "Finding optimal alignment. This may take a while.", (message, progress, cancelled) -> {
			message.accept(String.format("Checking %d combinations...", matcher.remainingCandidates().size() * matcher.remainingValues().size()));
			return matcher.match();
		}, result -> {
			setEnabled(true);

			if (result.isError()) {
				Throwable e = result.getError();
				if (!(e instanceof CancellationException)) {
					log.log(Level.WARNING, e.getMessage(), e);
				}
				return;
			}

			try {
				List<Match<Object, File>> matches = result.getValue();

				// put new data into model
				model.clear();
				model.addAll(matches);

				// insert objects that could not be matched at the end of the model
				model.addAll(matcher.remainingValues(), matcher.remainingCandidates());
			} catch (Throwable e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		});
	}

}
