package com.github.ragurney.spotless.trigger;

import com.github.ragurney.spotless.actions.ReformatCodeProcessor;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.util.PairConsumer;
import java.awt.*;
import java.util.Optional;
import javax.swing.*;
import org.jetbrains.annotations.Nullable;

public class SpotlessCheckinHandler extends CheckinHandler {
  private static final Logger LOGGER = Logger.getInstance(SpotlessCheckinHandler.class);
  private static final String ACTIVATED_OPTION_NAME = "SPOTLESS_PRECOMMIT_FORMATTING";

  private final Project project;
  private final CheckinProjectPanel checkinPanel;
  private JCheckBox checkBox;

  public SpotlessCheckinHandler(Project project, CheckinProjectPanel checkinPanel) {
    this.project = project;
    this.checkinPanel = checkinPanel;
  }

  @Override
  @Nullable
  public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
    this.checkBox = new NonFocusableCheckBox("Reformat Code with Spotless");
    return new MyRefreshableOnComponent(checkBox);
  }

  @Override
  public ReturnResult beforeCheckin(
      @Nullable CommitExecutor executor, PairConsumer<Object, Object> additionalDataConsumer) {
    if (checkBox != null && !checkBox.isSelected()) {
      return ReturnResult.COMMIT;
    }

    try {
      Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
      if (editor != null) {
        Document document = editor.getDocument();
        Optional.ofNullable(PsiDocumentManager.getInstance(project).getPsiFile(document))
            .ifPresent(
                file ->
                    new ReformatCodeProcessor(file, true)
                        .formatFile(file, ProgressExecutionMode.MODAL_SYNC));
        // make sure files are physical saved before commit
        FileDocumentManager.getInstance().saveAllDocuments();
      }
      return ReturnResult.COMMIT;
    } catch (Exception e) {
      handleError(e);
      return ReturnResult.CANCEL;
    }
  }

  private void handleError(Exception e) {
    var msg = "Error while reformatting code with Spotless";
    if (e.getMessage() != null) {
      msg = msg + ": " + e.getMessage();
    }
    LOGGER.info(msg, e);
    Messages.showErrorDialog(project, msg, "Error reformatting code with Spotless");
  }

  private class MyRefreshableOnComponent implements RefreshableOnComponent, UnnamedConfigurable {
    private final JCheckBox checkBox;

    private MyRefreshableOnComponent(JCheckBox checkBox) {
      this.checkBox = checkBox;
    }

    @Override
    public JComponent getComponent() {
      var panel = new JPanel(new BorderLayout());
      panel.add(checkBox);
      return panel;
    }

    @Override
    public void refresh() {}

    @Override
    public void saveState() {
      PropertiesComponent.getInstance(project)
          .setValue(ACTIVATED_OPTION_NAME, Boolean.toString(checkBox.isSelected()));
    }

    @Override
    public void restoreState() {
      checkBox.setSelected(getSavedStateOrDefault());
    }

    private boolean getSavedStateOrDefault() {
      var props = PropertiesComponent.getInstance(project);
      return props.getBoolean(ACTIVATED_OPTION_NAME);
    }

    @Override
    public @Nullable JComponent createComponent() {
      return getComponent();
    }

    @Override
    public boolean isModified() {
      return checkBox.isSelected() != getSavedStateOrDefault();
    }

    @Override
    public void apply() {
      saveState();
    }

    @Override
    public void reset() {
      restoreState();
    }
  }
}
