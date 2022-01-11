package gitlab.bean;

import lombok.Getter;
import lombok.Setter;
import org.gitlab.api.models.GitlabProject;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/7 00:14
 */

@Getter
@Setter
public class ProjectDto extends GitlabProject {

    private GitlabServer gitlabServer;

    @Override
    public String toString() {
        return this.getName() + "  (" + gitlabServer.getApiUrl() + ")";
    }
}
