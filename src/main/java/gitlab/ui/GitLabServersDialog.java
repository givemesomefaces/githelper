package gitlab.ui;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.github.lvlifeng.githelper.Bundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import gitlab.bean.GitlabServer;
import gitlab.bean.MergeRequest;
import gitlab.bean.ProjectDto;
import gitlab.bean.SelectedProjectDto;
import gitlab.bean.User;
import gitlab.helper.RepositoryHelper;
import gitlab.helper.UsersHelper;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import window.LcheckBox;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static gitlab.common.Constants.NAME_SPLIT_SYMBOL;

/**
 *
 *
 * @author Lv Lifeng
 * @date 2023-03-28 19:41
 */
public class GitLabServersDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(GitLabDialog.class);

    private JPanel contentPane;

    private JList gitlabServers;

    private JButton cancelButton;

    private JTextField search;

    private JCheckBox selectAllCheckBox;

    private JLabel selectedCount;

    private List<GitlabServer> gitlabServerList;
    private Set<GitlabServer> selectedGitlabServerList = new HashSet<>();
    private Project project;

    private List<GitlabServer> filterProjectList = new ArrayList<>();

    public GitLabServersDialog(@Nullable Project project, List<GitlabServer> gitlabServerList) {
        super(project, null, true, DialogWrapper.IdeModalityType.IDE, false);
        setTitle("GitLab");
        init();
        this.project = project;
        this.gitlabServerList = gitlabServerList;
        getProjectListAndSortByName();
        initSearch();
        initServerList(filterProjectsByProject(null));
        initSelectAllCheckBox();
        getRootPane().setDefaultButton(cancelButton);
        initBottomButton();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }



    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
}
