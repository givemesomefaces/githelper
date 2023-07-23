package com.github.itisokey.githelper.gitlab.bean;


/**
 * @author Lv LiFeng
 * @date 2022/1/7 00:35
 */

public class Namespace {

    private int id;
    private String path;
    private String kind;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}
