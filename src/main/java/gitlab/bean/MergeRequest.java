package gitlab.bean;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.gitlab.api.models.GitlabMergeRequest;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/12 01:31
 */
@Getter
@Setter
@Accessors(chain = true)
public class MergeRequest extends GitlabMergeRequest {
    private String projectName;
    private GitlabServer gitlabServer;

    @Override
    public String toString() {
        return projectName + " | " + getMergeStatus() + " | " + getSourceBranch() + "->" + getTargetBranch() + " | " + getChangesCount();
    }
}
