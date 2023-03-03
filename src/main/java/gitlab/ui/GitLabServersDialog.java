package gitlab.ui;

import cn.hutool.core.collection.CollectionUtil;
import com.google.common.collect.Lists;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import gitlab.bean.GitlabServer;
import gitlab.bean.ProjectDto;
import gitlab.settings.GitLabSettingsState;
import org.apache.commons.lang3.StringUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static gitlab.common.Constants.NAME_SPLIT_SYMBOL;

/**
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

    private final List<GitlabServer> gitlabServerList;
    private final Set<GitlabServer> selectedGitlabServerList = new HashSet<>();
    private final Project project;

    private final GitLabSettingsState gitLabSettingsState;

    private List<GitlabServer> filterGitLabServerList = new ArrayList<>();

    public GitLabServersDialog(@Nullable Project project, GitLabSettingsState gitLabSettingsState) {
        super(project, null, true, DialogWrapper.IdeModalityType.IDE, false);
        setTitle("GitLab Servers");
        init();
        this.project = project;
        this.gitLabSettingsState = gitLabSettingsState;
        this.gitlabServerList = gitLabSettingsState.getGitlabServers();
        this.createSouthPanel().setVisible(false);
        getServerListAndSortByName();
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

    private void getServerListAndSortByName() {
        unEnableBottomButton();
        unEnableOtherButtonWhenLoadingData();
        if (CollectionUtil.isEmpty(gitlabServerList)) {
            return;
        }
        enableOtherButtonAfterLoadingData();
        bottomButtonState();
        gitlabServerList.sort(Comparator.comparing(GitlabServer::getRepositoryUrl));
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
        } else {
            okButton.setEnabled(true);
        }
    }

    private void initBottomButton() {
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ProgressManager.getInstance().run(new Task.Modal(project, "GitLab", true) {
                    List<ProjectDto> projectDtoList = new ArrayList<>();

                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setText("Loading projects...");
                        AtomicInteger index = new AtomicInteger(1);
                        projectDtoList = selectedGitlabServerList
                                .stream()
                                .filter(o -> !indicator.isCanceled())
                                .map(o -> {
                                    indicator.setText2("(" + index.getAndIncrement() + "/" + selectedGitlabServerList.size() + ") " + o.getRepositoryUrl());
                                    return gitLabSettingsState.loadMapOfServersAndProjects(Lists.newArrayList(o)).values();
                                }).flatMap(Collection::stream)
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList());
                        if (CollectionUtil.isEmpty(projectDtoList)) {
                            return;
                        }
                        indicator.setText("Projects loaded");
                    }

                    @Override
                    public void onSuccess() {
                        super.onSuccess();
                        dispose();
                        if (CollectionUtil.isNotEmpty(projectDtoList)) {
                            new GitLabDialogV2(project, projectDtoList, selectedGitlabServerList).showAndGet();
                        }
                    }
                });
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
        search.setToolTipText("Search for Gitlab server by domain keyword");
        search.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                searchServer(e);
            }
        });
    }

    private void searchServer(KeyEvent e) {
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
                    filterGitLabServerList.forEach(selectedGitlabServerList::remove);
                    gitlabServers.clearSelection();
                }
                setSelectedCount();
                bottomButtonState();
            }
        });
    }

    private void setSelectedCount() {
        selectedCount.setText(String.format("(%s Selected)", selectedGitlabServerList.size()));
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
                }
                checkAll(filterGitLabServerList);
                setSelectedCount();
                bottomButtonState();
            }
        });
        if (CollectionUtil.isNotEmpty(selectedGitlabServerList)) {
            this.gitlabServers.setSelectedIndices(selectedGitlabServerList.stream()
                    .map(gitlabServerList::indexOf)
                    .mapToInt(Integer::valueOf)
                    .toArray());
            checkAll(gitlabServerList);
        } else {
            this.gitlabServers.clearSelection();
        }
    }


    private void checkAll(List<GitlabServer> filterServerList) {
        if (selectedGitlabServerList.size() == gitlabServerList.size()
                && filterServerList.size() == gitlabServerList.size()) {
            selectAllCheckBox.setSelected(true);
        } else {
            selectAllCheckBox.setSelected(false);
        }
    }

    private void clearSelected() {
        selectedGitlabServerList.clear();
        gitlabServers.clearSelection();
        selectAllCheckBox.setSelected(false);
        selectedCount.setText("(0 Selected)");
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
}
