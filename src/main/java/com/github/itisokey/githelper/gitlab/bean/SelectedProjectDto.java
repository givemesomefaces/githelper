package com.github.itisokey.githelper.gitlab.bean;

import com.github.itisokey.githelper.gitlab.settings.GitLabSettingsState;

import java.io.Serializable;
import java.util.Set;

/**
 * @author Lv LiFeng
 * @date 2022/1/11 22:12
 */
public class SelectedProjectDto implements Serializable {

    private Set<ProjectDto> selectedProjectList;
    private GitLabSettingsState gitLabSettingsState;

    public Set<ProjectDto> getSelectedProjectList() {
        return selectedProjectList;
    }

    public SelectedProjectDto setSelectedProjectList(Set<ProjectDto> selectedProjectList) {
        this.selectedProjectList = selectedProjectList;
        return this;
    }

    public GitLabSettingsState getGitLabSettingsState() {
        return gitLabSettingsState;
    }

    public SelectedProjectDto setGitLabSettingsState(GitLabSettingsState gitLabSettingsState) {
        this.gitLabSettingsState = gitLabSettingsState;
        return this;
    }
}
