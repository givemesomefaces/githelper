package gitlab.ui;

import cn.hutool.core.bean.BeanUtil;
import com.intellij.openapi.ui.DialogWrapper;
import gitlab.api.GitlabRestApi;
import gitlab.bean.MergeRequest;
import gitlab.bean.SelectedProjectDto;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabMergeRequest;
import org.jetbrains.annotations.Nullable;
import window.LcheckBox;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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
    private Set<MergeRequest> selectedMergeRequests;
    private SelectedProjectDto selectedProjectDto;

    public MergeDialog(SelectedProjectDto selectedProjectDto) {
        super(null, null, true, IdeModalityType.IDE, false);
        this.selectedProjectDto = selectedProjectDto;
        this.createSouthPanel().setVisible(false);
        init();
        getRootPane().setDefaultButton(cancelButton);
        List<MergeRequest> gitlabMergeRequests = selectedProjectDto.getSelectedProjectList()
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
        Set<String> allSourcebranch = gitlabMergeRequests.stream().map(MergeRequest::getSourceBranch).collect(Collectors.toSet());
        Set<String> allTargebranch = gitlabMergeRequests.stream().map(MergeRequest::getTargetBranch).collect(Collectors.toSet());
        Set<String> allMergeStatus = gitlabMergeRequests.stream().map(MergeRequest::getMergeStatus).collect(Collectors.toSet());

        sourceBranch.setModel(new DefaultComboBoxModel(allSourcebranch.toArray()));
        sourceBranch.setSelectedIndex(-1);
        targetBranch.setModel(new DefaultComboBoxModel(allTargebranch.toArray()));
        targetBranch.setSelectedIndex(-1);
        status.setModel(new DefaultComboBoxModel(allMergeStatus.toArray()));
        status.addItem("all");
        status.setSelectedItem("all");
        String statusStr = status.getSelectedItem() != null ? status.getSelectedItem().toString()  == "all" ? null : status.getSelectedItem().toString() : null;
        String sourceBranchStr = sourceBranch.getSelectedItem() != null ? sourceBranch.getSelectedItem().toString() : null;
        String targetBranchStr = targetBranch.getSelectedItem() != null ? targetBranch.getSelectedItem().toString() : null;
        List<MergeRequest> filterMergeRequest = gitlabMergeRequests.stream().filter(o ->
                (StringUtils.isEmpty(statusStr) || StringUtils.equalsIgnoreCase(o.getState(), statusStr))
                        && (StringUtils.isEmpty(sourceBranchStr) || StringUtils.equalsIgnoreCase(o.getSourceBranch(), sourceBranchStr))
                        && (StringUtils.isEmpty(targetBranchStr) || StringUtils.equalsIgnoreCase(o.getTargetBranch(), targetBranchStr))
        ).collect(Collectors.toList());


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
            }
        });

        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                List<GitlabMergeRequest> closeReusltList = selectedMergeRequests.stream()
                        .map(o -> {
                            GitlabRestApi api = selectedProjectDto.getGitLabSettingsState().api(o.getGitlabServer());
                            return api.updateMergeRequest(o.getProjectId(), o.getId(), o.getTargetBranch(),
                                    api.getCurrentUser().getId(), null, null, "close", null);
                        }
                ).collect(Collectors.toList());
            }
        });

        mergeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                List<GitlabMergeRequest> mergeReusltList = selectedMergeRequests.stream()
                        .filter(u -> StringUtils.equalsIgnoreCase(u.getState(), "cannot_be_merged"))
                        .map(o -> {
                            GitlabRestApi api = selectedProjectDto.getGitLabSettingsState().api(o.getGitlabServer());
                            return api.updateMergeRequest(o.getProjectId(), o.getId(), o.getTargetBranch(),
                                    api.getCurrentUser().getId(), null, null, "merge", null);
                        }
                ).collect(Collectors.toList());
            }
        });
        cancelButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                dispose();
            }
        });
    }


    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
