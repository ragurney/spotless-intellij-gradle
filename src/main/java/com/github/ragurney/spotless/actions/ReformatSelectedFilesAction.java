package com.github.ragurney.spotless.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ui.ChangesListView;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ReformatSelectedFilesAction extends AnAction {

	@Override
	public void actionPerformed(@NotNull AnActionEvent event) {
		DataContext dataContext = event.getDataContext();

		final Project project = CommonDataKeys.PROJECT.getData(dataContext);
		if (project == null) {
			return;
		}
		PsiManager manager = PsiManager.getInstance(project);
		Stream
				.concat(getExactlySelectedFiles(manager, event).stream(), selectedChangesetFiles(manager, event).stream())
				.forEach(file -> new ReformatCodeProcessor(file, false).run());
	}

	private List<PsiFile> selectedChangesetFiles(PsiManager manager, AnActionEvent event) {
		@Nullable ChangesListView selectedChanges = event.getData(ChangesListView.DATA_KEY);
		if (selectedChanges != null && selectedChanges.getSelectedChanges().isNotEmpty()) {
			return selectedChanges.getSelectedChanges()
					.filter(change -> change.getVirtualFile() != null)
					.map(change -> manager.findFile(change.getVirtualFile()))
					.toList();
		}
		return Collections.emptyList();
	}

	private List<PsiFile> getExactlySelectedFiles(PsiManager manager, AnActionEvent event) {
		@Nullable Iterable<VirtualFile> exactlySelected = event.getData(ChangesListView.EXACTLY_SELECTED_FILES_DATA_KEY);
		if (exactlySelected != null) {
			return StreamSupport
					.stream(exactlySelected.spliterator(), false)
					.map(manager::findFile)
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}
}
