package gitlab.ui;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.github.lvlifeng.githelper.Bundle;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import gitlab.bean.MergeRequest;
import gitlab.bean.Result;
import gitlab.bean.SelectedProjectDto;
import gitlab.bean.User;
import gitlab.common.Notifier;
import gitlab.enums.OperationTypeEnum;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabMergeRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import window.LcheckBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static gitlab.common.Constants.NAME_SPLIT_SYMBOL;

public class MergeRequestDialogV2 extends DialogWrapper {
    private static final String CREATING = "Merge request is creating...";
    private static final String CREATED = "Merge request created";
    private JPanel contentPane;
    private JComboBox sourceBranch;
    private JComboBox targetBranch;
    private JComboBox assignee;
    private JTextField description;
    private JTextField mergeTitle;
    private JLabel assign2me;
    private JCheckBox backgroudCheckBox;
    private JCheckBox jumpToMergeMenuCheckBox;
    private SelectedProjectDto selectedProjectDto;
    private Project project;
    private List<String> commonBranch;
    private List<User> currentUser;
    private Set<User> users;

    public MergeRequestDialogV2(Project project, SelectedProjectDto selectedProjectDto, List<String> commonBranch, List<User> currentUser, Set<User> users) {
        super(true);
        this.project = project;
        this.setTitle(Bundle.message("createMergeRequestDialogTitle"));
        this.selectedProjectDto = selectedProjectDto;
        this.commonBranch = commonBranch;
        this.currentUser = currentUser;
        this.users = users;
        init();
    }

