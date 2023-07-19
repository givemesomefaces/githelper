package com.github.itisokey.githelper.gitlab.settings;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.github.lvlifeng.githelper.Bundle;
import com.github.lvlifeng.githelper.bean.GitlabServer;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import com.github.itisokey.githelper.gitlab.actions.OpenGitLabSettingsAction;
import com.github.itisokey.githelper.gitlab.api.GitlabRestApi;
import com.github.itisokey.githelper.gitlab.bean.ProjectDto;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Lv LiFeng
 * @date 2022/1/7 00:13
 */
@State(
        name = "GitHelperGitLabSettingsState",
        storages = {
                @Storage("$APP_CONFIG$/githelper-gitlab-settings.xml")
        }
)
public class GitLabSettingsState implements PersistentStateComponent<GitLabSettingsState> {

    private String host;

    private String token;

    private boolean defaultRemoveBranch;

    private List<GitlabServer> gitlabServers = new ArrayList<>();

    public static GitLabSettingsState getInstance() {
        return ServiceManager.getService(GitLabSettingsState.class);
    }

    @Override
    public @Nullable GitLabSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull GitLabSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public Map<GitlabServer, List<ProjectDto>> loadMapOfServersAndProjects(List<GitlabServer> servers) {
        Map<GitlabServer, List<ProjectDto>> map = new HashMap<>();
        for (GitlabServer server : servers) {
            List<ProjectDto> projects = null;
            try {
                projects = loadProjects(server);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            map.put(server, projects);
        }
        return map;
    }

    public List<ProjectDto> loadProjects(GitlabServer server) throws Throwable {
        GitlabRestApi gitlabRestApi = api(server);

        return gitlabRestApi.getProjects().stream().map(o -> {
            ProjectDto projectDto = new ProjectDto();
            BeanUtil.copyProperties(o, projectDto);
            projectDto.setGitlabServer(server);
            return projectDto;
        }).collect(Collectors.toList());
    }

    public void isApiValid(String host, String key) throws IOException {
        GitlabRestApi gitlabRestApi = new GitlabRestApi();
        gitlabRestApi.reload(host, key);
        gitlabRestApi.getSession();
    }

    public GitlabRestApi api(GitlabServer serverDto) {
        String apiUrl = serverDto.getApiUrl();
        try {
            isApiValid(serverDto.getApiUrl(), serverDto.getApiToken());
        } catch (Exception e) {
            serverDto.setValidFlag(false);
            String errorMsg = e.getMessage();
            Notifications.Bus.notify(
                    NotificationGroupManager.getInstance().getNotificationGroup(Bundle.message("notifierGroup"))
                            .createNotification(
                                    Bundle.message("gitlabSettings"),
                                    "GitLab server \"" + apiUrl + "\" is invalid. The reason is '" + errorMsg + "' \n" +
                                            " Please click the button below to configure.",
                                    NotificationType.WARNING,
                                    null
                            ).addAction(
                                    new OpenGitLabSettingsAction()
                            )
            );
        }
        return new GitlabRestApi(serverDto.getApiUrl(), serverDto.getApiToken());
    }

    public void addServer(GitlabServer server) {
        if (getGitlabServers().stream().noneMatch(s -> server.getApiUrl().equals(s.getApiUrl()))) {
            getGitlabServers().add(server);
        } else {
            getGitlabServers().stream().filter(s -> server.getApiUrl().equals(s.getApiUrl())).forEach(changedServer -> {
                changedServer.setApiUrl(server.getApiUrl());
                changedServer.setRepositoryUrl(server.getRepositoryUrl());
                changedServer.setApiToken(server.getApiToken());
                changedServer.setPreferredConnection(server.getPreferredConnection());
            });
        }
    }

    public void deleteServer(GitlabServer server) {
        Iterator<GitlabServer> it = gitlabServers.iterator();
        while (it.hasNext()) {
            GitlabServer next = it.next();
            if (Objects.isNull(next) || StringUtils.equalsIgnoreCase(server.getApiUrl(), next.getApiUrl())) {
                it.remove();
            }
        }
    }

    public boolean hasSettings() {
        return CollectionUtil.isNotEmpty(getGitlabServers());
    }

    public List<GitlabServer> getGitlabServers() {
        gitlabServers = gitlabServers.stream().filter(Objects::nonNull).collect(Collectors.toList());
        return gitlabServers;
    }

    public GitlabServer getGitlabServer(GitRepository gitRepository) {
        for (GitRemote gitRemote : gitRepository.getRemotes()) {
            for (String remoteUrl : gitRemote.getUrls()) {
                for (GitlabServer server : getGitlabServers()) {
                    if (remoteUrl.contains(server.getRepositoryUrl())) {
                        return server;
                    }
                }
            }
        }
        return null;
    }
}
