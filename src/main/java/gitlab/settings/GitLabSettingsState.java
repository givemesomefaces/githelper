package gitlab.settings;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import gitlab.api.GitlabRestApi;
import gitlab.bean.GitlabServer;
import gitlab.bean.ProjectDto;
import gitlab.common.Notifier;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.SocketException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/7 00:13
 */
@Getter
@Setter
@Accessors(chain = true)
@State(
        name = "SettingsState",
        storages = {
                @Storage("$APP_CONFIG$/gitlab-settings-persistentstate.xml")
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

    @SneakyThrows
    public Map<GitlabServer, List<ProjectDto>> loadMapOfServersAndProjects(List<GitlabServer> servers) {
        Map<GitlabServer, List<ProjectDto>> map = new HashMap<>();
        for(GitlabServer server : servers) {
            List<ProjectDto> projects = loadProjects(server);
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
        try {
            isApiValid(serverDto.getApiUrl(), serverDto.getApiToken());
        } catch (IOException e) {
            Notifier.notifyError(null, "The GitLab server error occurred! (" + serverDto.getApiUrl() + ")\n " + e.getMessage());
        }
        return new GitlabRestApi(serverDto.getApiUrl(), serverDto.getApiToken());
    }

    public void addServer(GitlabServer server) {
        if(getGitlabServers().stream().noneMatch(s -> server.getApiUrl().equals(s.getApiUrl()))) {
            getGitlabServers().add(server);
        } else {
            getGitlabServers().stream().filter(s -> Objects.nonNull(server) && server.getApiUrl().equals(s.getApiUrl())).forEach(changedServer -> {
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

    public boolean hasSettings(){
        return CollectionUtil.isNotEmpty(getGitlabServers());
    }

    public List<GitlabServer> getGitlabServers() {
        gitlabServers = gitlabServers.stream().filter(Objects::nonNull).collect(Collectors.toList());
        return gitlabServers;
    }

    public GitlabServer getGitlabServer(GitRepository gitRepository) {
        for (GitRemote gitRemote : gitRepository.getRemotes()) {
            for (String remoteUrl : gitRemote.getUrls()) {
                for(GitlabServer server : getGitlabServers()) {
                    if(remoteUrl.contains(server.getRepositoryUrl())) {
                        return server;
                    }
                }
            }
        }
        return null;
    }
}
