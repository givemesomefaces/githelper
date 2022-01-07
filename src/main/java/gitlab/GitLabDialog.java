package gitlab;

import com.intellij.openapi.vfs.VirtualFile;
import git4idea.repo.GitRepository;
import gitlab.dto.ProjectDto;
import gitlab.settings.GitLabSettingsState;
import window.LcheckBox;

import javax.swing.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class GitLabDialog extends JDialog {
    private JPanel contentPane;
    private JList projectList;
    private JTextField textField1;
    private JRadioButton branchNameRadioButton;
    private JRadioButton projectNameRadioButton;
    private JButton cloneButton;
    private JButton createMergeRequestButton;
    private JCheckBox selectAllCheckBox;

    private GitLabSettingsState gitLabSettingsState = GitLabSettingsState.getInstance();
    private List<ProjectDto> projectDtoList = new ArrayList<>();

    public GitLabDialog() {
        initRadioButton();
        initProjectList();
        initSelectAllCheckBox();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(cloneButton);

        cloneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

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
            }
        });
    }

    private void initProjectList() {
        projectDtoList = gitLabSettingsState.loadMapOfServersAndProjects(gitLabSettingsState.getGitlabServers())
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        projectList.setListData(projectDtoList.stream().map(ProjectDto::getName).collect(Collectors.toList()).toArray());
        projectList.setCellRenderer(new LcheckBox());
        projectList.setEnabled(true);
        projectList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (super.isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                } else {
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

    public static void main(String[] args) {
        GitLabDialog dialog = new GitLabDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
