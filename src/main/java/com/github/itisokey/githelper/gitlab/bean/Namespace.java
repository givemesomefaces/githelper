package com.github.itisokey.githelper.gitlab.bean;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Lv LiFeng
 * @date 2022/1/7 00:35
 */
@Getter
@Setter
public class Namespace {

    private int id;
    private String path;
    private String kind;
}
