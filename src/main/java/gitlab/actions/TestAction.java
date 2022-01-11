package gitlab.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import gitlab.settings.GitLabSettingsState;
import gitlab.bean.GitlabServer;

import java.util.Collection;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/7 12:05
 */
public class TestAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Collection<GitlabServer> gitlabServers = GitLabSettingsState.getInstance().getGitlabServers();
        System.out.println("TestAction");

    }
}
