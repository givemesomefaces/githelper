package gitlab.ui;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import gitlab.common.Notifier;
import gitlab.enums.OperationTypeEnum;
import gitlab.bean.*;
import lombok.Setter;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/8 15:42
 */
@Setter
public class MergeRequestDialog extends DialogWrapper {
    private JPanel contentPane;
    private JComboBox sourceBranch;
    private JComboBox targetBranch;
    private JComboBox assignee;
    private JTextField description;
    private JTextField mergeTitle;
    private JLabel assign2me;
    private JCheckBox backgroudCheckBox;
    private SelectedProjectDto selectedProjectDto;
    private Project project;
    private List<String> commonBranch;
    private List<User> currentUser;
    private Set<User> users;
    private static final String TITLE = "Create Merge Request";
    private static final String CREATING = "Merge request is creating...";
    private static final String CREATED = "Merge request created";

    public MergeRequestDialog(Project project, SelectedProjectDto selectedProjectDto, List<String> commonBranch, List<User> currentUser, Set<User> users) {
        super(true);
        this.project = project;
        this.setTitle(TITLE);
        this.selectedProjectDto = selectedProjectDto;
        this.commonBranch = commonBranch;
        this.currentUser = currentUser;
        this.users = users;
        init();
    }

    @Override
    protected void init() {
        super.init();
        sourceBranch.setModel(new DefaultComboBoxModel(commonBranch.toArray()));
        sourceBranch.setSelectedIndex(-1);
        targetBranch.setModel(new DefaultComboBoxModel(commonBranch.toArray()));
        targetBranch.setSelectedIndex(-1);
        assignee.setModel(new DefaultComboBoxModel(users.toArray()));
        assignee.setSelectedIndex(-1);
        mergeTitle.setText("merge");
        sourceBranch.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                JTextField textField = (JTextField) e.getSource();
                String text = textField.getText();
                sourceBranch.setModel(new DefaultComboBoxModel(searchBranch(text, commonBranch).toArray()));
                textField.setText(text);
                sourceBranch.showPopup();
            }
        });
        targetBranch.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                JTextField textField = (JTextField) e.getSource();
                String text = textField.getText();
                targetBranch.setModel(new DefaultComboBoxModel(searchBranch(text, commonBranch).toArray()));
                textField.setText(text);
                targetBranch.showPopup();
            }
        });
        assignee.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                searchUser(e, users);
            }
        });
        assign2me.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                assignee.setSelectedItem(currentUser.get(0));
            }
        });
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        if (backgroudCheckBox.isSelected()) {
            ProgressManager.getInstance().run(new Task.Backgroundable(project, TITLE, false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setText(CREATING);
                    createMergeRequests();
                    indicator.setText(CREATED);
                }
            });
            dispose();
        } else {
            List<Result> resultList = (List<Result>) ProgressManager.getInstance().run(new Task.WithResult(project, TITLE, false) {
                @Override
                protected Object compute(@NotNull ProgressIndicator indicator) {
                    indicator.setText(CREATING);
                    List<Result> results = createMergeRequests();
                    indicator.setText(CREATED);
                    return results;
                }
            });
            dispose();
            new ResultDialog(resultList, OperationTypeEnum.CREATE_MERGE_REQUEST.getDialogTitle()).showAndGet();
        }
    }

    private List<Result> createMergeRequests() {
        String source = (String) sourceBranch.getSelectedItem();
        String target = (String) targetBranch.getSelectedItem();
        String desc = description.getText();
        if (StringUtils.isEmpty(source) || StringUtils.isEmpty(target) || StringUtils.isEmpty(mergeTitle.getText())) {
            return Lists.newArrayList();
        }
        User user = null;
        if (assignee.getSelectedItem() != null) {
            user = (User) assignee.getSelectedItem();
        }
        User finalUser = user;
        StringBuilder info = new StringBuilder();
        StringBuilder error = new StringBuilder();
        List<Result> results = selectedProjectDto.getSelectedProjectList().stream().map(s -> {
            try {
                GitlabMergeRequest mergeRequest = selectedProjectDto.getGitLabSettingsState().api(s.getGitlabServer())
                        .createMergeRequest(s, finalUser == null ? null : finalUser.resetId(s.getGitlabServer().getApiUrl()),
                                source, target, mergeTitle.getText(), desc, false);
                Result re = new Result(mergeRequest);
                re.setType(OperationTypeEnum.CREATE_MERGE_REQUEST)
                        .setProjectName(s.getName())
                        .setChangeFilesCount(mergeRequest.getChangesCount());
                info.append(re.toString()).append("\n");
                return re;
            } catch (IOException ioException) {
                Result re = new Result(new GitlabMergeRequest());
                re.setType(OperationTypeEnum.CREATE_MERGE_REQUEST)
                        .setProjectName(s.getName())
                        .setErrorMsg(ioException.getMessage());
                error.append(re.toString()).append("\n");
                return re;
            }
        }).collect(Collectors.toList());
        Notifier.notify(project, info, error, null);
        return results;
    }

    private List<String> searchBranch(String text, List<String> commonBranch) {
        return commonBranch.stream().filter(o ->
                        (StringUtils.isNotEmpty(text)
                                && o.toLowerCase().contains(text.toLowerCase()))
                                || StringUtils.isEmpty(text))
                .collect(Collectors.toList());
    }

    private void searchUser(KeyEvent e, Set<User> users) {
        JTextField textField = (JTextField) e.getSource();
        String text = textField.getText();
        users = users.stream().filter(o ->
                        (StringUtils.isNotEmpty(textField.getText())
                                && (o.getName().toLowerCase().contains(textField.getText().toLowerCase())
                                || o.getUsername().toLowerCase().contains(textField.getText().toLowerCase())))
                                || StringUtils.isEmpty(textField.getText()))
                .collect(Collectors.toSet());
        assignee.setModel(new DefaultComboBoxModel(users.toArray()));
        textField.setText(text);
        assignee.showPopup();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (StringUtils.isBlank(mergeTitle.getText())) {
            return new ValidationInfo("Merge title cannot be empty.", mergeTitle);
        }
        if (sourceBranch.getSelectedItem() == null || StringUtils.isBlank(sourceBranch.getSelectedItem().toString())) {
            return new ValidationInfo("Source Branch cannot be empty.", sourceBranch);
        }
        if (targetBranch.getSelectedItem() == null || StringUtils.isBlank(targetBranch.getSelectedItem().toString())) {
            return new ValidationInfo("Target Branch cannot be empty.", targetBranch);
        }

        if (targetBranch.getSelectedItem() != null && sourceBranch.getSelectedItem() != null
                &&  StringUtils.equalsIgnoreCase(targetBranch.getSelectedItem().toString(), sourceBranch.getSelectedItem().toString())) {
            return new ValidationInfo("Target branch must be different from Source branch.", targetBranch);
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
