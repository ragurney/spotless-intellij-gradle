package com.github.ragurney.spotless.formatters;

import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.formatting.service.AsyncDocumentFormattingService;
import com.intellij.formatting.service.AsyncFormattingRequest;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.Version;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.util.IncorrectOperationException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.FutureTask;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;


public class SpotlessFormattingService extends AsyncDocumentFormattingService {
  private static final Logger LOG = Logger.getInstance(SpotlessFormattingService.class);
  private static final Version NO_CONFIG_CACHE_MIN_GRADLE_VERSION = new Version(6, 6, 0);
  @Override
  protected @Nullable FormattingTask createFormattingTask(@NotNull AsyncFormattingRequest formattingRequest) {
    FutureTask<Boolean> formattingTask = createTask(formattingRequest);

    return new FormattingTask() {
      @Override
      public boolean cancel() {
        return formattingTask.cancel(/* mayInterruptIfRunning= */ true);
      }

      @Override
      public void run() {
        formattingTask.run();
      }
    };
  }

  @Override
  protected @NotNull String getNotificationGroupId() {
    return null;
  }

  @Override
  protected @NotNull @NlsSafe String getName() {
    return "Spotless";
  }

  @Override
  public @NotNull Set<Feature> getFeatures() {
    return null;
  }

  @Override
  public boolean canFormat(@NotNull PsiFile file) {
    return true;
  }

  private FutureTask<Boolean> createTask(AsyncFormattingRequest formattingRequest) {
    Project project = formattingRequest.getContext().getProject();
    PsiFile file = formattingRequest.getContext().getContainingFile();

    return new FutureTask<>(() -> {
      try {
        PsiFile fileToProcess = ensureValid(file);
        if (fileToProcess == null) {
          return false;
        }

        Document document = PsiDocumentManager.getInstance(project).getDocument(fileToProcess);

        LOG.assertTrue(document != null);

        reformatFilePreservingScrollPosition(fileToProcess, project);

        return true;
      } catch (IncorrectOperationException e) {
        LOG.error(e);
        return false;
      }
    });
  }

  /**
   * Executes the spotlessApply task for the current file, using the {@link EditorScrollingPositionKeeper} to maintain
   * scroll position. The gradle task is executed asynchronously in the background of the IDE.
   *
   * @param fileToProcess the file to format using spotlessApply
   */
  private void reformatFilePreservingScrollPosition(PsiFile fileToProcess, Project project) {
      // Constructs execution settings, setting the gradle task and its options
      ExternalSystemTaskExecutionSettings settings = constructTaskExecutionSettings(fileToProcess, project);

      // Execute gradle task
      ExternalSystemUtil.runTask(settings, DefaultRunExecutor.EXECUTOR_ID, project, GradleConstants.SYSTEM_ID, null,
          ProgressExecutionMode.IN_BACKGROUND_ASYNC, false);
  }

  private static void assertFileIsValid(@NotNull PsiFile file) {
    if (!file.isValid()) {
      LOG.error("Invalid Psi file, name: " + file.getName() + " , class: " + file.getClass().getSimpleName() + " , "
          + PsiInvalidElementAccessException.findOutInvalidationReason(file));
    }
  }

  /**
   * Constructs the settings needed for running the gradle `spotlessApply` task with the
   * <a href="https://github.com/diffplug/spotless/blob/master/plugin-gradle/IDE_HOOK.md">IDE hook</a> to format the
   * currently selected file.
   *
   * {@link ExternalSystemUtil}
   * @param fileToProcess the file selected to format
   * @param project the project of the file
   * @return {@link ExternalSystemTaskExecutionSettings} populated with spotlessApply task and IDE flag set
   */
  @NotNull
  private ExternalSystemTaskExecutionSettings constructTaskExecutionSettings(PsiFile fileToProcess, Project project) {
    String noConfigCacheOption = shouldAddNoConfigCacheOption(project) ? " --no-configuration-cache" : "";

    ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
    settings.setExternalProjectPath(project.getBasePath());
    settings.setTaskNames(List.of("spotlessApply"));
    settings.setScriptParameters(
        String.format("-PspotlessIdeHook=\"%s\"%s", fileToProcess.getVirtualFile().getPath(), noConfigCacheOption));
    settings.setVmOptions("");
    settings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.getId());
    return settings;
  }

  private boolean shouldAddNoConfigCacheOption(Project project) {
    Optional<GradleProjectSettings> maybeProjectSettings = Optional.ofNullable(GradleSettings.getInstance(project)
        .getLinkedProjectSettings(Objects.requireNonNull(project.getBasePath())));

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
}
