package gitlab.actions;

import cn.hutool.core.collection.CollectionUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.progress.util.TooManyUsagesStatus;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import gitlab.bean.ProjectDto;
import gitlab.settings.GitLabSettingsState;
import gitlab.settings.SettingsView;
import gitlab.ui.GitLabDialog;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/6 20:10
 */
public class GitLabAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {

        Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        GitLabSettingsState gitLabSettingsState = GitLabSettingsState.getInstance();
        if (gitLabSettingsState.hasSettings()) {
            showGitLabDialog(project, gitLabSettingsState);
        } else {
            boolean b = new SettingsView(null).showAndGet();
            if (gitLabSettingsState.hasSettings()) {
                showGitLabDialog(project, gitLabSettingsState);
            }
        }

    }
    private void showGitLabDialog(Project project, GitLabSettingsState gitLabSettingsState){
        ProgressManager.getInstance().run(new Task.Modal(project, "GitLab", true) {
            List<ProjectDto> projectDtoList = new ArrayList<>();
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Loading projects...");
                projectDtoList = gitLabSettingsState.loadMapOfServersAndProjects(gitLabSettingsState.getGitlabServers())
                        .values()
                        .stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
                if (CollectionUtil.isEmpty(projectDtoList)) {
                    return;
                }
                indicator.setText("Projects loaded");
            }

            @Override
            public void onCancel() {
                super.onCancel();
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                new GitLabDialog(project, projectDtoList).showAndGet();
            }
        });
    }
}
