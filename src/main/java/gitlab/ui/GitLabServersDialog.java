package gitlab.ui;

import cn.hutool.core.collection.CollectionUtil;
import com.github.lvlifeng.githelper.Bundle;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import gitlab.api.GitlabRestApi;
import gitlab.bean.MergeRequest;
import gitlab.bean.Result;
import gitlab.bean.SelectedProjectDto;
import gitlab.common.Notifier;
import gitlab.enums.MergeStatusEnum;
import gitlab.enums.OperationTypeEnum;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabMergeRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import window.LcheckBox;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Lv Lifeng
 * @date 2023-03-28 19:41
 */
public class GitLabServersDialog extends DialogWrapper {
    private JPanel contentPane;
    private Project project;

    public GitLabServersDialog(Project project) {
        super(null, null, true, IdeModalityType.IDE, false);
        setTitle(Bundle.message("mergeRequestDialogTitle"));
        this.project = project;
        this.createSouthPanel().setVisible(false);
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
