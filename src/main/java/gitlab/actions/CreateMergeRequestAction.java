package gitlab.actions;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import com.github.lvlifeng.githelper.Bundle;
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
import gitlab.bean.*;
import gitlab.helper.GitLabProjectHelper;
import gitlab.helper.UsersHelper;
import gitlab.settings.GitLabSettingsState;
import gitlab.settings.SettingsView;
import gitlab.ui.MergeDialog;
import gitlab.ui.MergeRequestDialog;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabUser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/19 22:12
 */
public class CreateMergeRequestAction extends DumbAwareAction {


    public CreateMergeRequestAction() {
        super("_Create Merge Request...", "Create merge request for projects selected", null);
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

        Set<GitRepository> repositories = Arrays.stream(data).map(o -> manager.getRepositoryForFileQuick(o)).collect(Collectors.toSet());
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
            repSets.stream().forEach(s -> sb.append(s).append("\n"));
            Messages.showInfoMessage("The following Gitlab server is not configured!  Please go to \n" +
                            "'Settings->Version Control->GitLab' to configure.\n\n" +
                            sb.toString(),
                    Bundle.message("gitLab"));
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
                    indicator.setText2(o.getProjectName()+" ("+ index.getAndIncrement() +"/"+ gitLabProjectDtos.size()+")");
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
                                commonBranch = CollectionUtil.disjunction(currentBranchNames, commonBranch).stream().collect(Collectors.toList());
                            }
                        }
                    } catch (IOException ioException) {
                    }
                });
                indicator.setText("Loading users...");
                currentUser = UsersHelper.getCurrentUser(indicator, selectedProjectList, gitLabSettingsState);
                if (indicator.isCanceled()) {
                    return;
                }
                users = UsersHelper.getAllUsers(indicator, gitLabSettingsState);
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

    private void showMessageDialog(){
        Messages.showInfoMessage("No projects to create, please reselect!", Bundle.message("mergeRequestDialogTitle"));
    }
}
