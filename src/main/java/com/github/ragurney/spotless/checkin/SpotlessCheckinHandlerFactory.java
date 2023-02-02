package com.github.ragurney.spotless.checkin;

import com.github.ragurney.spotless.actions.ReformatCodeProcessor;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.changes.ui.BooleanCommitOption;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class SpotlessCheckinHandlerFactory extends CheckinHandlerFactory {
	@Override
	public @NotNull CheckinHandler createHandler(@NotNull CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
		return new CheckinHandler() {
			private boolean spotlessApplyBeforeCommit = false;

			@Override
			public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
				return new BooleanCommitOption(panel, "Format with Spotless before commit", false, this::isSpotlessApplyBeforeCommit,
						this::setSpotlessApplyBeforeCommit);
			}

			public boolean isSpotlessApplyBeforeCommit() {
				return spotlessApplyBeforeCommit;
			}

			public void setSpotlessApplyBeforeCommit(boolean spotlessApplyBeforeCommit) {
				this.spotlessApplyBeforeCommit = spotlessApplyBeforeCommit;
			}

			@Override
			public ReturnResult beforeCheckin() {
				if (isSpotlessApplyBeforeCommit()) {
					List<PsiFile> files = getFiles();
					if (!files.isEmpty()) {
						files.forEach(file -> new ReformatCodeProcessor(file).run());
					}
				}
				return super.beforeCheckin();
			}

			List<PsiFile> getFiles() {
				PsiManager manager = PsiManager.getInstance(panel.getProject());
				return panel
						.getVirtualFiles()
						.stream()
						.map(manager::findFile)
						.filter(file -> file != null && file.isWritable() && !file.isDirectory())
						.collect(Collectors.toList());
			}
		};
	}
}
