package gitlab.settings;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import gitlab.api.GitlabRestApi;
import gitlab.bean.GitlabServer;
import gitlab.bean.ProjectDto;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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

    public String host;

    public String token;

    public boolean defaultRemoveBranch;

    public Collection<GitlabServer> gitlabServers = new ArrayList<>();

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
    public Map<GitlabServer, Collection<ProjectDto>> loadMapOfServersAndProjects(Collection<GitlabServer> servers) {
        Map<GitlabServer, Collection<ProjectDto>> map = new HashMap<>();
        for(GitlabServer server : servers) {
            Collection<ProjectDto> projects = loadProjects(server);
            map.put(server, projects);
        }
        return map;
    }

    public Collection<ProjectDto> loadProjects(GitlabServer server) throws Throwable {
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
        return new GitlabRestApi(serverDto.getApiUrl(), serverDto.getApiToken());
    }

    public void addServer(GitlabServer server) {
        if(getGitlabServers().stream().noneMatch(s -> server.getApiUrl().equals(s.getApiUrl()))) {
            getGitlabServers().add(server);
        } else {
            getGitlabServers().stream().filter(s -> server.getApiUrl().equals(s.getApiUrl())).forEach(changedServer -> {
                changedServer.setApiUrl(server.getApiUrl());
                changedServer.setRepositoryUrl(server.getRepositoryUrl());
                changedServer.setApiToken(server.getApiToken());
                changedServer.setPreferredConnection(server.getPreferredConnection());
                changedServer.setRemoveSourceBranch(server.isRemoveSourceBranch());
            });
        }
    }

    public void deleteServer(GitlabServer server) {
        getGitlabServers().stream().filter(s -> server.getApiUrl().equals(s.getApiUrl())).forEach(removedServer -> getGitlabServers().remove(removedServer));
    }

    public boolean hasSettings(){
        return CollectionUtil.isNotEmpty(getGitlabServers());
    }
}
