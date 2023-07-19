package com.github.itisokey.githelper.gitlab.bean;

import com.github.lvlifeng.githelper.bean.GitlabServer;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabProject;

import java.util.Objects;

/**
 * @author Lv LiFeng
 * @date 2022/1/7 00:14
 */

public class ProjectDto extends GitlabProject {

    private GitlabServer gitlabServer;

    @Override
    public String toString() {
        return this.getName() + "  (" + getWebUrl().replace(getName(), "") + ")";
    }

    @Override
    public boolean equals(Object o) {
        return StringUtils.equalsIgnoreCase(o.toString(), this.toString()) && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gitlabServer);
    }

    public String getGitlabServerRepositoryUrl() {
        return this.getGitlabServer().getRepositoryUrl();
    }

    public GitlabServer getGitlabServer() {
        return gitlabServer;
    }

    public void setGitlabServer(GitlabServer gitlabServer) {
        this.gitlabServer = gitlabServer;
    }
}
