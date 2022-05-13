package com.github.ragurney.spotless.config;

import com.intellij.ide.actionsOnSave.ActionOnSaveComment;
import com.intellij.ide.actionsOnSave.ActionOnSaveContext;
import com.intellij.ide.actionsOnSave.ActionOnSaveInfo;
import com.intellij.ide.actionsOnSave.ActionOnSaveInfoProvider;
import com.intellij.openapi.util.NlsContexts;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class SpotlessOnSaveInfoProvider extends ActionOnSaveInfoProvider {
  @Override
  protected @NotNull Collection<? extends ActionOnSaveInfo> getActionOnSaveInfos(
      @NotNull ActionOnSaveContext context) {
    return Collections.singleton(new SpotlessActionOnSaveInfo(context));
  }

  static class SpotlessActionOnSaveInfo extends ActionOnSaveInfo {

    private boolean isActionOnSaveEnabled;

    protected SpotlessActionOnSaveInfo(@NotNull ActionOnSaveContext context) {
      super(context);
      isActionOnSaveEnabled = SpotlessConfiguration.getInstance(getProject()).isRunOnSaveEnabled();
    }

    @Override
    protected void apply() {
      SpotlessConfiguration.getInstance(getProject()).setRunOnSave(isActionOnSaveEnabled);
    }

    @Override
    protected boolean isModified() {
      return isActionOnSaveEnabled
          != SpotlessConfiguration.getInstance(getProject()).isRunOnSaveEnabled();
    }

    @Override
    public @NotNull @NlsContexts.Checkbox String getActionOnSaveName() {
      return "Run spotless";
    }

    @Override
    public boolean isActionOnSaveEnabled() {
      return isActionOnSaveEnabled;
    }

    @Override
    public void setActionOnSaveEnabled(boolean enabled) {
      isActionOnSaveEnabled = enabled;
    }

    @Override
    public @Nullable ActionOnSaveComment getComment() {
      return ActionOnSaveComment.info("Runs spotless apply on changed files");
    }
  }
}
