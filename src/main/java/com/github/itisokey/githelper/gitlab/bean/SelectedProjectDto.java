package com.github.itisokey.githelper.gitlab.bean;

import com.github.itisokey.githelper.gitlab.settings.GitLabSettingsState;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Set;

/**
 * @author Lv LiFeng
 * @date 2022/1/11 22:12
 */
@Getter
@Setter
@Accessors(chain = true)
public class SelectedProjectDto implements Serializable {

    private Set<ProjectDto> selectedProjectList;
    private GitLabSettingsState gitLabSettingsState;
}
