package gitlab.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import gitlab.api.ApiFacade;
import gitlab.dto.GitlabServerDto;
import gitlab.dto.ProjectDto;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.gitlab.api.models.GitlabProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

    public Collection<GitlabServerDto> gitlabServers = new ArrayList<>();

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
    public Map<GitlabServerDto, Collection<ProjectDto>> loadMapOfServersAndProjects(Collection<GitlabServerDto> servers) {
        Map<GitlabServerDto, Collection<ProjectDto>> map = new HashMap<>();
        for(GitlabServerDto server : servers) {
            Collection<ProjectDto> projects = loadProjects(server);
            map.put(server, projects);
        }
        return map;
    }

    public Collection<ProjectDto> loadProjects(GitlabServerDto server) throws Throwable {
        ApiFacade apiFacade = api(server);

        Collection<ProjectDto> projects = new ArrayList<>();
        for (GitlabProject gitlabProject : apiFacade.getProjects()) {
            ProjectDto projectDto = new ProjectDto();
            projectDto.setName(gitlabProject.getName());
            projectDto.setNamespace(gitlabProject.getNamespace().getName());
            projectDto.setHttpUrl(gitlabProject.getHttpUrl());
            projectDto.setSshUrl(gitlabProject.getSshUrl());
            projects.add(projectDto);
        }
        return projects;

    }

    public void isApiValid(String host, String key) throws IOException {
        ApiFacade apiFacade = new ApiFacade();
        apiFacade.reload(host, key);
        apiFacade.getSession();
    }

    public ApiFacade api(GitlabServerDto serverDto) {
        return new ApiFacade(serverDto.getApiUrl(), serverDto.getApiToken());
    }

    public void addServer(GitlabServerDto server) {
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

    public void deleteServer(GitlabServerDto server) {
        getGitlabServers().stream().filter(s -> server.getApiUrl().equals(s.getApiUrl())).forEach(removedServer -> getGitlabServers().remove(removedServer));
    }
}
