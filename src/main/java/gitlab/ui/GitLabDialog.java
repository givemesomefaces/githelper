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
import gitlab.bean.*;
import gitlab.helper.RepositoryHelper;
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
import java.awt.event.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static gitlab.common.Constants.NAME_SPLIT_SYMBOL;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/8 16:14
 */
public class GitLabDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(GitLabDialog.class);
    private JPanel contentPane;
    private JList projectJList;
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

    private GitLabSettingsState gitLabSettingsState = GitLabSettingsState.getInstance();
    private List<ProjectDto> projectDtoList = new ArrayList<>();
    private List<ProjectDto> projectDtoListByBranch = new ArrayList<>();
    private Set<ProjectDto> selectedProjectList = new HashSet<>();
    private Project project;

    private List<ProjectDto> filterProjectList = new ArrayList<>();

    public GitLabDialog(@Nullable Project project, List<ProjectDto> projectDtoList) {
        super(project, null, true, DialogWrapper.IdeModalityType.IDE, false);
        setTitle("GitLab");
        init();
        this.project = project;
        this.projectDtoList = projectDtoList;
        getProjectListAndSortByName();
        initSerach();
        initRadioButton();
        initProjectList(filterProjectsByProject(null));
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
        if (CollectionUtil.isEmpty(projectDtoList)) {
            return;
        }
        enableOtherButtonAfterLoadingData();
        bottomButtonState();
        Collections.sort(projectDtoList, new Comparator<ProjectDto>() {
            @Override
            public int compare(ProjectDto o1, ProjectDto o2) {
                return StringUtils.compareIgnoreCase(o1.getName(), o2.getName());
            }
        });
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
                                    indicator.setText2("("+ index.getAndIncrement() +"/"+ selectedProjectList.size()+") " + o.getName());
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
                                    indicator.setText2("("+ index.getAndIncrement() +"/"+ selectedProjectList.size()+") " + o.getName());
                                    return gitLabSettingsState.api(o.getGitlabServer())
                                            .getBranchesByProject(o)
                                            .stream()
                                            .map(GitlabBranch::getName)
                                            .collect(Collectors.toList());
                                })
                                .collect(Collectors.toList())
                                .stream()
                                .reduce((a, b) -> CollectionUtil.intersectionDistinct(a, b).stream().collect(Collectors.toList()))
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
                        List<String> commonBranch = selectedProjectList.stream()
                                .filter(o -> !indicator.isCanceled())
                                .map(o -> {
                                    indicator.setText2("("+ indexBranch.getAndIncrement() +"/"+ selectedProjectList.size()+") " + o.getName());
                                    return gitLabSettingsState.api(o.getGitlabServer())
                                            .getBranchesByProject(o)
                                            .stream()
                                            .map(GitlabBranch::getName)
                                            .collect(Collectors.toList());
                                })
                                .collect(Collectors.toList())
                                .stream()
                                .reduce((a, b) -> CollectionUtil.intersectionDistinct(a, b).stream().collect(Collectors.toList()))
                                .orElse(Lists.newArrayList());
                        commonBranch.stream().sorted(String::compareToIgnoreCase);
                        if (indicator.isCanceled()) {
                            return;
                        }
                        indicator.setText("Loading common tags");
                        AtomicInteger indexTag = new AtomicInteger(1);
                        List<String> commonTag = selectedProjectList.stream()
                                .filter(o -> !indicator.isCanceled())
                                .map(o -> {
                                    indicator.setText2("("+ indexTag.getAndIncrement() +"/"+ selectedProjectList.size()+") " + o.getName());
                                    return gitLabSettingsState.api(o.getGitlabServer())
                                            .getTagsByProject(o)
                                            .stream()
                                            .map(GitlabTag::getName)
                                            .collect(Collectors.toList());
                                })
                                .collect(Collectors.toList())
                                .stream()
                                .reduce((a, b) -> CollectionUtil.intersectionDistinct(a, b).stream().collect(Collectors.toList()))
                                .orElse(Lists.newArrayList());
                        commonTag.stream().sorted(String::compareToIgnoreCase);
                        if (CollectionUtil.isNotEmpty(commonBranch)) {
                            commonFrom.addAll(commonBranch);
                        }
                        if (CollectionUtil.isNotEmpty(commonTag)) {
                            commonFrom.addAll(commonTag);
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
                            Messages.showMessageDialog("No common from, please reselect project!", Bundle.message("createTagDialogTitle"), null);
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

    private void initSerach() {
        search.setToolTipText("Search for projects in all gitlab server by project name");
        search.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                searchProject(e);
            }
        });
    }

    private void searchProject(KeyEvent e){
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
                    projectJList.addSelectionInterval(0, filterProjectList.size());
                } else {
                    selectedProjectList.removeAll(filterProjectList);
                    projectJList.clearSelection();
                }
                setSelectedCount();
                bottomButtonState();
            }
        });
    }

    private void setSelectedCount() {
        selectedCount.setText(String.format("(%s Selected)", selectedProjectList.size()));
    }

    private List<ProjectDto> filterProjectsByProject(String searchWord){
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

        projectJList.setListData(projectList.toArray());
        projectJList.setCellRenderer(new LcheckBox());
        projectJList.setEnabled(true);
        projectJList.setSelectionModel(new DefaultListSelectionModel() {
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
            projectJList.setSelectedIndices(selectedProjectList.stream()
                    .map(o -> projectList.indexOf(o))
                    .mapToInt(Integer::valueOf)
                    .toArray());
            checkAll(projectList);
        } else {
            projectJList.clearSelection();
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
        projectJList.clearSelection();
        selectAllCheckBox.setSelected(false);
        selectedCount.setText("(0 Selected)");
    }

    private List<ProjectDto> filterProjectListByBranch(String searchWord) {
        List<GitRepository> repositories = GitUtil.getRepositories(project).stream().collect(Collectors.toList());
        RepositoryHelper.sortRepositoriesByName(repositories);
        projectDtoListByBranch  = projectDtoList.stream()
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
