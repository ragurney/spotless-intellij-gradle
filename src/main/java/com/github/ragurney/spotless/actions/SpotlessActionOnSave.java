package com.github.ragurney.spotless.actions;

import com.github.ragurney.spotless.config.SpotlessConfiguration;
import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action to be called whenever a document is saved
 */
public class SpotlessActionOnSave extends ActionsOnSaveFileDocumentManagerListener.ActionOnSave {
    @Override
    public boolean isEnabledForProject(@NotNull Project project) {
        return SpotlessConfiguration.getInstance(project).isRunOnSave();
    }

    @Override
    public void processDocuments(@NotNull Project project, @NotNull Document @NotNull [] documents) {
        SpotlessConfiguration spotlessConfiguration = SpotlessConfiguration.getInstance(project);
        if (!spotlessConfiguration.isRunOnSave()) return;

        for (Document document : documents) {
            var file = PsiDocumentManager.getInstance(project).getPsiFile(document);

            if (file == null) {
                continue;
            }

            new ReformatCodeProcessor(file).run();
        }
    }
}
