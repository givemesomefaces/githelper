package gitlab.actions;


import com.github.lvlifeng.githelper.Bundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import gitlab.helper.ActionHelper;
import gitlab.settings.SettingsComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 *
 *
 * @author Lv Lifeng
 * @date 2023-04-02 18:02
 */
public class OpenGitLabSettingsAction extends AnAction implements DumbAware {


    private static final Icon SETTINGS_ICON = AllIcons.General.Settings;

    public OpenGitLabSettingsAction() {
        super(Bundle.message("notify_gitlab_settings_name"), "Edit the GitLab settings for the current project", SETTINGS_ICON);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ActionHelper.getProject(event).ifPresent(OpenGitLabSettingsAction::showSettingsFor);
    }

    private static void showSettingsFor(Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsComponent.class);
    }
}
