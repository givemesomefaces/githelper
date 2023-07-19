package com.github.itisokey.githelper.gitlab.actions;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.github.itisokey.githelper.gitlab.bean.GitLabProjectDto;
import com.github.itisokey.githelper.gitlab.bean.ProjectDto;
import com.github.itisokey.githelper.gitlab.bean.SelectedProjectDto;
import com.github.itisokey.githelper.gitlab.bean.User;
import com.github.itisokey.githelper.gitlab.helper.GitLabProjectHelper;
import com.github.itisokey.githelper.gitlab.helper.UsersHelper;
import com.github.itisokey.githelper.gitlab.settings.GitLabSettingsState;
import com.github.itisokey.githelper.gitlab.settings.SettingsView;
import com.github.itisokey.githelper.gitlab.ui.MergeRequestDialog;
import com.github.lvlifeng.githelper.Bundle;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import com.github.lvlifeng.githelper.bean.GitlabServer;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabProject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Lv LiFeng
 * @date 2022/1/19 22:12
 */
public class CreateMergeRequestAction extends DumbAwareAction {


    public CreateMergeRequestAction() {
        super("_Create Merge Request...", "Create merge request of GitLab for all selected projects", null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        GitLabSettingsState gitLabSettingsState = GitLabSettingsState.getInstance();
        if (!gitLabSettingsState.hasSettings()) {
            new SettingsView(null).showAndGet();
            if (!gitLabSettingsState.hasSettings()) {
                return;
            }
        }

        Project project = e.getData(CommonDataKeys.PROJECT);
        VirtualFile[] data = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);

        GitRepositoryManager manager = GitUtil.getRepositoryManager(project);

        Set<GitRepository> repositories = Arrays.stream(data).map(manager::getRepositoryForFileQuick).collect(Collectors.toSet());
        if (CollectionUtil.isEmpty(repositories)) {
            showMessageDialog();
            return;
        }
        Set<GitLabProjectDto> gitLabProjectDtos = GitLabProjectHelper.getGitLabProjectDtos(repositories);
        if (CollectionUtil.isEmpty(gitLabProjectDtos)) {
            showMessageDialog();
            return;
        }
        List<GitlabServer> gitlabServers = gitLabSettingsState.getGitlabServers();
        Set<String> repSets = gitLabProjectDtos.stream().map(GitLabProjectDto::getRepUrl).collect(Collectors.toSet());
        repSets.removeAll(gitlabServers.stream().map(GitlabServer::getRepositoryUrl).collect(Collectors.toSet()));
        if (CollectionUtil.isNotEmpty(repSets)) {
            StringBuilder sb = new StringBuilder();
            repSets.forEach(s -> sb.append(s).append("\n"));
            // TODO
            Notifications.Bus.notify(
                    NotificationGroupManager.getInstance().getNotificationGroup(Bundle.message("notifierGroup"))
                            .createNotification(
                                    Bundle.message("gitlabSettings"),
                                    "The following Gitlab server is not configured!" + sb + "' \n" +
                                            " Please click the button below to configure.",
                                    NotificationType.WARNING,
                                    null
                            ).addAction(
                                    new OpenGitLabSettingsAction()
                            )
            );
            return;
        }

        ProgressManager.getInstance().run(new Task.Modal(project, Bundle.message("createMergeRequestDialogTitle"), true) {
            Set<ProjectDto> selectedProjectList = new HashSet<>();
            List<String> commonBranch = new ArrayList<>();
            List<User> currentUser = new ArrayList<>();
            Set<User> users = new HashSet<>();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Loading common branches...");
                AtomicInteger index = new AtomicInteger(1);
                gitLabProjectDtos.stream().filter(o -> !indicator.isCanceled()).forEach(o -> {
                    indicator.setText2("(" + index.getAndIncrement() + "/" + gitLabProjectDtos.size() + ") " + o.getProjectName());
                    GitlabServer gitlabServer = gitlabServers.stream().filter(server -> StringUtils.equals(server.getRepositoryUrl(), o.getRepUrl())).findFirst().orElse(null);
                    if (gitlabServer == null) {
                        return;
                    }
                    try {
                        GitlabProject gitlabProject = gitLabSettingsState.api(gitlabServer).getProjectByNamespaceAndName(o.getNamespace(), o.getProjectName());
                        if (gitlabProject != null) {
                            ProjectDto projectDto = new ProjectDto();
                            BeanUtil.copyProperties(gitlabProject, projectDto);
                            projectDto.setGitlabServer(gitlabServer);
                            selectedProjectList.add(projectDto);
                            List<GitlabBranch> branchesByProject = gitLabSettingsState.api(gitlabServer).getBranchesByProject(gitlabProject);
                            if (CollectionUtil.isNotEmpty(branchesByProject)) {
                                Set<String> currentBranchNames = branchesByProject.stream().map(GitlabBranch::getName).collect(Collectors.toSet());
                                if (CollectionUtil.isEmpty(commonBranch)) {
                                    commonBranch = new ArrayList<>(currentBranchNames);
                                }
                                commonBranch = new ArrayList<>(CollectionUtil.intersectionDistinct(currentBranchNames, commonBranch));
                            }
                        }
                    } catch (IOException ioException) {
                    }
                });
                if (indicator.isCanceled()) {
                    return;
                }
                indicator.setText2(null);
                indicator.setText("Loading users...");
                Set<GitlabServer> gitlabServerSet = selectedProjectList.stream().map(ProjectDto::getGitlabServer).collect(Collectors.toSet());
                currentUser = UsersHelper.getCurrentUser(indicator, gitlabServerSet, gitLabSettingsState);
                if (indicator.isCanceled()) {
                    return;
                }
                users = UsersHelper.getAllUsers(indicator, gitlabServerSet, gitLabSettingsState);
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                if (CollectionUtil.isEmpty(commonBranch)) {
                    Messages.showMessageDialog("No common branches, please reselect project!", Bundle.message("createMergeRequestDialogTitle"), null);
                    return;
                }
                new MergeRequestDialog(project,
                        new SelectedProjectDto()
                                .setGitLabSettingsState(gitLabSettingsState)
                                .setSelectedProjectList(selectedProjectList),
                        commonBranch,
                        currentUser,
                        users
                ).showAndGet();
            }
        });
    }

    private void showMessageDialog() {
        Messages.showInfoMessage("No projects to create, please reselect!", Bundle.message("mergeRequestDialogTitle"));
    }
}
