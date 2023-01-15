package com.github.ragurney.spotless.actions;

import com.intellij.CodeStyleBundle;
import com.intellij.codeInsight.actions.AbstractLayoutCodeProcessor;
import com.intellij.codeInsight.actions.LayoutCodeInfoCollector;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.Version;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SlowOperations;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;


/**
 * A code processor used to execute the spotlessApply gradle task on the current file to reformat it
 */
public class ReformatCodeProcessor extends AbstractLayoutCodeProcessor {
  private static final Logger LOG = Logger.getInstance(ReformatCodeProcessor.class);
  private static final Version NO_CONFIG_CACHE_MIN_GRADLE_VERSION = new Version(6, 6, 0);
  private boolean isReformatAllEnabled = false;

  public ReformatCodeProcessor(@NotNull PsiFile file) {
    super(file.getProject(), file, getProgressText(), getCommandName(), true);
  }

  public ReformatCodeProcessor(@NotNull PsiFile file, boolean reformatAll) {
    super(file.getProject(), file, getProgressText(), getCommandName(), true);
    this.isReformatAllEnabled = reformatAll;
  }

  @Override
  protected @NotNull FutureTask<Boolean> prepareTask(@NotNull PsiFile file, boolean processChangedTextOnly)
      throws IncorrectOperationException {
    return new FutureTask<>(() -> {
      try {
        PsiFile fileToProcess = ensureValid(file);
        if (fileToProcess == null) {
          return false;
        }

        Document document = PsiDocumentManager.getInstance(myProject).getDocument(fileToProcess);

        final LayoutCodeInfoCollector infoCollector = getInfoCollector();
        LOG.assertTrue(infoCollector == null || document != null);

        reformatFilePreservingScrollPosition(fileToProcess, document);

        return true;
      } catch (IncorrectOperationException e) {
        LOG.error(e);
        return false;
      }
    });
  }

  /**
   * Executes the spotlessApply task for the current file, using the {@link EditorScrollingPositionKeeper} to maintain scroll position.
   * The gradle task is executed asynchronously in the background of the IDE.
   *
   * @param fileToProcess the file to format using spotlessApply
   * @param document the {@link Document} of the file
   */
  private void reformatFilePreservingScrollPosition(PsiFile fileToProcess, Document document) {
    EditorScrollingPositionKeeper.perform(document, true, () -> SlowOperations.allowSlowOperations(() -> {
      assertFileIsValid(fileToProcess);

      // Constructs execution settings, setting the gradle task and its options
      ExternalSystemTaskExecutionSettings settings = constructTaskExecutionSettings(fileToProcess);

      // Execute gradle task
      ExternalSystemUtil.runTask(settings, DefaultRunExecutor.EXECUTOR_ID, myProject, GradleConstants.SYSTEM_ID, null,
          ProgressExecutionMode.IN_BACKGROUND_ASYNC, false);
    }));
  }

  /**
   * Constructs the settings needed for running the gradle `spotlessApply` task with the
   * <a href="https://github.com/diffplug/spotless/blob/master/plugin-gradle/IDE_HOOK.md">IDE hook</a> to format the
   * currently selected file.
   *
   * {@link ExternalSystemUtil}
   * @param fileToProcess the file selected to format
   * @return {@link ExternalSystemTaskExecutionSettings} populated with spotlessApply task and IDE flag set
   */
  @NotNull
  private ExternalSystemTaskExecutionSettings constructTaskExecutionSettings(PsiFile fileToProcess) {
    String noConfigCacheOption = shouldAddNoConfigCacheOption() ? " --no-configuration-cache" : "";

    ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
    settings.setExternalProjectPath(myProject.getBasePath());
    settings.setTaskNames(List.of("spotlessApply"));
    if (!isReformatAllEnabled) {
      settings.setScriptParameters(
          String.format("-PspotlessIdeHook=\"%s\"%s", fileToProcess.getVirtualFile().getPath(), noConfigCacheOption));
    }
    settings.setVmOptions("");
    settings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.getId());
    return settings;
  }

  private boolean shouldAddNoConfigCacheOption() {
    Optional<GradleProjectSettings> maybeProjectSettings = Optional.ofNullable(GradleSettings.getInstance(myProject)
        .getLinkedProjectSettings(Objects.requireNonNull(myProject.getBasePath())));

    if (maybeProjectSettings.isEmpty()) {
      LOG.warn("Unable to parse linked project settings, leaving off `--no-configuration-cache` argument");
      return false;
    }

    GradleVersion gradleVersion = maybeProjectSettings.get().resolveGradleVersion();

    return Objects.requireNonNull(Version.parseVersion(gradleVersion.getVersion()))
        .isOrGreaterThan(NO_CONFIG_CACHE_MIN_GRADLE_VERSION.major, NO_CONFIG_CACHE_MIN_GRADLE_VERSION.minor);
  }

  @Nullable
  private static PsiFile ensureValid(@NotNull PsiFile file) {
    if (file.isValid()) {
      return file;
    }

    VirtualFile virtualFile = file.getVirtualFile();
    if (!virtualFile.isValid()) {
      return null;
    }

    FileViewProvider provider = file.getManager().findViewProvider(virtualFile);
    if (provider == null) {
      return null;
    }

    Language language = file.getLanguage();
    return provider.hasLanguage(language) ? provider.getPsi(language) : provider.getPsi(provider.getBaseLanguage());
  }

  private static void assertFileIsValid(@NotNull PsiFile file) {
    if (!file.isValid()) {
      LOG.error("Invalid Psi file, name: " + file.getName() + " , class: " + file.getClass().getSimpleName() + " , "
          + PsiInvalidElementAccessException.findOutInvalidationReason(file));
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  private static @NlsContexts.ProgressText String getProgressText() {
    return CodeStyleBundle.message("reformat.progress.common.text");
  }

  @SuppressWarnings("UnstableApiUsage")
  public static @NlsContexts.Command String getCommandName() {
    return CodeStyleBundle.message("process.reformat.code");
  }
}
