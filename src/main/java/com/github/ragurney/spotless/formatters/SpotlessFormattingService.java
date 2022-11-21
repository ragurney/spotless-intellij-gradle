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
import com.intellij.openapi.externalSystem.task.TaskCallback;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.Version;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.util.IncorrectOperationException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;


public class SpotlessFormattingService extends AsyncDocumentFormattingService {
  private static final Logger LOG = Logger.getInstance(SpotlessFormattingService.class);
  private static final Version NO_CONFIG_CACHE_MIN_GRADLE_VERSION = new Version(6, 6, 0);

  public static final String NOTIFICATION_GROUP_ID = "INTELLIJ GRADLE SPOTLESS";

  private static final Set<Feature> FEATURES = EnumSet.noneOf(Feature.class);

  @Override
  protected @Nullable FormattingTask createFormattingTask(@NotNull AsyncFormattingRequest formattingRequest) {
    Semaphore done = new Semaphore(1);

    return new FormattingTask() {
      @Override
      public boolean isRunUnderProgress() {
        return true;
      }

      @Override
      public boolean cancel() {
        return false;
      }

      @Override
      public void run() {
        System.out.println("hello");
        try {
          done.acquire();
        } catch (InterruptedException e) {
          LOG.warn("Tried to reformat when reformatting is already underway.");
          return;
        }

        Project project = formattingRequest.getContext().getProject();
        TextRange textRange = formattingRequest.getFormattingRanges().get(0);
        PsiFile file = formattingRequest.getContext().getContainingFile();

        String textToFormat = file.getViewProvider().getDocument().getText(textRange);

        try {
          PsiFile fileToProcess = ensureValid(file);
          if (fileToProcess == null) {
            return;
          }

          Document document = PsiDocumentManager.getInstance(project).getDocument(fileToProcess);
          LOG.assertTrue(document != null);

          reformatFilePreservingScrollPosition(textToFormat,fileToProcess, project, () -> {
            formattingRequest.onTextReady("");
            done.release();
          });
        } catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }
    };
  }

  @Override
  protected @NotNull String getNotificationGroupId() {
    return NOTIFICATION_GROUP_ID;
  }

  @Override
  protected @NotNull @NlsSafe String getName() {
    return "Spotless";
  }

  @Override
  public @NotNull Set<Feature> getFeatures() {
    return FEATURES;
  }

  @Override
  public boolean canFormat(@NotNull PsiFile file) {
    return true;
  }

  /**
   * Executes the spotlessApply task for the current file, using the {@link EditorScrollingPositionKeeper} to maintain
   * scroll position. The gradle task is executed asynchronously in the background of the IDE.
   *
   * @param fileToProcess the file to format using spotlessApply
   */
  private void reformatFilePreservingScrollPosition(String textToFormat, PsiFile fileToProcess, Project project, Runnable callback) {
    // Constructs execution settings, setting the gradle task and its options
    ExternalSystemTaskExecutionSettings settings = constructTaskExecutionSettings(textToFormat, fileToProcess, project);

    // Execute gradle task
    ExternalSystemUtil.runTask(settings, DefaultRunExecutor.EXECUTOR_ID, project, GradleConstants.SYSTEM_ID,
        new TaskCallback() {
          @Override
          public void onSuccess() {
            callback.run();
          }

          @Override
          public void onFailure() {
            callback.run();
          }
        }, ProgressExecutionMode.IN_BACKGROUND_ASYNC, false);
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
  private ExternalSystemTaskExecutionSettings constructTaskExecutionSettings(String textToFormat, PsiFile fileToProcess, Project project) {
    ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
    settings.setExternalProjectPath(project.getBasePath());
    settings.setTaskNames(List.of("spotlessApply"));

    //TODO: SET STDIN???!!!

    List<String> paramList = new ArrayList<>();
    // Set the file to execute Spotless on
    paramList.add(String.format("-PspotlessIdeHook=\"%s\"", fileToProcess.getVirtualFile().getPath()));
    // Set no config cache option if necessary, determined by Gradle version
    if (shouldAddNoConfigCacheOption(project)) {
      paramList.add("--no-configuration-cache");
    }
    // Tell Spotless to use stdIn to support format by range
    paramList.add("-PspotlessIdeHookUseStdIn");

    settings.setScriptParameters(String.join(" ", paramList));
    settings.setVmOptions("");
    settings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.getId());
    return settings;
  }

  private boolean shouldAddNoConfigCacheOption(Project project) {
    Optional<GradleProjectSettings> maybeProjectSettings = Optional.ofNullable(
        GradleSettings.getInstance(project).getLinkedProjectSettings(Objects.requireNonNull(project.getBasePath())));

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
