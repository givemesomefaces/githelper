package gitlab;

import com.intellij.dvcs.ui.CloneDvcsValidationUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import gitlab.common.GitCheckoutProvider;
import gitlab.dto.GitlabServerDto;
import gitlab.dto.ProjectDto;
import gitlab.settings.GitLabSettingsState;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabUser;
import window.LcheckBox;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    private JRadioButton branchNameRadioButton;
    private JRadioButton projectNameRadioButton;
    private JButton cloneButton;
    private JButton createMergeRequestButton;
    private JCheckBox selectAllCheckBox;
    private JLabel selectedCount;

    private GitLabSettingsState gitLabSettingsState = GitLabSettingsState.getInstance();
    private List<ProjectDto> projectDtoList = new ArrayList<>();
    private List<ProjectDto> selectedProjectList = new ArrayList<>();
    private CheckoutProvider.Listener checkoutListener;
    private Project project;

    private CloneDialog cloneDialog;
    private MergeRequestDialog mergeRequestDialog;

    public GitLabDialog(Project project) {
        this.project = project;
        initSerach();
        initRadioButton();
        initProjectList(null);
        initSelectAllCheckBox();
        checkoutListener = ProjectLevelVcsManager.getInstance(project).getCompositeCheckoutListener();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(cloneButton);
        initBottomButton();
        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
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
        mergeRequestDialog.getMergeTitle().setText(search.getText());
        String source = null;//mergeRequestDialog.getSourceBranch().getText();
        String target = null;//mergeRequestDialog.getTargetBranch().getText();
        String mergeTitle = mergeRequestDialog.getMergeTitle().getText();
        String desc = mergeRequestDialog.getDescription().getText();
        mergeRequestDialog.getButtonOK().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedProjectList.stream().forEach(s -> {
                    try {
                        gitLabSettingsState.api(s.getGitlabServerDto()).createMergeRequest(s, new GitlabUser(), source, target, mergeTitle, desc, false);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                });
            }
        });
        mergeRequestDialog.pack();
        mergeRequestDialog.setVisible(true);
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
                try {
                    searchProject(e);
                } catch (BadLocationException badLocationException) {
                    badLocationException.printStackTrace();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                try {
                    searchProject(e);
                } catch (BadLocationException badLocationException) {
                    badLocationException.printStackTrace();
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                try {
                    searchProject(e);
                } catch (BadLocationException badLocationException) {
                    badLocationException.printStackTrace();
                }
            }
        });
    }

    private void searchProject(DocumentEvent e) throws BadLocationException{
        String searchWord = e.getDocument().getText(e.getDocument().getStartPosition().getOffset(), e.getDocument().getLength());
        if (projectNameRadioButton.isSelected()) {
            initProjectList(searchWord);
        }

        if (branchNameRadioButton.isSelected()) {

        }

    }

    private void initSelectAllCheckBox() {
        selectAllCheckBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                JCheckBox jCheckBox = (JCheckBox) e.getSource();
                if (jCheckBox.isSelected()) {
                    projectList.setSelectionInterval(0, projectDtoList.size());
                } else {
                    projectList.clearSelection();
                }
                setSelectedCount();
            }
        });
    }

    private void setSelectedCount() {
        selectedCount.setText(String.format("(Selected %s)", selectedProjectList.size()));
    }

    private void initProjectList(String searchWord) {
        projectDtoList = gitLabSettingsState.loadMapOfServersAndProjects(gitLabSettingsState.getGitlabServers())
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<ProjectDto> filterRepositories = projectDtoList
                .stream()
                .filter(o ->
                        (StringUtils.isNotEmpty(searchWord)
                                && o.getName().toLowerCase().contains(searchWord.toLowerCase()))
                                || StringUtils.isEmpty(searchWord)
                ).collect(Collectors.toList());


        projectList.setListData(filterRepositories.stream().map(ProjectDto::getName).collect(Collectors.toList()).toArray());
        projectList.setCellRenderer(new LcheckBox());
        projectList.setEnabled(true);
        projectList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (super.isSelectedIndex(index0)) {
                    selectedProjectList.remove(filterRepositories.get(index0));
                    super.removeSelectionInterval(index0, index1);
                } else {
                    selectedProjectList.add(filterRepositories.get(index0));
                    super.addSelectionInterval(index0, index1);
                }
            }
        });
    }

    private void initRadioButton() {
        ButtonGroup btnGroup = new ButtonGroup();
        btnGroup.add(branchNameRadioButton);
        btnGroup.add(projectNameRadioButton);
        projectNameRadioButton.setSelected(true);
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
