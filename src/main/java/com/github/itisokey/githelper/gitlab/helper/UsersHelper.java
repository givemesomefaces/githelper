package com.github.itisokey.githelper.gitlab.helper;

import cn.hutool.core.map.MapUtil;
import com.github.itisokey.githelper.gitlab.settings.GitLabSettingsState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.github.lvlifeng.githelper.bean.GitlabServer;
import com.github.itisokey.githelper.gitlab.bean.User;
import org.gitlab.api.models.GitlabUser;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Lv LiFeng
 * @date 2022/1/28 15:01
 */
public class UsersHelper {

    public static List<User> getCurrentUser(ProgressIndicator indicator, Set<GitlabServer> serverDtos, GitLabSettingsState gitLabSettingsState) {
        return serverDtos.stream().filter(o -> !indicator.isCanceled()).map(o -> {
            GitlabUser m = gitLabSettingsState.api(o).getCurrentUser();
            User u = new User();
            u.setServerUserIdMap(new HashMap<>() {{
                put(o.getApiUrl(), m.getId());
            }});
            u.setUsername(m.getUsername());
            u.setName(m.getName());
            return u;
        }).collect(Collectors.toMap(User::getUsername, Function.identity(), (a, b) -> {
            if (MapUtil.isNotEmpty(b.getServerUserIdMap())) {
                a.getServerUserIdMap().putAll(b.getServerUserIdMap());
            }
            return a;
        })).values().stream().collect(Collectors.toList());
    }

    public static Set<User> getAllUsers(ProgressIndicator indicator, Set<GitlabServer> serverDtos, GitLabSettingsState gitLabSettingsState) {
        return serverDtos.stream()
                .filter(o -> !indicator.isCanceled())
                .map(o -> gitLabSettingsState.api(o).getActiveUsers().stream().map(m -> {
                            User u = new User();
                            u.setServerUserIdMap(new HashMap<>() {{
                                put(o.getApiUrl(), m.getId());
                            }});
                            u.setUsername(m.getUsername());
                            u.setName(m.getName());
                            return u;
                        }).collect(Collectors.toList())
                ).flatMap(Collection::stream)
                .collect(Collectors.toList())
                .stream().collect(Collectors.toMap(User::getUsername, Function.identity(), (a, b) -> {
                    if (MapUtil.isNotEmpty(b.getServerUserIdMap())) {
                        a.getServerUserIdMap().putAll(b.getServerUserIdMap());
                    }
                    return a;
                })).values().stream().collect(Collectors.toSet());
    }
}
