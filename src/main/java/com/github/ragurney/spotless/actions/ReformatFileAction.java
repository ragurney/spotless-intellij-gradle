package com.github.ragurney.spotless.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;


/**
 * The action which is called on "Reformat Current File."
 */
public class ReformatFileAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(ReformatFileAction.class);

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    DataContext dataContext = event.getDataContext();

    final Project project = CommonDataKeys.PROJECT.getData(dataContext);

    if (project == null) {
      return;
    }
    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);

    PsiFile file;

    if (editor != null) {
      file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

      if (file == null) {
        return;
      }

      new ReformatCodeProcessor(file).run();
    }
  }
}
