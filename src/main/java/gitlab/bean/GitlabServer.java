package gitlab.bean;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/7 00:18
 */
@Getter
@Setter
@Accessors(chain = true)
public class GitlabServer {

    private String apiUrl = "";
    private String apiToken = "";
    private String repositoryUrl = "";
    private CloneType preferredConnection = CloneType.SSH;

    @Override
    public String toString() {
        return apiUrl;
    }

    public enum CloneType {
        SSH,
        HTTPS;
    }

}
