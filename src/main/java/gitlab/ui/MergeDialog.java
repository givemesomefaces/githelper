package gitlab.ui;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import gitlab.OperationTypeEnum;
import gitlab.api.GitlabRestApi;
import gitlab.bean.MergeRequest;
import gitlab.bean.Result;
import gitlab.bean.SelectedProjectDto;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabMergeRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import window.LcheckBox;

import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/12 01:04
 */
public class MergeDialog extends DialogWrapper {
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
    private Set<MergeRequest> selectedMergeRequests = new HashSet<>();
    private SelectedProjectDto selectedProjectDto;
    private List<MergeRequest> filterMergeRequest;
    private List<MergeRequest> gitlabMergeRequests;
    private Set<String> allSourcebranch;
    private Set<String> allTargebranch;
    private Set<String> allMergeStatus;
    private String inputSourceBranch = null;
    private String inputTargetBranch = null;

    public MergeDialog(SelectedProjectDto selectedProjectDto) {
        super(null, null, true, IdeModalityType.IDE, false);
        setTitle("Merge Requests");
        this.selectedProjectDto = selectedProjectDto;
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
                dispose();
            }
        });
        ProgressManager.getInstance().run(new Task.Modal(null, "Merge Requests", false) {

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Loading merge requests...");
                gitlabMergeRequests = selectedProjectDto.getSelectedProjectList()
                        .stream()
                        .map(o -> {
                            List<GitlabMergeRequest> openMergeRequest = selectedProjectDto.getGitLabSettingsState()
                                    .api(o.getGitlabServer())
                                    .getOpenMergeRequest(o.getId());
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
                if (CollectionUtil.isEmpty(gitlabMergeRequests)) {
                    mergeButton.setEnabled(false);
                    closeButton.setEnabled(false);
                    selectAllCheckBox.setEnabled(false);
                    return;
                }
                Collections.sort(gitlabMergeRequests, new Comparator<MergeRequest>() {
                    @Override
                    public int compare(MergeRequest o1, MergeRequest o2) {
                        return StringUtils.compareIgnoreCase(o1.getProjectName(), o2.getProjectName());
                    }
                });
                allSourcebranch = gitlabMergeRequests.stream().map(MergeRequest::getSourceBranch).collect(Collectors.toSet());
                allTargebranch = gitlabMergeRequests.stream().map(MergeRequest::getTargetBranch).collect(Collectors.toSet());
                allMergeStatus = gitlabMergeRequests.stream().map(MergeRequest::getMergeStatus).collect(Collectors.toSet());

                sourceBranch.setModel(new DefaultComboBoxModel(allSourcebranch.toArray()));
                sourceBranch.setSelectedIndex(-1);
                targetBranch.setModel(new DefaultComboBoxModel(allTargebranch.toArray()));
                targetBranch.setSelectedIndex(-1);
                status.setModel(new DefaultComboBoxModel(allMergeStatus.toArray()));
                status.insertItemAt("all", 0);
                status.setSelectedItem("all");
                indicator.setText("Merge requests loaded");
            }
        });
    }

    private void initButton() {
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
                sourceBranch.setModel(new DefaultComboBoxModel(searchBranch(text, allSourcebranch).toArray()));
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
                targetBranch.setModel(new DefaultComboBoxModel(searchBranch(text, allTargebranch).toArray()));
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
                List<Result> results = (List<Result>) ProgressManager.getInstance().run(new Task.WithResult<>(null, "Merge Requests", false) {
                    @Override
                    protected Object compute(@NotNull ProgressIndicator indicator) {
                        indicator.setText("Closing...");
                        List<Result> results = selectedMergeRequests.stream()
                                .map(o -> {
                                    GitlabRestApi api = selectedProjectDto.getGitLabSettingsState().api(o.getGitlabServer());
                                    GitlabMergeRequest gitlabMergeRequest = api.updateMergeRequest(o.getProjectId(), o.getIid(), o.getTargetBranch(),
                                            api.getCurrentUser().getId(), null, null, OperationTypeEnum.CLOSE_MERGE_REQUEST.getType(), null);
                                    return new Result(gitlabMergeRequest)
                                            .setType(OperationTypeEnum.CLOSE_MERGE_REQUEST)
                                            .setProjectName(o.getProjectName());
                                }).collect(Collectors.toList());
                        indicator.setText("Closed");
                        return results;
                    }
                });
                dispose();
                new ResultDialog(results, OperationTypeEnum.CLOSE_MERGE_REQUEST.getDialogTitle()).showAndGet();
            }
        });

        mergeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                List<Result> results = (List<Result>) ProgressManager.getInstance().run(new Task.WithResult(null, "Merge Requests", false) {
                    @Override
                    protected Object compute(@NotNull ProgressIndicator indicator) {
                        indicator.setText("Merging...");
                        List<Result> results = selectedMergeRequests.stream()
                                .filter(u -> StringUtils.equalsIgnoreCase(u.getState(), "cannot_be_merged"))
                                .map(o -> {
                                    GitlabRestApi api = selectedProjectDto.getGitLabSettingsState().api(o.getGitlabServer());
                                    GitlabMergeRequest gitlabMergeRequest = api.updateMergeRequest(o.getProjectId(), o.getIid(), o.getTargetBranch(),
                                            api.getCurrentUser().getId(), null, null, OperationTypeEnum.MERGE.getType(), null);
                                    return new Result(gitlabMergeRequest)
                                            .setType(OperationTypeEnum.MERGE)
                                            .setProjectName(o.getProjectName());
                                }).collect(Collectors.toList());
                        indicator.setText("Merged");
                        return results;
                    }
                });
                dispose();
                new ResultDialog(results, OperationTypeEnum.MERGE.getDialogTitle()).showAndGet();
            }
        });
    }

    private Collection<Object> searchBranch(String text, Set<String> allSourcebranch) {
        return allSourcebranch.stream().filter(s-> s.toLowerCase().contains(text.toLowerCase())).collect(Collectors.toList());
    }

    private List<MergeRequest> searchMergeRequest() {
        String statusStr = status.getSelectedItem() != null ? status.getSelectedItem().toString()  == "all" ? null : status.getSelectedItem().toString() : null;
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
                JCheckBox checkAll = (JCheckBox) e.getSource();
                if (checkAll.isSelected()) {
                    selectedMergeRequests.addAll(filterMergeRequest);
                    mergeRequestList.setSelectionInterval(0, filterMergeRequest.size());
                } else {
                    selectedMergeRequests.removeAll(filterMergeRequest);
                    mergeRequestList.clearSelection();
                }
                setSelectedCount();
                updateBottomButtonState();
            }
        });
    }

    private void updateBottomButtonState() {
        if (selectedMergeRequests.stream().anyMatch(o -> StringUtils.equalsIgnoreCase(o.getMergeStatus(), "cannot_be_merged"))) {
            mergeButton.setEnabled(false);
        } else {
            mergeButton.setEnabled(true);
        }
    }

    private void setSelectedCount() {
        selected.setText(String.format("(Selected %s)", selectedMergeRequests.size()));
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (CollectionUtil.isEmpty(selectedMergeRequests)) {
            return new ValidationInfo("Please select at least one branch", mergeRequestList);
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
