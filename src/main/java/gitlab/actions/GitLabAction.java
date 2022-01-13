package gitlab.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import gitlab.settings.GitLabSettingsState;
import gitlab.settings.SettingsView;
import gitlab.ui.GitLabDialog;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/6 20:10
 */
public class GitLabAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {

        Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        GitLabSettingsState gitLabSettingsState = GitLabSettingsState.getInstance();
        if (gitLabSettingsState.hasSettings()) {
            showGitLabDialog(project);
        } else {
            boolean b = new SettingsView(null).showAndGet();
            if (gitLabSettingsState.hasSettings()) {
                showGitLabDialog(project);
            }
        }

    }
    private boolean showGitLabDialog(Project project){
        return new GitLabDialog(project, null, true, DialogWrapper.IdeModalityType.IDE, false).showAndGet();
    }
}
