package gitlab.ui;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
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
import com.github.lvlifeng.githelper.bean.GitlabServer;
import gitlab.bean.MergeRequest;
import gitlab.bean.ProjectDto;
import gitlab.bean.SelectedProjectDto;
import gitlab.bean.User;
import gitlab.helper.UsersHelper;
import gitlab.settings.GitLabSettingsState;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static gitlab.common.Constants.NAME_SPLIT_SYMBOL;

public class GitLabDialogV2 extends DialogWrapper {
    private static final Logger log = Logger.getInstance(GitLabDialog.class);
    private JPanel contentPane;
    private JList projectList;
    private JTextField search;
    private JRadioButton branchNameRadioButton;
    private JRadioButton projectNameRadioButton;
    private JButton cloneButton;
    private JButton createMergeRequestButton;
    private JCheckBox selectAllCheckBox;
    private JLabel selectedCount;
    private JButton cancelButton;
    private JButton mergeButton;
    private JButton tagButton;
    private JList gitRemoteServerList;
    private JPanel gitlabServerListJpanel;

    private final List<String> selectedGitRemoteServerList;

    private final GitLabSettingsState gitLabSettingsState = GitLabSettingsState.getInstance();
    private List<ProjectDto> projectDtoList;
    private List<ProjectDto> projectDtoListByBranch = new ArrayList<>();
    private final Set<ProjectDto> selectedProjectList = new HashSet<>();
    private final Project project;

    private List<ProjectDto> filterProjectList = new ArrayList<>();

    private final Map<String, List<ProjectDto>> serverProjectsMap;

    private final List<GitlabServer> selectedGitlabServerList;

    public GitLabDialogV2(@Nullable Project project, List<ProjectDto> projectDtoList, Set<GitlabServer> selectedGitlabServerList) {
        super(project, null, true, DialogWrapper.IdeModalityType.IDE, false);
        setTitle("GitLab");
        init();
        this.project = project;
        this.serverProjectsMap = projectDtoList.stream().collect(Collectors.groupingBy(ProjectDto::getGitlabServerRepositoryUrl));
        this.projectDtoList = projectDtoList;
        this.selectedGitlabServerList = CollUtil.isNotEmpty(selectedGitlabServerList) ? new ArrayList<>(selectedGitlabServerList) : Lists.newArrayList();
        this.selectedGitRemoteServerList = CollUtil.isNotEmpty(selectedGitlabServerList) ? selectedGitlabServerList.stream().map(GitlabServer::getRepositoryUrl).collect(Collectors.toList()) : Lists.newArrayList();
        initAll();
        initGitLabServerList();
    }

    private void initAll() {
        getProjectListAndSortByName();
        initSearch();
        initRadioButton();
        initProjectList(filterProjectsByProject(null));
        initSelectAllCheckBox();
        getRootPane().setDefaultButton(cancelButton);
        initBottomButton();
    }

