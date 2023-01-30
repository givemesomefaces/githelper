package gitlab.helper;


import git4idea.repo.GitRepository;
import gitlab.bean.GitLabProjectDto;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lv LiFeng
 * @date 2022/1/28 10:35
 */

public class GitLabProjectHelper {


    public static GitLabProjectDto getGitLabProjectDtoByGitUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        int atIndex = url.indexOf("git@");
        int colonIndex = url.indexOf(":");
        int slashIndex = url.lastIndexOf("/");
        int lastPointIndex = url.indexOf(".git");
        String repUrl = null;
        if (atIndex != -1 && colonIndex != -1) {
            repUrl = url.substring(atIndex + 4, colonIndex);
        }
        if (atIndex != -1 && colonIndex == -1 && slashIndex != -1) {
            repUrl = url.substring(atIndex + 4, slashIndex);
        }
        if (repUrl == null) {
            return null;
        }
        String namespace = null;
        if (colonIndex != -1 && slashIndex != -1) {
            namespace = url.substring(colonIndex + 1, slashIndex);
        }
        String projectName = null;
        if (slashIndex != -1 && lastPointIndex != -1) {
            projectName = url.substring(slashIndex + 1, lastPointIndex);
        }
        if (projectName == null) {
            return null;
        }
        return new GitLabProjectDto().setNamespace(namespace).setProjectName(projectName).setRepUrl(repUrl);
    }

    @NotNull
    public static Set<GitLabProjectDto> getGitLabProjectDtos(Set<GitRepository> repositories) {
        Set<GitLabProjectDto> gitLabProjectDtos = repositories.stream()
                .map(s -> s.getInfo()
                        .getRemotes()
                        .stream()
                        .map(o -> GitLabProjectHelper.getGitLabProjectDtoByGitUrl(o.getFirstUrl()))
                        .collect(Collectors.toSet()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        return gitLabProjectDtos;
    }
}
