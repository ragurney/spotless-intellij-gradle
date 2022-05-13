package com.github.ragurney.spotless.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/** Configuration to store whether spotless should be executed on save */
@State(name = "SpotlessConfiguration", storages = @Storage("spotless.xml"))
public class SpotlessConfiguration
    implements PersistentStateComponent<SpotlessConfiguration.State> {

  private static final boolean SPOTLESS_ON_SAVE_DEFAULT = false;
  private @NotNull State state = new State();

  @NotNull
  public static SpotlessConfiguration getInstance(@NotNull Project project) {
    return project.getService(SpotlessConfiguration.class);
  }

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    this.state = state;
  }

  public boolean isRunOnSaveEnabled() {
    return state.myRunOnSave;
  }

  public void setRunOnSave(boolean runOnSave) {
    state.myRunOnSave = runOnSave;
  }

  static class State {
    public boolean myRunOnSave = SPOTLESS_ON_SAVE_DEFAULT;
  }
}