    private void initGitLabServerList() {
        if (CollectionUtil.isEmpty(selectedGitlabServerList) || selectedGitlabServerList.size() == 1) {
            gitlabServerListJpanel.setVisible(false);
            return;
        }
        this.gitRemoteServerList.setListData(selectedGitlabServerList.toArray());
        this.gitRemoteServerList.setCellRenderer(new LcheckBox());
        this.gitRemoteServerList.setEnabled(true);
        this.gitRemoteServerList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                GitlabServer gitlabServer = selectedGitlabServerList.get(index0);
                if (Objects.equals(false, gitlabServer.getValidFlag())) {
                    return;
                }
                if (super.isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                    selectedGitRemoteServerList.remove(gitlabServer.getRepositoryUrl());
                } else {
                    super.addSelectionInterval(index0, index1);
                    selectedGitRemoteServerList.add(gitlabServer.getRepositoryUrl());
                }
                projectDtoList = selectedGitRemoteServerList.stream()
                        .filter(server -> Objects.nonNull(serverProjectsMap.get(server)))
                        .flatMap(server -> serverProjectsMap.get(server).stream())
                        .collect(Collectors.toList());
                initAll();
            }
        });
        this.gitRemoteServerList.addSelectionInterval(0, selectedGitlabServerList.size());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    private void getProjectListAndSortByName() {
        unEnableBottomButton();
        unEnableOtherButtonWhenLoadingData();
        if (CollectionUtil.isEmpty(projectDtoList)) {
            return;
        }
        enableOtherButtonAfterLoadingData();
        bottomButtonState();
        projectDtoList.sort(Comparator.comparing(ProjectDto::getName));
        initProjectList(filterProjectsByProject(null));
    }

    private void unEnableOtherButtonWhenLoadingData() {
        selectAllCheckBox.setEnabled(false);
        branchNameRadioButton.setEnabled(false);
        projectNameRadioButton.setEnabled(false);
    }

    private void enableOtherButtonAfterLoadingData() {
        selectAllCheckBox.setEnabled(true);
        branchNameRadioButton.setEnabled(true);
        projectNameRadioButton.setEnabled(true);
    }

    private void unEnableBottomButton() {
        createMergeRequestButton.setEnabled(false);
        cloneButton.setEnabled(false);
        mergeButton.setEnabled(false);
        tagButton.setEnabled(false);
    }

    private void bottomButtonState() {
        if (CollectionUtil.isEmpty(projectDtoList) || CollectionUtil.isEmpty(selectedProjectList)) {
            createMergeRequestButton.setEnabled(false);
            cloneButton.setEnabled(false);
            mergeButton.setEnabled(false);
            tagButton.setEnabled(false);
        }
        if (CollectionUtil.isNotEmpty(selectedProjectList)) {
            createMergeRequestButton.setEnabled(true);
            cloneButton.setEnabled(true);
            mergeButton.setEnabled(true);
            tagButton.setEnabled(true);
        }
        if (branchNameRadioButton.isSelected()) {
            cloneButton.setEnabled(false);
        }
    }

    private void initBottomButton() {
        mergeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ProgressManager.getInstance().run(new Task.Modal(project, Bundle.message("mergeRequestDialogTitle"), true) {
                    List<MergeRequest> gitlabMergeRequests = new ArrayList<>();

                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setText("Loading merge requests...");
                        AtomicInteger index = new AtomicInteger(1);
                        gitlabMergeRequests = selectedProjectList
                                .stream()
                                .filter(o -> !indicator.isCanceled())
                                .map(o -> {
                                    indicator.setText2("(" + index.getAndIncrement() + "/" + selectedProjectList.size() + ") " + o.getName());
                                    List<GitlabMergeRequest> openMergeRequest = null;
                                    try {
                                        openMergeRequest = gitLabSettingsState
                                                .api(o.getGitlabServer())
                                                .getOpenMergeRequest(o.getId());
                                    } catch (IOException ioException) {
                                        openMergeRequest = Lists.newArrayList();
                                    }
                                    return openMergeRequest.stream().map(u -> {
                                        MergeRequest m = new MergeRequest();
                                        BeanUtil.copyProperties(u, m);
                                        m.setProjectName(o.getName());
                                        m.setGitlabServer(o.getGitlabServer());
                                        return m;
                                    }).collect(Collectors.toList());
                                })
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList());

                        indicator.setText("Merge requests loaded");
                    }

                    @Override
                    public void onSuccess() {
                        super.onSuccess();
                        if (CollectionUtil.isEmpty(gitlabMergeRequests)) {
                            Messages.showMessageDialog("No merge requests to merge!", Bundle.message("mergeRequestDialogTitle"), null);
                            return;
                        }
                        new MergeDialog(project, new SelectedProjectDto()
                                .setGitLabSettingsState(gitLabSettingsState)
                                .setSelectedProjectList(selectedProjectList),
                                gitlabMergeRequests).showAndGet();
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
        cloneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new CloneDialog(project, selectedProjectList).showAndGet();
            }
        });
        createMergeRequestButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ProgressManager.getInstance().run(new Task.Modal(project, Bundle.message("createMergeRequestDialogTitle"), true) {
                    List<String> commonBranch = new ArrayList<>();
                    List<User> currentUser = new ArrayList<>();
                    Set<User> users = new HashSet<>();

                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setText("Loading common branches...");
                        AtomicInteger index = new AtomicInteger(1);
                        commonBranch = selectedProjectList.stream()
                                .filter(o -> !indicator.isCanceled())
                                .map(o -> {
                                    indicator.setText2("(" + index.getAndIncrement() + "/" + selectedProjectList.size() + ") " + o.getName());
                                    return gitLabSettingsState.api(o.getGitlabServer())
                                            .getBranchesByProject(o)
                                            .stream()
                                            .map(GitlabBranch::getName)
                                            .collect(Collectors.toList());
                                })
                                .collect(Collectors.toList())
                                .stream()
                                .reduce((a, b) -> new ArrayList<>(CollectionUtil.intersectionDistinct(a, b)))
                                .orElse(Lists.newArrayList());
                        commonBranch.stream().sorted(String::compareToIgnoreCase);
                        if (CollectionUtil.isEmpty(commonBranch)) {
                            return;
                        }
                        if (indicator.isCanceled()) {
                            return;
                        }
                        indicator.setText2(null);
                        indicator.setText("Loading users...");
                        Set<GitlabServer> gitlabServerSet = selectedProjectList.stream().map(ProjectDto::getGitlabServer).collect(Collectors.toSet());
                        currentUser = UsersHelper.getCurrentUser(indicator, gitlabServerSet, gitLabSettingsState);
                        if (indicator.isCanceled()) {
                            return;
                        }
                        users = UsersHelper.getAllUsers(indicator, gitlabServerSet, gitLabSettingsState);
                    }

                    @Override
                    public void onSuccess() {
                        super.onSuccess();
                        if (CollectionUtil.isEmpty(commonBranch)) {
                            Messages.showMessageDialog("No common branches, please reselect project!", Bundle.message("createMergeRequestDialogTitle"), null);
                            return;
                        }
                        new MergeRequestDialog(project, new SelectedProjectDto()
                                .setGitLabSettingsState(gitLabSettingsState)
                                .setSelectedProjectList(selectedProjectList),
                                commonBranch,
                                currentUser,
                                users).showAndGet();
                    }
                });
            }
        });
        tagButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ProgressManager.getInstance().run(new Task.Modal(project, Bundle.message("createTagDialogTitle"), true) {
                    List<String> commonFrom = new ArrayList<>();

                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setText("Loading common branches");
                        AtomicInteger indexBranch = new AtomicInteger(1);
                        commonFrom = new ArrayList<>();
                        List<String> commonBranches = selectedProjectList.stream()
                                .filter(o -> !indicator.isCanceled())
                                .map(o -> {
                                    indicator.setText2("(" + indexBranch.getAndIncrement() + "/" + selectedProjectList.size() + ") " + o.getName());
                                    return gitLabSettingsState.api(o.getGitlabServer())
                                            .getBranchesByProject(o)
                                            .stream()
                                            .map(GitlabBranch::getName)
                                            .collect(Collectors.toList());
                                })
                                .collect(Collectors.toList())
                                .stream()
                                .reduce((a, b) -> new ArrayList<>(CollectionUtil.intersectionDistinct(a, b)))
                                .orElse(Lists.newArrayList());
                        commonBranches.sort(String::compareToIgnoreCase);
                        if (indicator.isCanceled()) {
                            return;
                        }
                        indicator.setText("Loading common tags");
                        AtomicInteger indexTag = new AtomicInteger(1);
                        List<String> commonTags = selectedProjectList.stream()
                                .filter(o -> !indicator.isCanceled())
                                .map(o -> {
                                    indicator.setText2("(" + indexTag.getAndIncrement() + "/" + selectedProjectList.size() + ") " + o.getName());
                                    return gitLabSettingsState.api(o.getGitlabServer())
                                            .getTagsByProject(o)
                                            .stream()
                                            .map(GitlabTag::getName)
                                            .collect(Collectors.toList());
                                })
                                .collect(Collectors.toList())
                                .stream()
                                .reduce((a, b) -> new ArrayList<>(CollectionUtil.intersectionDistinct(a, b)))
                                .orElse(Lists.newArrayList());
                        commonTags.sort(String::compareToIgnoreCase);
                        if (CollectionUtil.isNotEmpty(commonBranches)) {
                            commonFrom.addAll(commonBranches);
                        }
                        if (CollectionUtil.isNotEmpty(commonTags)) {
                            commonFrom.addAll(commonTags);
                        }
                        indicator.setText("Common branches and tags loaded");
                    }

                    @Override
                    public void onCancel() {
                        super.onCancel();
                    }

                    @Override
                    public void onSuccess() {
                        super.onSuccess();
                        if (CollectionUtil.isEmpty(commonFrom)) {
                            Messages.showMessageDialog("No common branches or tags, please reselect project!", Bundle.message("createTagDialogTitle"), null);
                            return;
                        }
                        new TagDialog(project, new SelectedProjectDto()
                                .setSelectedProjectList(selectedProjectList)
                                .setGitLabSettingsState(gitLabSettingsState),
                                commonFrom).showAndGet();
                    }
                });
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

        if (projectNameRadioButton.isSelected()) {
            initProjectList(filterProjectsByProject(searchWord));
        }
        if (branchNameRadioButton.isSelected()) {
            initProjectList(filterProjectListByBranch(searchWord));
        }
        if (selectedProjectList.containsAll(filterProjectList) && CollectionUtil.isNotEmpty(filterProjectList)) {
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
                    selectedProjectList.addAll(filterProjectList);
                    projectList.addSelectionInterval(0, filterProjectList.size());
                } else {
                    filterProjectList.forEach(selectedProjectList::remove);
                    projectList.clearSelection();
                }
                setSelectedCount();
                bottomButtonState();
            }
        });
    }

    private void setSelectedCount() {
        selectedCount.setText(String.format("(%s Selected)", selectedProjectList.size()));
    }

    private List<ProjectDto> filterProjectsByProject(String searchWord) {
        return filterProjectList = projectDtoList
                .stream()
                .filter(o -> filterProject(o, searchWord))
                .collect(Collectors.toList());
    }

    private boolean filterProject(ProjectDto o, String searchWord) {
        Set<String> searchProjectList = new HashSet<>();
        if (StringUtils.isNotBlank(searchWord)) {
            searchProjectList = Arrays.stream(StringUtils.split(searchWord, NAME_SPLIT_SYMBOL)).collect(Collectors.toSet());
        }
        return (CollectionUtil.isNotEmpty(searchProjectList)
                && ((searchProjectList.size() == 1 && o.getName().toLowerCase().contains(new ArrayList<>(searchProjectList).get(0)))
                || (searchProjectList.size() != 1 && searchProjectList.stream().anyMatch(s -> StringUtils.equals(o.getName().toLowerCase(), s.toLowerCase())))))
                || CollectionUtil.isEmpty(searchProjectList);

    }

    private void initProjectList(List<ProjectDto> projectList) {

        this.projectList.setListData(projectList.toArray());
        this.projectList.setCellRenderer(new LcheckBox());
        this.projectList.setEnabled(true);
        this.projectList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (super.isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                    selectedProjectList.remove(projectList.get(index0));
                } else {
                    super.addSelectionInterval(index0, index1);
                    selectedProjectList.add(projectList.get(index0));
                    checkAll(projectList);
                }
                setSelectedCount();
                bottomButtonState();
            }
        });
        if (CollectionUtil.isNotEmpty(selectedProjectList)) {
            this.projectList.setSelectedIndices(selectedProjectList.stream()
                    .map(projectList::indexOf)
                    .mapToInt(Integer::valueOf)
                    .toArray());
            checkAll(projectList);
        } else {
            this.projectList.clearSelection();
        }
    }


    private void checkAll(List<ProjectDto> filterRepositories) {
        if (projectNameRadioButton.isSelected()) {
            if (selectedProjectList.size() == projectDtoList.size()
                    && filterRepositories.size() == projectDtoList.size()) {
                selectAllCheckBox.setSelected(true);
            }
        }
        if (branchNameRadioButton.isSelected()) {
            if (selectedProjectList.size() == projectDtoListByBranch.size()
                    && filterRepositories.size() == projectDtoListByBranch.size()) {
                selectAllCheckBox.setSelected(true);
            }
        }
    }

    private void initRadioButton() {
        ButtonGroup btnGroup = new ButtonGroup();
        branchNameRadioButton.setMultiClickThreshhold(888);
        projectNameRadioButton.setMultiClickThreshhold(888);
        btnGroup.add(branchNameRadioButton);
        btnGroup.add(projectNameRadioButton);
        projectNameRadioButton.setSelected(true);

        projectNameRadioButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() != 1) {
                    return;
                }
                JRadioButton jRadioButton = (JRadioButton) e.getSource();
                if (jRadioButton.isSelected()) {
                    search.setToolTipText("Search for projects in all gitlab server by project name");
                    clearSelected();
                    unEnableBottomButton();
                    initProjectList(filterProjectsByProject(search.getText()));
                }
            }
        });

        branchNameRadioButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() != 1) {
                    return;
                }
                JRadioButton jRadioButton = (JRadioButton) e.getSource();
                if (jRadioButton.isSelected()) {
                    search.setToolTipText("Search for projects in current workspace by branch name");
                    clearSelected();
                    unEnableBottomButton();
                    initProjectList(filterProjectListByBranch(search.getText()));
                }
            }
        });
    }

    private void clearSelected() {
        selectedProjectList.clear();
        projectList.clearSelection();
        selectAllCheckBox.setSelected(false);
        selectedCount.setText("(0 Selected)");
    }

    private List<ProjectDto> filterProjectListByBranch(String searchWord) {
        List<GitRepository> repositories = new ArrayList<>(GitUtil.getRepositories(project));
        repositories.sort(Comparator.comparing(o -> o.getRoot().getName()));
        projectDtoListByBranch = projectDtoList.stream()
                .filter(o -> repositories.stream()
                        .map(GitRepository::getRoot)
                        .map(VirtualFile::getName)
                        .collect(Collectors.toList())
                        .contains(o.getName()))
                .collect(Collectors.toList());
        List<GitRepository> filterRepositories = repositories
                .stream()
                .filter(o ->
                        (StringUtils.isNotEmpty(searchWord)
                                && o.getBranches().getRemoteBranches()
                                .stream()
                                .anyMatch(i -> i.getName().toLowerCase().contains(searchWord.toLowerCase())))
                                || StringUtils.isEmpty(searchWord)
                ).collect(Collectors.toList());
        return filterProjectList = projectDtoList.stream()
                .filter(o -> filterRepositories.stream()
                        .anyMatch(z -> z.getRoot().getName().toLowerCase().equals(o.getName())
                                && z.getBranches().getRemoteBranches()
                                .stream()
                                .anyMatch(y -> y.getRemote().getUrls().stream()
                                        .anyMatch(re -> re.toLowerCase()
                                                .contains(o.getGitlabServer().getRepositoryUrl().toLowerCase()))))
                ).collect(Collectors.toList());
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
}
