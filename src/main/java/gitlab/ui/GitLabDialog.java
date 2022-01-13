package gitlab.ui;

import cn.hutool.core.collection.CollectionUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import gitlab.bean.ProjectDto;
import gitlab.bean.SelectedProjectDto;
import gitlab.helper.RepositoryHelper;
import gitlab.settings.GitLabSettingsState;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import window.LcheckBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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

    private GitLabSettingsState gitLabSettingsState = GitLabSettingsState.getInstance();
    private List<ProjectDto> projectDtoList = new ArrayList<>();
    private List<ProjectDto> projectDtoListByBranch = new ArrayList<>();
    private Set<ProjectDto> selectedProjectList = new HashSet<>();
    private Project project;

    private List<ProjectDto> filterProjectList = new ArrayList<>();

    public GitLabDialog(@Nullable Project project, @Nullable Component parentComponent, boolean canBeParent, @NotNull IdeModalityType ideModalityType, boolean createSouth) {
        super(project, parentComponent, canBeParent, ideModalityType, createSouth);
        setTitle("GitLab");
        init();
        this.project = project;
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
        ProgressManager.getInstance().run(new Task.Modal(project, "GitLab", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Loading projects...");
                projectDtoList = gitLabSettingsState.loadMapOfServersAndProjects(gitLabSettingsState.getGitlabServers())
                        .values()
                        .stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
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
                indicator.setText("Projects loaded");
            }

            @Override
            public void onCancel() {
                super.onCancel();
            }
        });
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
    }

    private void bottomButtonState() {
        if (CollectionUtil.isEmpty(projectDtoList) || CollectionUtil.isEmpty(selectedProjectList)) {
            createMergeRequestButton.setEnabled(false);
            cloneButton.setEnabled(false);
            mergeButton.setEnabled(false);
        }
        if (CollectionUtil.isNotEmpty(selectedProjectList)) {
            createMergeRequestButton.setEnabled(true);
            cloneButton.setEnabled(true);
            mergeButton.setEnabled(true);
        }
        if (branchNameRadioButton.isSelected()) {
            cloneButton.setEnabled(false);
        }
    }

    private void initBottomButton() {
        mergeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new MergeDialog(new SelectedProjectDto()
                        .setGitLabSettingsState(gitLabSettingsState)
                        .setSelectedProjectList(selectedProjectList)).showAndGet();
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
                new MergeRequestDialog(project, new SelectedProjectDto()
                        .setGitLabSettingsState(gitLabSettingsState)
                        .setSelectedProjectList(selectedProjectList)).showAndGet();
            }
        });
    }

    private void initSerach() {
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
        if (selectedProjectList.size() == projectDtoList.size()
                || selectedProjectList.size() == projectDtoListByBranch.size()
                || selectedProjectList.size() == filterProjectList.size()) {
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
                JCheckBox jCheckBox = (JCheckBox) e.getSource();
                if (jCheckBox.isSelected()) {
                    selectedProjectList.addAll(filterProjectList);
                    projectJList.setSelectionInterval(0, filterProjectList.size());
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
        selectedCount.setText(String.format("(Selected %s)", selectedProjectList.size()));
    }

    private List<ProjectDto> filterProjectsByProject(String searchWord){
        return filterProjectList = projectDtoList
                .stream()
                .filter(o ->
                        (StringUtils.isNotEmpty(searchWord)
                                && o.getName().toLowerCase().contains(searchWord.toLowerCase()))
                                || StringUtils.isEmpty(searchWord)
                ).collect(Collectors.toList());
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
                JRadioButton jRadioButton = (JRadioButton) e.getSource();
                if (jRadioButton.isSelected()) {
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
                JRadioButton jRadioButton = (JRadioButton) e.getSource();
                if (jRadioButton.isSelected()) {
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
        selectedCount.setText("(Selected 0)");
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
                        .anyMatch(z -> z.getRoot().getName().toLowerCase().contains(o.getName())
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
