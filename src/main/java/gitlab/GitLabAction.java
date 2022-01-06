package gitlab;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import gitlab.dto.GitlabServerDto;
import gitlab.dto.ProjectDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/6 20:10
 */
public class GitLabAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        ArrayList<GitlabServerDto> list = Lists.newArrayList();

        Map<GitlabServerDto, Collection<ProjectDto>> gitlabServerDtoCollectionMap = new GitLabState().loadMapOfServersAndProjects(list);
        gitlabServerDtoCollectionMap.values().stream().forEach(o -> System.out.println(o.size()));

//        Project project = e.getRequiredData(CommonDataKeys.PROJECT);
//        CheckoutProvider.Listener checkoutListener = ProjectLevelVcsManager.getInstance(project).getCompositeCheckoutListener();
//        VcsCloneDialog dialog = new VcsCloneDialog.Builder(project).forVcs(GitCheckoutProvider.class);
//        if (dialog.showAndGet()) {
//            dialog.doClone(checkoutListener);
//        }
    }
}
