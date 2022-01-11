package gitlab.ui;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import com.intellij.dvcs.ui.CloneDvcsValidationUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.commands.Git;
import git4idea.repo.GitRepository;
import gitlab.bean.UserModel;
import gitlab.common.GitCheckoutProvider;
import gitlab.dto.GitlabServerDto;
import gitlab.dto.ProjectDto;
import gitlab.helper.RepositoryHelper;
import gitlab.settings.GitLabSettingsState;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import window.LcheckBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
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
    private JLabel projectListDefaultText;

    private GitLabSettingsState gitLabSettingsState = GitLabSettingsState.getInstance();
    private List<ProjectDto> projectDtoList = new ArrayList<>();
    private List<ProjectDto> projectDtoListByBranch = new ArrayList<>();
    private Set<ProjectDto> selectedProjectList = new HashSet<>();
    private CheckoutProvider.Listener checkoutListener;
    private Project project;

    private CloneDialog cloneDialog;
    private MergeRequestDialog mergeRequestDialog;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private LoadingPanel glasspane = new LoadingPanel();
    private List<String> commonBranch = new ArrayList<>();
    private Set<UserModel> users = new HashSet<>();
    private List<UserModel> currentUser = new ArrayList<>();
    private List<ProjectDto> filterProjectList = new ArrayList<>();

    public GitLabDialog(@Nullable Project project, @Nullable Component parentComponent, boolean canBeParent, @NotNull IdeModalityType ideModalityType, boolean createSouth) {
        super(project, parentComponent, canBeParent, ideModalityType, createSouth);
        setTitle("Gitlab");
        init();
        if (gitLabSettingsState.hasSettings()) {
            projectListDefaultText.setVisible(false);
            //loading(this);
            this.project = project;
            getProjectListAndSortByName();
            initSerach();
            initRadioButton();
            initProjectList(filterProjectsByProject(null));
            initSelectAllCheckBox();
            checkoutListener = ProjectLevelVcsManager.getInstance(project).getCompositeCheckoutListener();
        } else {
            projectListDefaultText.setVisible(true);
        }
        getRootPane().setDefaultButton(cancelButton);
        initBottomButton();
    }

    public GitLabDialog(Project project) {
        super(true);
        setTitle("Gitlab");
        init();
        this.createSouthPanel().setVisible(false);
        if (gitLabSettingsState.hasSettings()) {
            projectListDefaultText.setVisible(false);
            //loading(this);
            this.project = project;
            getProjectListAndSortByName();
            initSerach();
            initRadioButton();
            initProjectList(filterProjectsByProject(null));
            initSelectAllCheckBox();
            checkoutListener = ProjectLevelVcsManager.getInstance(project).getCompositeCheckoutListener();
        } else {
            projectListDefaultText.setVisible(true);
        }

//        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(cancelButton);
        initBottomButton();
        // call onCancel() when cross is clicked
//        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
//        addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowClosing(WindowEvent e) {
//                onCancel();
//            }
//        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    private void loading(JDialog jDialog) {
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        glasspane.setBounds(100, 100, (dimension.width) / 2, (dimension.height) / 2);
        jDialog.setGlassPane(glasspane);
        glasspane.start();//开始动画加载效果
    }

    private void getProjectListAndSortByName() {
        createMergeRequestButton.setEnabled(false);
        cloneButton.setEnabled(false);
        executor.submit(() -> {
            projectDtoList = gitLabSettingsState.loadMapOfServersAndProjects(gitLabSettingsState.getGitlabServers())
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            bottomButtonState();
            Collections.sort(projectDtoList, new Comparator<ProjectDto>() {
                @Override
                public int compare(ProjectDto o1, ProjectDto o2) {
                    return StringUtils.compareIgnoreCase(o1.getName(), o2.getName());
                }
            });
            glasspane.stop();
            initProjectList(filterProjectsByProject(null));
        });
    }

    private void bottomButtonState() {
        if (CollectionUtil.isEmpty(projectDtoList) || CollectionUtil.isEmpty(selectedProjectList)) {
            createMergeRequestButton.setEnabled(false);
            cloneButton.setEnabled(false);
        }
        if (CollectionUtil.isNotEmpty(selectedProjectList)) {
            createMergeRequestButton.setEnabled(true);
            cloneButton.setEnabled(true);
        }
        if (branchNameRadioButton.isSelected()) {
            cloneButton.setEnabled(false);
        }
    }

    private void initBottomButton() {
        cloneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showCloneDialog();
            }
        });
        createMergeRequestButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showCreateMergeRequestDialog();
            }
        });
    }

    private void showCreateMergeRequestDialog() {
        mergeRequestDialog = new MergeRequestDialog();
        mergeRequestDialog.setLocationByPlatform(true);
//        mergeRequestDialog.setLocationRelativeTo(this);
        List<String> commonBranch = selectedProjectList.stream()
                .map(o -> gitLabSettingsState.api(o.getGitlabServerDto())
                        .getBranchesByProject(o)
                        .stream()
                        .map(GitlabBranch::getName)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList())
                .stream()
                .reduce((a, b) -> CollectionUtil.intersectionDistinct(a, b).stream().collect(Collectors.toList()))
                .orElse(Lists.newArrayList());
        commonBranch.stream().sorted(String::compareToIgnoreCase);
        mergeRequestDialog.getSourceBranch().setModel(new DefaultComboBoxModel(commonBranch.toArray()));
        mergeRequestDialog.getSourceBranch().setSelectedIndex(-1);
        mergeRequestDialog.getTargetBranch().setModel(new DefaultComboBoxModel(commonBranch.toArray()));
        mergeRequestDialog.getTargetBranch().setSelectedIndex(-1);
        Set<GitlabServerDto> serverDtos = selectedProjectList.stream().map(ProjectDto::getGitlabServerDto).collect(Collectors.toSet());
        currentUser = serverDtos.stream().map(o -> {
            GitlabUser m = gitLabSettingsState.api(o).getCurrentUser();
            UserModel u = new UserModel();
            u.setServerUserIdMap(new HashMap<>() {{
                put(o.getApiUrl(), m.getId());
            }});
            u.setUsername(m.getUsername());
            u.setName(m.getName());
            return u;
        }).collect(Collectors.toMap(UserModel::getUsername, Function.identity(), (a, b) -> {
            if (MapUtil.isNotEmpty(b.getServerUserIdMap())) {
                a.getServerUserIdMap().putAll(b.getServerUserIdMap());
            }
            return a;
        })).values().stream().collect(Collectors.toList());
        users = serverDtos.stream()
                .map(o -> gitLabSettingsState.api(o).getActiveUsers().stream().map(m -> {
                            UserModel u = new UserModel();
                            u.setServerUserIdMap(new HashMap<>(){{
                                put(o.getApiUrl(), m.getId());
                            }});
                            u.setUsername(m.getUsername());
                            u.setName(m.getName());
                            return u;
                        }).collect(Collectors.toList())
                ).flatMap(Collection::stream)
                .collect(Collectors.toList())
                .stream().collect(Collectors.toMap(UserModel::getUsername, Function.identity(), (a, b) -> {
                    if (MapUtil.isNotEmpty(b.getServerUserIdMap())) {
                        a.getServerUserIdMap().putAll(b.getServerUserIdMap());
                    }
                    return a;
                })).values().stream().collect(Collectors.toSet());

        mergeRequestDialog.getAssignee().setModel(new DefaultComboBoxModel(users.toArray()));
        mergeRequestDialog.getAssignee().setSelectedIndex(-1);
        mergeRequestDialog.getMergeTitle().setText("merge");
        mergeRequestDialog.getButtonOK().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String source = (String) mergeRequestDialog.getSourceBranch().getSelectedItem();
                String target = (String) mergeRequestDialog.getTargetBranch().getSelectedItem();
                String mergeTitle = mergeRequestDialog.getMergeTitle().getText();
                String desc = mergeRequestDialog.getDescription().getText();
                if (StringUtils.isEmpty(source) || StringUtils.isEmpty(target) || StringUtils.isEmpty(mergeTitle)) {
                    return;
                }
                UserModel user = null;
                if (mergeRequestDialog.getAssignee().getSelectedItem() != null) {
                    user = (UserModel) mergeRequestDialog.getAssignee().getSelectedItem();
                }
                mergeRequestDialog.dispose();
                dispose();
                UserModel finalUser = user;
                selectedProjectList.stream().forEach(s -> {
                    try {
                        GitlabMergeRequest mergeRequest = gitLabSettingsState.api(s.getGitlabServerDto())
                                .createMergeRequest(s, finalUser == null ? null : finalUser.resetId(s.getGitlabServerDto().getApiUrl()),
                                        source, target, mergeTitle, desc, false);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                });
            }
        });
        mergeRequestDialog.getSourceBranch().getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                JTextField textField = (JTextField) e.getSource();
                String text = textField.getText();
                mergeRequestDialog.getSourceBranch().setModel(new DefaultComboBoxModel(searchBranch(text, commonBranch).toArray()));
                textField.setText(text);
                mergeRequestDialog.getSourceBranch().showPopup();
            }
        });
        mergeRequestDialog.getTargetBranch().getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                JTextField textField = (JTextField) e.getSource();
                String text = textField.getText();
                mergeRequestDialog.getTargetBranch().setModel(new DefaultComboBoxModel(searchBranch(text, commonBranch).toArray()));
                textField.setText(text);
                mergeRequestDialog.getTargetBranch().showPopup();
            }
        });
        mergeRequestDialog.getAssignee().getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                searchUser(e, users);
            }
        });
        mergeRequestDialog.getAssign2me().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                mergeRequestDialog.getAssignee().setSelectedItem(currentUser.get(0));
            }
        });
        mergeRequestDialog.pack();
        mergeRequestDialog.setVisible(true);
        removeLoading();
    }

    private void removeLoading() {
        glasspane.stop();
    }

    private void searchUser(KeyEvent e, Set<UserModel> users) {
        JTextField textField = (JTextField) e.getSource();
        String text = textField.getText();
        users = users.stream().filter(o ->
                (StringUtils.isNotEmpty(textField.getText())
                        && (o.getName().toLowerCase().contains(textField.getText().toLowerCase())
                        || o.getUsername().toLowerCase().contains(textField.getText().toLowerCase())))
                        || StringUtils.isEmpty(textField.getText()))
                .collect(Collectors.toSet());
        mergeRequestDialog.getAssignee().setModel(new DefaultComboBoxModel(users.toArray()));
        textField.setText(text);
        mergeRequestDialog.getAssignee().showPopup();
    }

    private List<String> searchBranch(String text, List<String> commonBranch) {
        return commonBranch.stream().filter(o ->
                (StringUtils.isNotEmpty(text)
                        && o.toLowerCase().contains(text.toLowerCase()))
                        || StringUtils.isEmpty(text))
                .collect(Collectors.toList());
    }

    private void showCloneDialog() {
        cloneDialog = new CloneDialog(project, contentPane, true, IdeModalityType.IDE, false);
//        cloneDialog.setLocationByPlatform(true);
//        cloneDialog.setLocationRelativeTo(this);
        cloneDialog.getButtonOK().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                ValidationInfo destinationValidation = CloneDvcsValidationUtils.createDestination(cloneDialog.getDirectory().getText());
                if (destinationValidation != null) {
                    LOG.error("Unable to create destination directory", destinationValidation.message);
                    return;
                }

                LocalFileSystem lfs = LocalFileSystem.getInstance();
                File file = new File(cloneDialog.getDirectory().getText());
                VirtualFile destinationParent = lfs.findFileByIoFile(file);
                if (destinationParent == null) {
                    destinationParent = lfs.refreshAndFindFileByIoFile(file);
                }
                if (destinationParent == null) {
                    LOG.error("Clone Failed. Destination doesn't exist");
                    return;
                }
                cloneDialog.disposeIfNeeded();
                dispose();

                VirtualFile finalDestinationParent = destinationParent;
                selectedProjectList.stream().forEach(s -> {
                    GitCheckoutProvider.clone(project, Git.getInstance(), checkoutListener, finalDestinationParent,
                            s.getSshUrl(), s.getName(), cloneDialog.getDirectory().getText());
                });
            }
        });
        cloneDialog.showAndGet();
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
                    selectedProjectList.clear();
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
                    createMergeRequestButton.setEnabled(false);
                    cloneButton.setEnabled(false);
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
                    createMergeRequestButton.setEnabled(false);
                    cloneButton.setEnabled(false);
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
                                                .contains(o.getGitlabServerDto().getRepositoryUrl().toLowerCase()))))
                ).collect(Collectors.toList());
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

}
