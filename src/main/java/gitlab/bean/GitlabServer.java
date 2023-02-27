package gitlab.bean;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GitlabServer that = (GitlabServer) o;
        return Objects.equals(apiUrl, that.apiUrl)
                && Objects.equals(apiToken, that.apiToken)
                && Objects.equals(repositoryUrl, that.repositoryUrl)
                && preferredConnection == that.preferredConnection;
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiUrl, apiToken, repositoryUrl, preferredConnection);
    }
}
