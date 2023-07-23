package com.github.itisokey.githelper.gitlab.helper;




import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 *
 *
 * @author Lv Lifeng
 * @date 2023-04-02 18:11
 */
public class ActionHelper {

    private ActionHelper() {
    }

    @NotNull
    public static Optional<Project> getProject(AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        return Optional.ofNullable(CommonDataKeys.PROJECT.getData(dataContext));
    }
}
