package com.github.itisokey.githelper.gitlab.ui;

import cn.hutool.core.collection.CollectionUtil;
import com.github.lvlifeng.githelper.Bundle;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.github.itisokey.githelper.gitlab.api.GitlabRestApi;
import com.github.itisokey.githelper.gitlab.bean.MergeRequest;
import com.github.itisokey.githelper.gitlab.bean.Result;
import com.github.itisokey.githelper.gitlab.bean.SelectedProjectDto;
import com.github.itisokey.githelper.gitlab.common.Notifier;
import com.github.itisokey.githelper.gitlab.enums.MergeStatusEnum;
import com.github.itisokey.githelper.gitlab.enums.OperationTypeEnum;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabMergeRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.github.itisokey.githelper.window.LcheckBox;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Lv LiFeng
 * @date 2022/1/12 01:04
 */
public class MergeDialog extends DialogWrapper {
    private static final String CLOSING_TITLE = "Close Requests";
    private static final String CLOSED = "Merge requests closed";
    private static final String CLOSING = "Closing merge requests";
    private static final String MERGED = "Requests merged";
    private static final String MERGING = "Merging requests";
    private JPanel contentPane;
    private JList mergeRequestList;
    private JComboBox sourceBranch;
    private JComboBox status;
    private JCheckBox selectAllCheckBox;
    private JLabel selected;
    private JComboBox targetBranch;
    private JButton cancelButton;
    private JButton closeButton;
    private JButton mergeButton;
    private JCheckBox backgroudCheckBox;
    private final Set<MergeRequest> selectedMergeRequests = new HashSet<>();
    private final SelectedProjectDto selectedProjectDto;
    private List<MergeRequest> filterMergeRequest;
    private final List<MergeRequest> gitlabMergeRequests;
    private final Project project;
    private Set<String> allSourceBranches;
    private Set<String> allMergeStatus;
    private String inputSourceBranch = null;
    private String inputTargetBranch = null;
    private Set<String> allTargetBranches;

    public MergeDialog(Project project, SelectedProjectDto selectedProjectDto, List<MergeRequest> gitlabMergeRequests) {
        super(null, null, true, IdeModalityType.IDE, false);
        setTitle(Bundle.message("mergeRequestDialogTitle"));
        this.project = project;
        this.selectedProjectDto = selectedProjectDto;
        this.gitlabMergeRequests = gitlabMergeRequests;
        this.createSouthPanel().setVisible(false);
        getRootPane().setDefaultButton(cancelButton);
        init();
        initData();
        if (CollectionUtil.isEmpty(gitlabMergeRequests)) {
            return;
        }
        initMergeRequestList(searchMergeRequest());
        initButton();
        initCheckAll();
    }

