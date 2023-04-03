package gitlab.actions;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import gitlab.bean.Result;
import gitlab.helper.ActionHelper;
import gitlab.ui.ResultDialog;
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
