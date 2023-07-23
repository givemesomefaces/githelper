package com.github.itisokey.githelper.gitlab.helper;

import git4idea.repo.GitRepository;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Lv LiFeng
 * @date 2022/1/10 10:08
 */
public class RepositoryHelper {
    public static void sortRepositoriesByName(List<GitRepository> gitRepositories) {
        Collections.sort(gitRepositories, new Comparator<GitRepository>() {
            @Override
            public int compare(GitRepository o1, GitRepository o2) {
                return StringUtils.compareIgnoreCase(o1.getRoot().getName(), o2.getRoot().getName());
            }
        });
    }

}
