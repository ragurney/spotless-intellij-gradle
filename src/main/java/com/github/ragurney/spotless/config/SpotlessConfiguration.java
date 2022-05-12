package com.github.ragurney.spotless.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration to store whether spotless should be executed on save
 */
@State(name = "SpotlessConfiguration", storages = @Storage("spotless.xml"))
public class SpotlessConfiguration implements PersistentStateComponent<SpotlessConfiguration.State> {


    private static final boolean SPOTLESS_ON_SAVE_DEFAULT = false;
    private @NotNull State myState = new State();

    @NotNull
    public static SpotlessConfiguration getInstance(@NotNull Project project) {
        return project.getService(SpotlessConfiguration.class);
    }

    @Override
    public @NotNull State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public boolean isRunOnSave() {
        return myState.myRunOnSave;
    }

    public void setRunOnSave(boolean runOnSave) {
        myState.myRunOnSave = runOnSave;
    }

    static class State {
        public boolean myRunOnSave = SPOTLESS_ON_SAVE_DEFAULT;
    }

}