    @Override
    protected void init() {
        super.init();
        sortDatas();
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
        assign2me.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        assign2me.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() != 1) {
                    return;
                }
                assignee.setSelectedItem(currentUser.get(0));
            }
        });
    }

    private void sortDatas() {
        CollectionUtil.sort(commonBranch, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return StringUtils.compareIgnoreCase(o1, o2);
            }
        });
        CollectionUtil.sort(users, new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                return StringUtils.compareIgnoreCase(o1.getUsername(), o2.getUsername());
            }
        });
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        if (backgroudCheckBox.isSelected()) {
            ProgressManager.getInstance().run(new Task.Backgroundable(project, Bundle.message("createMergeRequestDialogTitle"), false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setText(CREATING);
                    createMergeRequests(indicator);
                    indicator.setText(CREATED);
                }
            });
            dispose();
        } else {
            List<Result> resultList = (List<Result>) ProgressManager.getInstance().run(new Task.WithResult(project, Bundle.message("createMergeRequestDialogTitle"), false) {
                @Override
                protected Object compute(@NotNull ProgressIndicator indicator) {
                    indicator.setText(CREATING);
                    List<Result> results = createMergeRequests(indicator);
                    indicator.setText(CREATED);
                    return results;
                }
            });
            dispose();
            new ResultDialog(resultList, OperationTypeEnum.CREATE_MERGE_REQUEST.getDialogTitle()).showAndGet();
        }
        if (jumpToMergeMenuCheckBox.isSelected()) {
            showMergeDialog();
        }
    }

    private void showMergeDialog() {
        ProgressManager.getInstance().run(new Task.Modal(project, Bundle.message("mergeRequestDialogTitle"), true) {
            List<MergeRequest> gitlabMergeRequests = new ArrayList<>();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Loading merge requests...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                AtomicInteger index = new AtomicInteger(1);
                gitlabMergeRequests = selectedProjectDto.getSelectedProjectList()
                        .stream()
                        .filter(o -> !indicator.isCanceled())
                        .map(o -> {
                            indicator.setText2("(" + index.getAndIncrement() + "/" + selectedProjectDto.getSelectedProjectList().size() + ") " + o.getName());
                            List<GitlabMergeRequest> openMergeRequest = null;
                            try {
                                openMergeRequest = selectedProjectDto.getGitLabSettingsState()
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
                new MergeDialog(project, selectedProjectDto,
                        gitlabMergeRequests).showAndGet();
            }
        });
    }

    private List<Result> createMergeRequests(ProgressIndicator indicator) {
        String source = (String) sourceBranch.getSelectedItem();
        String targetStr = (String) targetBranch.getSelectedItem();
        String desc = description.getText();
        if (StringUtils.isEmpty(source) || StringUtils.isEmpty(targetStr) || StringUtils.isEmpty(mergeTitle.getText())) {
            return Lists.newArrayList();
        }
        Set<String> targets = Arrays.stream(StringUtils.split(targetStr, NAME_SPLIT_SYMBOL)).collect(Collectors.toSet());
        User user = null;
        if (assignee.getSelectedItem() != null) {
            user = (User) assignee.getSelectedItem();
        }
        User finalUser = user;
        StringBuilder info = new StringBuilder();
        StringBuilder error = new StringBuilder();
        AtomicInteger index = new AtomicInteger(1);
        List<Result> results = selectedProjectDto.getSelectedProjectList().stream().map(s -> {
                    indicator.setText2("(" + index.getAndIncrement() + "/" + selectedProjectDto.getSelectedProjectList().size() + ") " + s.getName());
                    return targets.stream().map(target -> {
                        try {
                            GitlabMergeRequest mergeRequest = selectedProjectDto.getGitLabSettingsState().api(s.getGitlabServer())
                                    .createMergeRequest(s, finalUser == null ? null : finalUser.resetId(s.getGitlabServer().getApiUrl()),
                                            source, target, mergeTitle.getText(), desc, false);
                            Result re = new Result(mergeRequest);
                            re.setType(OperationTypeEnum.CREATE_MERGE_REQUEST)
                                    .setProjectName(s.getName())
                                    .setChangeFilesCount(mergeRequest.getChangesCount());
                            info.append("<a href=\"" + re.toString().replace(" [ChangeFiles:\" + getChangeFilesCount() + \"]",
                                    "") + "\">" + re + "</a>").append("\n");
                            return re;
                        } catch (IOException ioException) {
                            Result re = new Result(new GitlabMergeRequest());
                            re.setType(OperationTypeEnum.CREATE_MERGE_REQUEST)
                                    .setProjectName(s.getName())
                                    .setErrorMsg(ioException.getMessage());
                            error.append(re).append("\n");
                            return re;
                        }
                    }).collect(Collectors.toList());
                }).flatMap(Collection::stream)
                .collect(Collectors.toList());
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
        if (sourceBranch.getSelectedItem() != null
                && StringUtils.isNotBlank(sourceBranch.getSelectedItem().toString())
                && !commonBranch.contains(sourceBranch.getSelectedItem().toString())) {
            return new ValidationInfo("This source branch does not exist, please choose again!", sourceBranch);
        }
        if (targetBranch.getSelectedItem() == null || StringUtils.isBlank(targetBranch.getSelectedItem().toString())) {
            return new ValidationInfo("Target Branch cannot be empty.", targetBranch);
        }

        if (targetBranch.getSelectedItem() != null
                && StringUtils.isNotBlank(targetBranch.getSelectedItem().toString())) {
            Set<String> targets = Arrays.stream(StringUtils.split(targetBranch.getSelectedItem().toString(), NAME_SPLIT_SYMBOL))
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            Set<String> notExistTargets = targets.stream().filter(target -> !commonBranch.contains(target)).collect(Collectors.toSet());
            if (CollectionUtil.isNotEmpty(notExistTargets)) {
                return new ValidationInfo(String.format("This target branch does not exist, Please reselect! %s", notExistTargets), targetBranch);
            }
        }

        if (targetBranch.getSelectedItem() != null && sourceBranch.getSelectedItem() != null) {
            Set<String> targets = Arrays.stream(StringUtils.split(targetBranch.getSelectedItem().toString(), NAME_SPLIT_SYMBOL))
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            if (targets.contains(sourceBranch.getSelectedItem().toString().toLowerCase())) {
                return new ValidationInfo(String.format("Target branch must be different from Source branch. [%s]", sourceBranch.getSelectedItem().toString()), targetBranch);
            }
        }
        if (assignee.getSelectedItem() != null
                && StringUtils.isNotBlank(assignee.getSelectedItem().toString())
                && !users.stream().map(User::toString).collect(Collectors.toList()).contains(assignee.getSelectedItem().toString())) {
            return new ValidationInfo("This user does not exist, Please reselect!", assignee);
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }


    private JPanel getTargetsJpanel() {
        JPanel targetPanel = new JPanel();

        // targetList
        JScrollPane targetScrollPane = new JBScrollPane(targetPanel);
        JBList targetList = new JBList();
        targetList.setModel(new DefaultListModel());
        targetList.setCellRenderer(new LcheckBox());
//        targetList.setListData(commonBranch.toArray());
        targetList.setEnabled(true);
        targetList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (super.isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                } else {
                    super.addSelectionInterval(index0, index1);
                }
            }
        });
        targetScrollPane.add(targetList);

        // search
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JTextField searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(200, 30));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
//                List<String> searchBranch = searchBranch(searchField.getText(), commonBranch);
//                targetList.setModel(new DefaultListModel());
//                targetList.setListData(searchBranch.toArray());
            }
        });

        searchPanel.add(searchField);

        targetPanel.add(searchPanel);
        targetPanel.add(targetScrollPane);
        return targetPanel;
    }

}
