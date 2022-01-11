package gitlab.bean;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/11 23:49
 */
@Getter
@Setter
@Accessors(chain = true)
public class MergeRequestResult {
    private String projectName;
    private String webUrl;
    private String changesCount;
    private String errorMsg;

    @Override
    public String toString() {
        if (StringUtils.isNotEmpty(errorMsg)) {
            return projectName + " (" + errorMsg + ")";
        } else {
            return webUrl + " (changesCount=" + changesCount + ")";
        }
    }
}
