package gitlab.ui;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import com.intellij.dvcs.ui.CloneDvcsValidationUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import gitlab.bean.UserModel;
import gitlab.common.GitCheckoutProvider;
import gitlab.dto.GitlabServerDto;
import gitlab.dto.ProjectDto;
import gitlab.settings.GitLabSettingsState;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabUser;
import window.LcheckBox;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/8 16:14
 */
public class GitLabDialog extends JDialog {
    private static final Logger LOG = Logger.getInstance(GitLabDialog.class);
    private JPanel contentPane;
    private JList projectList;
    private JTextField search;
//    private JRadioButton branchNameRadioButton;
//    private JRadioButton projectNameRadioButton;
    private JButton cloneButton;
    private JButton createMergeRequestButton;
    private JCheckBox selectAllCheckBox;
    private JLabel selectedCount;
    private JButton cancleButton;

    private GitLabSettingsState gitLabSettingsState = GitLabSettingsState.getInstance();
    private List<ProjectDto> projectDtoList = new ArrayList<>();
    private List<ProjectDto> selectedProjectList = new ArrayList<>();
    private CheckoutProvider.Listener checkoutListener;
    private Project project;

    private CloneDialog cloneDialog;
    private MergeRequestDialog mergeRequestDialog;

    public GitLabDialog(Project project) {
        this.setTitle("Gitlab");
        this.project = project;
        sortProjectListByName();
        initSerach();
        //initRadioButton();
        initProjectList(null);
        initSelectAllCheckBox();
        checkoutListener = ProjectLevelVcsManager.getInstance(project).getCompositeCheckoutListener();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(cancleButton);
        initBottomButton();
        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
        cancleButton.addActionListener(new ActionListener() {
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

    private void sortProjectListByName() {
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
        mergeRequestDialog.setLocationRelativeTo(this);
        mergeRequestDialog.getMergeTitle().setText("WIP:");
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
        List<UserModel> currentUser = serverDtos.stream().map(o -> {
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
        Set<UserModel> users = serverDtos.stream()
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
                UserModel user = (UserModel) mergeRequestDialog.getAssignee().getSelectedItem();
                mergeRequestDialog.dispose();
                dispose();
                selectedProjectList.stream().forEach(s -> {
                    try {
                        GitlabMergeRequest mergeRequest = gitLabSettingsState.api(s.getGitlabServerDto())
                                .createMergeRequest(s, user.resetId(s.getGitlabServerDto().getApiUrl()),
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
        cloneDialog = new CloneDialog();
        cloneDialog.setLocationRelativeTo(this);
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
                cloneDialog.dispose();
                dispose();

                VirtualFile finalDestinationParent = destinationParent;
                selectedProjectList.stream().forEach(s -> {
                    GitCheckoutProvider.clone(project, Git.getInstance(), checkoutListener, finalDestinationParent,
                            s.getSshUrl(), s.getName(), cloneDialog.getDirectory().getText());
                });
            }
        });
        cloneDialog.pack();
        cloneDialog.setVisible(true);
    }

    private void initSerach() {
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchProject(e);
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                searchProject(e);
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                searchProject(e);
            }
        });
    }

    private void searchProject(DocumentEvent e){
        String searchWord = null;
        try {
            searchWord = e.getDocument().getText(e.getDocument().getStartPosition().getOffset(), e.getDocument().getLength());
        } catch (BadLocationException badLocationException) {
            badLocationException.printStackTrace();
        }
        initProjectList(searchWord);
//        if (projectNameRadioButton.isSelected()) {
//        }
//
//        if (branchNameRadioButton.isSelected()) {
//
//        }

    }

    private void initSelectAllCheckBox() {
        selectAllCheckBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                JCheckBox jCheckBox = (JCheckBox) e.getSource();
                if (jCheckBox.isSelected()) {
                    selectedProjectList.addAll(projectDtoList);
                    projectList.setSelectionInterval(0, projectDtoList.size());
                } else {
                    selectedProjectList.clear();
                    projectList.clearSelection();
                }
                setSelectedCount();
                bottomButtonState();
            }
        });
    }

    private void setSelectedCount() {
        selectedCount.setText(String.format("(Selected %s)", selectedProjectList.size()));
    }

    private void initProjectList(String searchWord) {
        hideButton(searchWord);
        List<ProjectDto> filterRepositories = projectDtoList
                .stream()
                .filter(o ->
                        (StringUtils.isNotEmpty(searchWord)
                                && o.getName().toLowerCase().contains(searchWord.toLowerCase()))
                                || StringUtils.isEmpty(searchWord)
                ).collect(Collectors.toList());


        projectList.setListData(filterRepositories.toArray());
        projectList.setCellRenderer(new LcheckBox());
        projectList.setEnabled(true);
        projectList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (super.isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                    selectedProjectList.remove(filterRepositories.get(index0));
                } else {
                    super.addSelectionInterval(index0, index1);
                    selectedProjectList.add(filterRepositories.get(index0));
                    checkAll(filterRepositories);
                }
                setSelectedCount();
                bottomButtonState();
            }
        });
        if (CollectionUtil.isNotEmpty(selectedProjectList)) {
            projectList.setSelectedIndices(selectedProjectList.stream()
                    .map(o -> filterRepositories.indexOf(o))
                    .mapToInt(Integer::valueOf)
                    .toArray());
            checkAll(filterRepositories);
        } else {
            projectList.clearSelection();
        }
    }

    private void hideButton(String searchWord) {
        if (StringUtils.isNotEmpty(searchWord)) {
            selectAllCheckBox.setEnabled(false);
        } else {
            selectAllCheckBox.setEnabled(true);
        }
    }

    private void checkAll(List<ProjectDto> filterRepositories) {
        if (selectedProjectList.size() == projectDtoList.size()
                && filterRepositories.size() == projectDtoList.size()) {
            selectAllCheckBox.setSelected(true);
        }
    }

    /*private void initRadioButton() {
        ButtonGroup btnGroup = new ButtonGroup();
        btnGroup.add(branchNameRadioButton);
        btnGroup.add(projectNameRadioButton);
        projectNameRadioButton.setSelected(true);
    }*/

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

}