    private void initData() {
        cancelButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() != 1) {
                    return;
                }
                dispose();
            }
        });
        if (CollectionUtil.isEmpty(gitlabMergeRequests)) {
            mergeButton.setEnabled(false);
            closeButton.setEnabled(false);
            selectAllCheckBox.setEnabled(false);
            return;
        }
        gitlabMergeRequests.sort(Comparator.comparing(MergeRequest::getProjectName));

        allSourceBranches = gitlabMergeRequests.stream().map(MergeRequest::getSourceBranch).collect(Collectors.toSet());
        allTargetBranches = gitlabMergeRequests.stream().map(MergeRequest::getTargetBranch).collect(Collectors.toSet());
        allMergeStatus = gitlabMergeRequests.stream().map(MergeRequest::getMergeStatus).collect(Collectors.toSet());

        sourceBranch.setModel(new DefaultComboBoxModel(allSourceBranches.toArray()));
        sourceBranch.setSelectedIndex(-1);
        targetBranch.setModel(new DefaultComboBoxModel(allTargetBranches.toArray()));
        targetBranch.setSelectedIndex(-1);
        status.setModel(new DefaultComboBoxModel(allMergeStatus.toArray()));
        status.insertItemAt("all", 0);
        status.setSelectedItem("all");
    }

    private void initButton() {
        closeButton.setEnabled(false);
        mergeButton.setEnabled(false);
        status.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                initMergeRequestList(searchMergeRequest());
            }
        });
        sourceBranch.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                inputSourceBranch = null;
                initMergeRequestList(searchMergeRequest());
            }
        });
        sourceBranch.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                JTextField textField = (JTextField) e.getSource();
                String text = textField.getText();
                sourceBranch.setModel(new DefaultComboBoxModel(searchBranch(text, allSourceBranches).toArray()));
                sourceBranch.setSelectedItem(null);
                textField.setText(text);
                sourceBranch.showPopup();
                inputSourceBranch = text;
                initMergeRequestList(searchMergeRequest());
            }
        });
        targetBranch.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                inputTargetBranch = null;
                initMergeRequestList(searchMergeRequest());
            }
        });
        targetBranch.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                JTextField textField = (JTextField) e.getSource();
                String text = textField.getText();
                targetBranch.setModel(new DefaultComboBoxModel(searchBranch(text, allTargetBranches).toArray()));
                targetBranch.setSelectedItem(null);
                textField.setText(text);
                targetBranch.showPopup();
                inputTargetBranch = text;
                initMergeRequestList(searchMergeRequest());
            }
        });
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() != 1) {
                    return;
                }
                if (backgroudCheckBox.isSelected()) {
                    ProgressManager.getInstance().run(new Task.Backgroundable(project, CLOSING_TITLE, false) {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            indicator.setText(CLOSING);
                            closeMergeRequest(indicator);
                            indicator.setText(CLOSED);
                        }
                    });
                    dispose();
                } else {
                    List<Result> resultList = (List<Result>) ProgressManager.getInstance().run(new Task.WithResult(project, CLOSING_TITLE, false) {
                        @Override
                        protected Object compute(@NotNull ProgressIndicator indicator) {
                            indicator.setText(CLOSING);
                            List<Result> results = closeMergeRequest(indicator);
                            indicator.setText(CLOSED);
                            return results;
                        }
                    });
                    dispose();
                    new ResultDialog(resultList, OperationTypeEnum.CLOSE_MERGE_REQUEST.getDialogTitle()).showAndGet();
                }
            }
        });

        mergeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() != 1) {
                    return;
                }
                if (backgroudCheckBox.isSelected()) {
                    ProgressManager.getInstance().run(new Task.Backgroundable(project, Bundle.message("mergeRequestDialogTitle"), false) {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            indicator.setText(MERGING);
                            mergeRequests(indicator);
                            indicator.setText(MERGED);
                        }
                    });
                    dispose();
                } else {
                    List<Result> resultList = (List<Result>) ProgressManager.getInstance().run(new Task.WithResult(project, Bundle.message("mergeRequestDialogTitle"), false) {
                        @Override
                        protected Object compute(@NotNull ProgressIndicator indicator) {
                            indicator.setText(MERGING);
                            List<Result> results = mergeRequests(indicator);
                            indicator.setText(MERGED);
                            return results;
                        }
                    });
                    dispose();
                    new ResultDialog(resultList, OperationTypeEnum.MERGE.getDialogTitle()).showAndGet();
                }
            }
        });
    }

    private List<Result> mergeRequests(ProgressIndicator indicator) {
        StringBuilder info = new StringBuilder();
        StringBuilder error = new StringBuilder();
        AtomicInteger index = new AtomicInteger(1);
        List<Result> results = selectedMergeRequests.stream()
                .filter(u -> StringUtils.equalsIgnoreCase(u.getMergeStatus(), MergeStatusEnum.CAN_BE_MERGED.getMergeStatus()))
                .map(o -> {
                    try {
                        indicator.setText2("(" + index.getAndIncrement() + "/" + selectedMergeRequests.size() + ") " + o.getProjectName());
                        GitlabRestApi api = selectedProjectDto.getGitLabSettingsState().api(o.getGitlabServer());
                        GitlabMergeRequest gitlabMergeRequest = api.acceptMergeRequest(o.getProjectId(), o.getIid(), null);
                        Result rs = new Result(gitlabMergeRequest)
                                .setType(OperationTypeEnum.MERGE)
                                .setProjectName(o.getProjectName());
                        info.append(rs.toString()).append("\n");
                        return rs;
                    } catch (Exception e) {
                        Result re = new Result(new GitlabMergeRequest());
                        re.setType(OperationTypeEnum.MERGE)
                                .setProjectName(o.getProjectName())
                                .setErrorMsg(e.getMessage());
                        error.append(re).append("\n");
                        return re;
                    }
                }).collect(Collectors.toList());
        Notifier.notify(project, info, error, null);
        return results;
    }

    private List<Result> closeMergeRequest(ProgressIndicator indicator) {
        StringBuilder info = new StringBuilder();
        StringBuilder error = new StringBuilder();
        AtomicInteger index = new AtomicInteger(1);
        List<Result> results = selectedMergeRequests.stream()
                .map(o -> {
                    try {
                        indicator.setText2("(" + index.getAndIncrement() + "/" + selectedMergeRequests.size() + ") " + o.getProjectName());
                        GitlabRestApi api = selectedProjectDto.getGitLabSettingsState().api(o.getGitlabServer());
                        GitlabMergeRequest gitlabMergeRequest = api.updateMergeRequest(o.getProjectId(), o.getIid(), o.getTargetBranch(),
                                api.getCurrentUser().getId(), null, null, OperationTypeEnum.CLOSE_MERGE_REQUEST.getType(), null);
                        Result rs = new Result(gitlabMergeRequest)
                                .setType(OperationTypeEnum.CLOSE_MERGE_REQUEST)
                                .setProjectName(o.getProjectName());
                        info.append(rs.toString()).append("\n");
                        return rs;
                    } catch (Exception e) {
                        Result re = new Result(new GitlabMergeRequest());
                        re.setType(OperationTypeEnum.CLOSE_MERGE_REQUEST)
                                .setProjectName(o.getProjectName())
                                .setErrorMsg(e.getMessage());
                        error.append(re).append("\n");
                        return re;
                    }
                }).collect(Collectors.toList());
        Notifier.notify(project, info, error, null);
        return results;
    }

    private Collection<Object> searchBranch(String text, Set<String> allSourcebranch) {
        return allSourcebranch.stream().filter(s -> s.toLowerCase().contains(text.toLowerCase())).collect(Collectors.toList());
    }

    private List<MergeRequest> searchMergeRequest() {
        String statusStr = status.getSelectedItem() != null ? status.getSelectedItem().toString() == "all" ? null : status.getSelectedItem().toString() : null;
        String sourceBranchStr = inputSourceBranch != null ? inputSourceBranch : sourceBranch.getSelectedItem() != null ? sourceBranch.getSelectedItem().toString() : null;
        String targetBranchStr = inputTargetBranch != null ? inputTargetBranch : targetBranch.getSelectedItem() != null ? targetBranch.getSelectedItem().toString() : null;
        return filterMergeRequest = gitlabMergeRequests.stream().filter(o ->
                (StringUtils.isEmpty(statusStr) || StringUtils.equalsIgnoreCase(o.getMergeStatus(), statusStr))
                        && (StringUtils.isEmpty(sourceBranchStr) || o.getSourceBranch().toLowerCase().contains(sourceBranchStr))
                        && (StringUtils.isEmpty(targetBranchStr) || o.getTargetBranch().toLowerCase().contains(targetBranchStr))
        ).collect(Collectors.toList());
    }

    private void initMergeRequestList(List<MergeRequest> filterMergeRequest) {
        selectAllCheckBox.setSelected(false);
        selectedMergeRequests.clear();
        setSelectedCount();
        mergeRequestList.setListData(filterMergeRequest.toArray());
        mergeRequestList.setCellRenderer(new LcheckBox());
        mergeRequestList.setEnabled(true);
        mergeRequestList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (super.isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                    selectedMergeRequests.remove(filterMergeRequest.get(index0));
                } else {
                    super.addSelectionInterval(index0, index1);
                    selectedMergeRequests.add(filterMergeRequest.get(index0));
                }
                setSelectedCount();
                updateBottomButtonState();
            }
        });
    }

    private void initCheckAll() {
        selectAllCheckBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() != 1) {
                    return;
                }
                JCheckBox checkAll = (JCheckBox) e.getSource();
                if (checkAll.isSelected()) {
                    selectedMergeRequests.addAll(filterMergeRequest);
                    mergeRequestList.setSelectionInterval(0, filterMergeRequest.size());
                } else {
                    filterMergeRequest.forEach(selectedMergeRequests::remove);
                    mergeRequestList.clearSelection();
                }
                setSelectedCount();
                updateBottomButtonState();
            }
        });
    }

    private void updateBottomButtonState() {
        if (selectedMergeRequests.size() == 0) {
            closeButton.setEnabled(false);
            mergeButton.setEnabled(false);
        } else {
            closeButton.setEnabled(true);
            if (selectedMergeRequests.stream().anyMatch(o -> StringUtils.equalsIgnoreCase(o.getMergeStatus(), "cannot_be_merged"))) {
                mergeButton.setEnabled(false);
            } else {
                mergeButton.setEnabled(true);
            }
        }
    }

    private void setSelectedCount() {
        selected.setText(String.format("(%s Selected)", selectedMergeRequests.size()));
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (CollectionUtil.isEmpty(selectedMergeRequests)) {
            return new ValidationInfo("Please select one branch at least", mergeRequestList);
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
