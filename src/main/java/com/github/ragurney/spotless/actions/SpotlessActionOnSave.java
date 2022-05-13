package com.github.ragurney.spotless.actions;

import com.github.ragurney.spotless.config.SpotlessConfiguration;
import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/** Action to be called whenever a document is saved */
public class SpotlessActionOnSave extends ActionsOnSaveFileDocumentManagerListener.ActionOnSave {
  @Override
  public boolean isEnabledForProject(@NotNull Project project) {
    return SpotlessConfiguration.getInstance(project).isRunOnSaveEnabled();
  }

  @Override
  public void processDocuments(@NotNull Project project, @NotNull Document @NotNull [] documents) {
    SpotlessConfiguration spotlessConfiguration = SpotlessConfiguration.getInstance(project);
    if (!spotlessConfiguration.isRunOnSaveEnabled()) return;

    for (Document document : documents) {
      Optional.ofNullable(PsiDocumentManager.getInstance(project).getPsiFile(document))
          .ifPresent(file -> new ReformatCodeProcessor(file).run());
    }
  }
}
