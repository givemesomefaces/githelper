package com.github.itisokey.githelper.window;

import com.github.lvlifeng.githelper.Bundle;
import com.google.common.collect.Lists;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Lv LiFeng
 * @date 2022/1/22 20:23
 */
public class Lpopup extends BaseListPopupStep {

    private String selectedBranchName;
    private Set<GitRepository> choosedRepositories;
    private GitBrancher gitBrancher;
    private boolean isRemote;


    public Lpopup(List values, GitBrancher gitBrancher, String selectedBranchName, Set<GitRepository> choosedRepositories, boolean isRemote) {
        super(null, values);
        this.selectedBranchName = selectedBranchName;
        this.choosedRepositories = choosedRepositories;
        this.isRemote = isRemote;
        this.gitBrancher = gitBrancher;
    }

    @Override
    public @Nullable PopupStep<?> onChosen(Object selectedValue, boolean finalChoice) {
        if (StringUtils.equalsIgnoreCase(selectedValue.toString(), Bundle.message("newBranchFromSelected"))) {
            doFinalStep(new Runnable() {
                @Override
                public void run() {
                    new CheckOutDialog("New Branch from " + selectedBranchName,
                            choosedRepositories,
                            gitBrancher,
                            selectedBranchName).showAndGet();
                }
            });
        }
        if (StringUtils.equalsIgnoreCase(Bundle.message("checkout"), selectedValue.toString())
                && StringUtils.isNotEmpty(selectedBranchName)) {
            if (isRemote) {
                final String startPointfinal = selectedBranchName;
                final String finalBranchName = selectedBranchName.split("/")[1];
                choosedRepositories.stream().forEach(o -> gitBrancher.checkoutNewBranchStartingFrom(finalBranchName, startPointfinal, Lists.newArrayList(o), null));
            } else {
                gitBrancher.checkout(selectedBranchName, false, new ArrayList<>(choosedRepositories), null);
            }
        }
        if (StringUtils.equalsIgnoreCase("Checkout New branch", selectedValue.toString())
                && StringUtils.isNotEmpty(selectedBranchName)) {
            //gitBrancher.checkoutNewBranch(jMenuItem.getText(), choosedRepositories);

        }
        if (StringUtils.equalsIgnoreCase(Bundle.message("delete"), selectedValue.toString())) {
            doFinalStep(new Runnable() {
                @Override
                public void run() {
                    if (popupConfirmDialog(isRemote, selectedBranchName)) {
                        if (isRemote) {
                            gitBrancher.deleteRemoteBranch(selectedBranchName, new ArrayList<>(choosedRepositories));
                        } else {
                            gitBrancher.deleteBranch(selectedBranchName, new ArrayList<>(choosedRepositories));
                        }
                    }
                }
            });
        }

        if (StringUtils.equalsIgnoreCase(Bundle.message("update"), selectedValue.toString())) {
            // gitBrancher
        }
        return super.onChosen(selectedValue, finalChoice);
    }

    private boolean popupConfirmDialog(boolean isRemote, String branchName) {
        int res = Messages.showYesNoDialog("Continue to delete" + (isRemote ? " remote " : " local ") + "branch: " + branchName,
                "Delete Branch", null);
        return res == Messages.YES;
    }
}
