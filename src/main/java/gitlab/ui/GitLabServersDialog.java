package gitlab.ui;

import cn.hutool.core.collection.CollectionUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import gitlab.bean.GitlabServer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import window.LcheckBox;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private JButton okButton;

    private JTextField search;

    private JCheckBox selectAllCheckBox;

    private JLabel selectedCount;

    private List<GitlabServer> gitlabServerList;
    private Set<GitlabServer> selectedGitlabServerList = new HashSet<>();
    private Project project;

    private List<GitlabServer> filterGitLabServerList = new ArrayList<>();

    public GitLabServersDialog(@Nullable Project project, List<GitlabServer> gitlabServerList) {
        super(project, null, true, DialogWrapper.IdeModalityType.IDE, false);
        setTitle("GitLab");
        init();
        this.project = project;
        this.gitlabServerList = gitlabServerList;
        getProjectListAndSortByName();
        initSearch();
        initServerList(filterServersByProject(null));
        initSelectAllCheckBox();
        getRootPane().setDefaultButton(cancelButton);
        initBottomButton();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    private void getProjectListAndSortByName() {
        unEnableBottomButton();
        unEnableOtherButtonWhenLoadingData();
        if (CollectionUtil.isEmpty(gitlabServerList)) {
            return;
        }
        enableOtherButtonAfterLoadingData();
        bottomButtonState();
        Collections.sort(gitlabServerList, new Comparator<GitlabServer>() {
            @Override
            public int compare(GitlabServer o1, GitlabServer o2) {
                return StringUtils.compareIgnoreCase(o1.getRepositoryUrl(), o2.getRepositoryUrl());
            }
        });
        initServerList(filterServersByProject(null));
    }

    private void unEnableOtherButtonWhenLoadingData() {
        selectAllCheckBox.setEnabled(false);
    }

    private void enableOtherButtonAfterLoadingData() {
        selectAllCheckBox.setEnabled(true);
    }

    private void unEnableBottomButton() {
        okButton.setEnabled(false);
    }

    private void bottomButtonState() {
        if (CollectionUtil.isEmpty(gitlabServerList) || CollectionUtil.isEmpty(selectedGitlabServerList)) {
            okButton.setEnabled(false);
        }
    }

    private void initBottomButton() {
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

    }

    private void initSearch() {
        search.setToolTipText("Search for projects in all gitlab server by project name");
        search.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                searchProject(e);
            }
        });
    }

    private void searchProject(KeyEvent e) {
        String searchWord = ((JTextField) e.getSource()).getText();

        initServerList(filterServersByProject(searchWord));
        if (selectedGitlabServerList.containsAll(filterGitLabServerList) && CollectionUtil.isNotEmpty(filterGitLabServerList)) {
            selectAllCheckBox.setSelected(true);
        } else {
            selectAllCheckBox.setSelected(false);
        }

    }

    private void initSelectAllCheckBox() {
        selectAllCheckBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() != 1) {
                    return;
                }
                JCheckBox jCheckBox = (JCheckBox) e.getSource();
                if (jCheckBox.isSelected()) {
                    selectedGitlabServerList.addAll(filterGitLabServerList);
                    gitlabServers.addSelectionInterval(0, filterGitLabServerList.size());
                } else {
                    selectedGitlabServerList.removeAll(filterGitLabServerList);
                    gitlabServers.clearSelection();
                }
                setSelectedCount();
                bottomButtonState();
            }
        });
    }

    private void setSelectedCount() {
        selectedCount.setText(String.format("(%s Selected)", selectedCount.size()));
    }

    private List<GitlabServer> filterServersByProject(String searchWord) {
        return filterGitLabServerList = gitlabServerList
                .stream()
                .filter(o -> filterServers(o, searchWord))
                .collect(Collectors.toList());
    }

    private boolean filterServers(GitlabServer o, String searchWord) {
        Set<String> searchProjectList = new HashSet<>();
        if (StringUtils.isNotBlank(searchWord)) {
            searchProjectList = Arrays.stream(StringUtils.split(searchWord, NAME_SPLIT_SYMBOL)).collect(Collectors.toSet());
        }
        return (CollectionUtil.isNotEmpty(searchProjectList)
                && ((searchProjectList.size() == 1 && o.getRepositoryUrl().toLowerCase().contains(new ArrayList<>(searchProjectList).get(0)))
                || (searchProjectList.size() != 1 && searchProjectList.stream().anyMatch(s -> StringUtils.equals(o.getRepositoryUrl().toLowerCase(), s.toLowerCase())))))
                || CollectionUtil.isEmpty(searchProjectList);

    }

    private void initServerList(List<GitlabServer> gitlabServerList) {

        this.gitlabServers.setListData(gitlabServerList.toArray());
        this.gitlabServers.setCellRenderer(new LcheckBox());
        this.gitlabServers.setEnabled(true);
        this.gitlabServers.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (super.isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                    selectedGitlabServerList.remove(gitlabServerList.get(index0));
                } else {
                    super.addSelectionInterval(index0, index1);
                    selectedGitlabServerList.add(gitlabServerList.get(index0));
                    checkAll(gitlabServerList);
                }
                setSelectedCount();
                bottomButtonState();
            }
        });
        if (CollectionUtil.isNotEmpty(selectedGitlabServerList)) {
            this.gitlabServers.setSelectedIndices(selectedGitlabServerList.stream()
                    .map(o -> gitlabServerList.indexOf(o))
                    .mapToInt(Integer::valueOf)
                    .toArray());
            checkAll(gitlabServerList);
        } else {
            this.gitlabServers.clearSelection();
        }
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
}