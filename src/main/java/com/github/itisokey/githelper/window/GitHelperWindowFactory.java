package com.github.itisokey.githelper.window;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author Lv LiFeng
 * @date 2022/1/3 17:03
 */
public class GitHelperWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        GitHelperWindow gitHelperWindow = new GitHelperWindow(project);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(gitHelperWindow.getGitHelperPanel(), "", false);
        toolWindow.getContentManager().addContent(content);

    }
}
