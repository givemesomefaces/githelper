package com.github.itisokey.githelper.gitlab.bean;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.itisokey.githelper.gitlab.enums.OperationTypeEnum;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabMergeRequest;

/**
 * @author Lv LiFeng
 * @date 2022/1/11 23:49
 */
public class Result extends GitlabMergeRequest {
    private String projectName;
    private String errorMsg;
    private String changeFilesCount;
    private OperationTypeEnum type;
    private String desc;

    public Result() {
    }

    public Result(GitlabMergeRequest gitlabMergeRequest) {
        BeanUtil.copyProperties(gitlabMergeRequest, this);
    }

    public void setErrorMsg(String errorMsg) {
        if (JSONUtil.isJson(errorMsg)) {
            JSONObject jsonObject = JSONUtil.parseObj(errorMsg);
            this.errorMsg = jsonObject.getStr("message");
        }
    }

    @Override
    public String toString() {
        if (StringUtils.isNotEmpty(errorMsg)) {
            return projectName + " (" + errorMsg + ")";
        }
        switch (type) {
            case CREATE_MERGE_REQUEST:
                return getWebUrl() + " [ChangeFiles:" + getChangeFilesCount() + "]";
            case MERGE:
            case CLOSE_MERGE_REQUEST:
                return projectName + "  " + getSourceBranch() + "->" + getTargetBranch() + "  " + getState();
            case CREATE_TAG:
                return projectName + " Tag " + desc + " created";
            default:
        }
        return null;
    }

    public String getProjectName() {
        return projectName;
    }

    public Result setProjectName(String projectName) {
        this.projectName = projectName;
        return this;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public String getChangeFilesCount() {
        return changeFilesCount;
    }

    public Result setChangeFilesCount(String changeFilesCount) {
        this.changeFilesCount = changeFilesCount;
        return this;
    }

    public OperationTypeEnum getType() {
        return type;
    }

    public Result setType(OperationTypeEnum type) {
        this.type = type;
        return this;
    }

    public String getDesc() {
        return desc;
    }

    public Result setDesc(String desc) {
        this.desc = desc;
        return this;
    }


}
