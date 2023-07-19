package com.github.itisokey.githelper.gitlab.bean;


import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.github.lvlifeng.githelper.bean.GitlabServer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.gitlab.api.models.GitlabMergeRequest;

/**
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
        return projectName + " | " + getMergeStatus() + " | "
                + getSourceBranch() + "->" + getTargetBranch() + " | "
                + DateUtil.format(getCreatedAt(), DatePattern.NORM_DATETIME_MINUTE_PATTERN);
    }
}
