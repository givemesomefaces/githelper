package gitlab;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializerUtil;
import git4idea.repo.GitRepository;
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
public class GitLabState implements PersistentStateComponent<GitLabState> {

    public String host;

    public String token;

    public boolean defaultRemoveBranch;

    public Collection<ProjectDto> projects = new ArrayList<>();

    public Collection<GitlabServerDto> gitlabServers = new ArrayList<>();

    @Override
    public @Nullable GitLabState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull GitLabState state) {
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

    public Collection<ProjectDto> getProjects() {
        return projects;
    }

    public void setProjects(Collection<ProjectDto> projects) {
        this.projects = projects;
    }

    public ApiFacade api(GitlabServerDto serverDto) {
        return new ApiFacade(serverDto.getApiUrl(), serverDto.getApiToken());
    }
}
