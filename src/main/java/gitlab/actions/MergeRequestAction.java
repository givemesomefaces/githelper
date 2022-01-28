package gitlab.actions;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.github.lvlifeng.githelper.Bundle;
import com.google.common.collect.Sets;
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
import gitlab.settings.GitLabSettingsState;
import gitlab.settings.SettingsView;
import gitlab.ui.MergeDialog;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.o;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/19 22:12
 */
public class MergeRequestAction extends DumbAwareAction {


    public MergeRequestAction() {
        super("_Merge Request...", "Create merge request for projects all selected", null);
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
            Messages.showMessageDialog("The following Gitlab server is not configured!  Please go to \n" +
                            "'Settings->Version Control->GitLab' to configure.\n\n" +
                            sb.toString(),
                    Bundle.message("gitLab"), null);
            return;
        }

        ProgressManager.getInstance().run(new Task.Modal(project, Bundle.message("mergeRequestDialogTitle"), true) {
            Set<ProjectDto> selectedProjectList = new HashSet<>();
            Set<MergeRequest> requests = new HashSet<>();
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Loading merge requests...");
                AtomicInteger index = new AtomicInteger(1);
                requests = gitLabProjectDtos.stream().filter(o -> !indicator.isCanceled()).map(o -> {
                    indicator.setText2(o.getProjectName()+" ("+ index.getAndIncrement() +"/"+ gitLabProjectDtos.size()+")");
                    Set<MergeRequest> mergeRequests = new HashSet<>();
                    GitlabServer gitlabServer = gitlabServers.stream().filter(server -> StringUtils.equals(server.getRepositoryUrl(), o.getRepUrl())).findFirst().orElse(null);
                    if (gitlabServer == null) {
                        return mergeRequests;
                    }
                    try {
                        GitlabProject gitlabProject = gitLabSettingsState.api(gitlabServer).getProjectByNamespaceAndName(o.getNamespace(), o.getProjectName());
                        if (gitlabProject != null) {
                            ProjectDto projectDto = new ProjectDto();
                            BeanUtil.copyProperties(gitlabProject, projectDto);
                            projectDto.setGitlabServer(gitlabServer);
                            selectedProjectList.add(projectDto);
                            List<GitlabMergeRequest> openMergeRequest = gitLabSettingsState.api(gitlabServer).getOpenMergeRequest(gitlabProject.getId());
                            if (CollectionUtil.isNotEmpty(openMergeRequest)) {
                                mergeRequests = openMergeRequest.stream().map(u -> {
                                    MergeRequest m = new MergeRequest();
                                    BeanUtil.copyProperties(u, m);
                                    m.setProjectName(o.getProjectName());
                                    m.setGitlabServer(gitlabServer);
                                    return m;
                                }).collect(Collectors.toSet());
                            }
                        }
                    } catch (IOException ioException) {
                        mergeRequests = new HashSet<>();
                    }
                    return mergeRequests;
                }).flatMap(Collection::stream)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                indicator.setText("Loading merge requests...");
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                if (CollectionUtil.isEmpty(requests)) {
                    showMessageDialog();
                    return;
                }

                new MergeDialog(project,
                        new SelectedProjectDto()
                                .setGitLabSettingsState(gitLabSettingsState)
                                .setSelectedProjectList(selectedProjectList),
                        new ArrayList<>(requests)
                ).showAndGet();
            }
        });
    }

    private void showMessageDialog(){
        Messages.showMessageDialog("No merge requests to merge!", Bundle.message("mergeRequestDialogTitle"), null);
    }
}
