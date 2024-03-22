package com.github.ragurney.spotless.trigger;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;

public class SpotlessCheckinHandlerFactory extends CheckinHandlerFactory {

  @NotNull
  @Override
  public CheckinHandler createHandler(
      @NotNull CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
    var project = panel.getProject();
    return new SpotlessCheckinHandler(project, panel);
  }
}
