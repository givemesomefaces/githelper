package gitlab.ui;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import gitlab.OperationTypeEnum;
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
    private SelectedProjectDto selectedProjectDto;
    private Project project;

    public MergeRequestDialog(Project project, SelectedProjectDto selectedProjectDto) {
        super(true);
        this.project = project;
        this.setTitle("Create Merge Request");
        this.selectedProjectDto = selectedProjectDto;
        init();
    }

    @Override
    protected void init() {
        super.init();
        ProgressManager.getInstance().run(new Task.Modal(null, "Create Merge Request", false) {

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Loading branches...");
                List<String> commonBranch = selectedProjectDto.getSelectedProjectList().stream()
                        .map(o -> selectedProjectDto.getGitLabSettingsState().api(o.getGitlabServer())
                                .getBranchesByProject(o)
                                .stream()
                                .map(GitlabBranch::getName)
                                .collect(Collectors.toList()))
                        .collect(Collectors.toList())
                        .stream()
                        .reduce((a, b) -> CollectionUtil.intersectionDistinct(a, b).stream().collect(Collectors.toList()))
                        .orElse(Lists.newArrayList());
                commonBranch.stream().sorted(String::compareToIgnoreCase);
                sourceBranch.setModel(new DefaultComboBoxModel(commonBranch.toArray()));
                sourceBranch.setSelectedIndex(-1);
                targetBranch.setModel(new DefaultComboBoxModel(commonBranch.toArray()));
                targetBranch.setSelectedIndex(-1);
                Set<GitlabServer> serverDtos = selectedProjectDto.getSelectedProjectList().stream().map(ProjectDto::getGitlabServer).collect(Collectors.toSet());
                List<User> currentUser = serverDtos.stream().map(o -> {
                    GitlabUser m = selectedProjectDto.getGitLabSettingsState().api(o).getCurrentUser();
                    User u = new User();
                    u.setServerUserIdMap(new HashMap<>() {{
                        put(o.getApiUrl(), m.getId());
                    }});
                    u.setUsername(m.getUsername());
                    u.setName(m.getName());
                    return u;
                }).collect(Collectors.toMap(User::getUsername, Function.identity(), (a, b) -> {
                    if (MapUtil.isNotEmpty(b.getServerUserIdMap())) {
                        a.getServerUserIdMap().putAll(b.getServerUserIdMap());
                    }
                    return a;
                })).values().stream().collect(Collectors.toList());
                Set<User> users = serverDtos.stream()
                        .map(o -> selectedProjectDto.getGitLabSettingsState().api(o).getActiveUsers().stream().map(m -> {
                                    User u = new User();
                                    u.setServerUserIdMap(new HashMap<>(){{
                                        put(o.getApiUrl(), m.getId());
                                    }});
                                    u.setUsername(m.getUsername());
                                    u.setName(m.getName());
                                    return u;
                                }).collect(Collectors.toList())
                        ).flatMap(Collection::stream)
                        .collect(Collectors.toList())
                        .stream().collect(Collectors.toMap(User::getUsername, Function.identity(), (a, b) -> {
                            if (MapUtil.isNotEmpty(b.getServerUserIdMap())) {
                                a.getServerUserIdMap().putAll(b.getServerUserIdMap());
                            }
                            return a;
                        })).values().stream().collect(Collectors.toSet());

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
                indicator.setText("Common branches loaded");
            }
        });

    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        List<Result> resultList = (List<Result>) ProgressManager.getInstance().run(new Task.WithResult(null, "Create Merge Request", false) {
            @Override
            protected Object compute(@NotNull ProgressIndicator indicator) {
                indicator.setText("Merge request is creating...");
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
                List<Result> results = selectedProjectDto.getSelectedProjectList().stream().map(s -> {
                    try {
                        GitlabMergeRequest mergeRequest = selectedProjectDto.getGitLabSettingsState().api(s.getGitlabServer())
                                .createMergeRequest(s, finalUser == null ? null : finalUser.resetId(s.getGitlabServer().getApiUrl()),
                                        source, target, mergeTitle.getText(), desc, false);
                        Result re = new Result(mergeRequest);
                        re.setType(OperationTypeEnum.CREATE_MERGE_REQUEST)
                                .setProjectName(s.getName())
                                .setChangeFilesCount(mergeRequest.getChangesCount());
                        return re;
                    } catch (IOException ioException) {
                        Result re = new Result(new GitlabMergeRequest());
                        re.setType(OperationTypeEnum.CREATE_MERGE_REQUEST)
                                .setProjectName(s.getName())
                                .setErrorMsg(ioException.getMessage());
                        return re;
                    }
                }).collect(Collectors.toList());
                indicator.setText("Merge request created");
                return results;
            }
        });
        new ResultDialog(resultList, OperationTypeEnum.CREATE_MERGE_REQUEST.getDialogTitle()).showAndGet();
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
            return new ValidationInfo("Targe Branch cannot be empty.", targetBranch);
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
