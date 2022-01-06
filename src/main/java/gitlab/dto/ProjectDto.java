package gitlab.dto;

import lombok.Getter;
import lombok.Setter;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/7 00:14
 */

@Getter
@Setter
public class ProjectDto {

    private String name;
    private String namespace;
    private String sshUrl;
    private String httpUrl;
}
