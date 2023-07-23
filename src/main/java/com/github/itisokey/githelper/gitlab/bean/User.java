package com.github.itisokey.githelper.gitlab.bean;

import org.gitlab.api.models.GitlabUser;

import java.util.Map;
import java.util.Objects;

/**
 * @author Lv LiFeng
 * @date 2022/1/9 11:25
 */

public class User extends GitlabUser {

    private Map<String, Integer> serverUserIdMap;


    public User resetId(String apiUrl) {
        super.setId(serverUserIdMap.getOrDefault(apiUrl, null));
        return this;
    }

    @Override
    public String toString() {
        return this.getName() + "@" + this.getUsername();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(this.toString(), user.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.toString());
    }

    public Map<String, Integer> getServerUserIdMap() {
        return serverUserIdMap;
    }

    public void setServerUserIdMap(Map<String, Integer> serverUserIdMap) {
        this.serverUserIdMap = serverUserIdMap;
    }
}
