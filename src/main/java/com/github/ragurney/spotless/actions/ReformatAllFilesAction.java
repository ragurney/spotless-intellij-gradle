package com.github.ragurney.spotless.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;


/**
 * The action which is called on "Reformat All Files With Spotless".
 */
public class ReformatAllFilesAction extends AnAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    DataContext dataContext = event.getDataContext();
    event.getData(CommonDataKeys.PSI_FILE);

    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      return;
    }

    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (editor != null) {
      Optional.ofNullable(PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()))
          .ifPresent(file -> new ReformatCodeProcessor(file, true).run());
    }
  }
}
