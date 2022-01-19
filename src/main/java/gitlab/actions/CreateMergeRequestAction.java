package gitlab.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/19 22:12
 */
public class CreateMergeRequestAction extends DumbAwareAction {


    public CreateMergeRequestAction() {
        super("_Create Merge Request", "Create merge request for projects selected", null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here

    }
}
