package com.github.itisokey.githelper.gitlab.bean;


import java.io.Serializable;
import java.util.Objects;

/**
 * @author Lv LiFeng
 * @date 2022/1/28 10:20
 */
public class GitLabProjectDto implements Serializable {
    private String namespace;
    private String projectName;
    private String repUrl;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitLabProjectDto that = (GitLabProjectDto) o;
        return Objects.equals(namespace, that.namespace) && Objects.equals(projectName, that.projectName) && Objects.equals(repUrl, that.repUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, projectName, repUrl);
    }

    public String getNamespace() {
        return namespace;
    }

    public GitLabProjectDto setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getProjectName() {
        return projectName;
    }

    public GitLabProjectDto setProjectName(String projectName) {
        this.projectName = projectName;
        return this;
    }

    public String getRepUrl() {
        return repUrl;
    }

    public GitLabProjectDto setRepUrl(String repUrl) {
        this.repUrl = repUrl;
        return this;
    }
}
