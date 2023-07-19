package com.github.itisokey.githelper.gitlab.enums;

/**
 * @author Lv LiFeng
 * @date 2022/1/19 01:05
 */
public enum StatusEnum {

    SUCCESSES("GitLab Operation Successful:"),
    FAILED("GitLab Operation Failed:"),
    ;


    private String desc;

    StatusEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
