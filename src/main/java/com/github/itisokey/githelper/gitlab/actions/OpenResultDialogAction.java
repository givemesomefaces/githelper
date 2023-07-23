package com.github.itisokey.githelper.gitlab.actions;


import com.github.itisokey.githelper.gitlab.ui.ResultDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.github.itisokey.githelper.gitlab.bean.Result;
import com.github.itisokey.githelper.gitlab.helper.ActionHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Lv Lifeng
 * @date 2023-04-02 18:02
 */
public class OpenResultDialogAction extends AnAction implements DumbAware {


    public OpenResultDialogAction(List<Result> results, String title) {
        super("Details", "Details about Gitlab operation", null);
    }

    private static void showDetailsFor(Project project) {
        new ResultDialog(null, null).showAndGet();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ActionHelper.getProject(event).ifPresent(OpenResultDialogAction::showDetailsFor);
    }
}
