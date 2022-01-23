package window;

import com.github.lvlifeng.githelper.Bundle;
import com.google.common.collect.Lists;
import com.intellij.openapi.ui.messages.MessageDialog;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.components.JBOptionButton;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/22 20:23
 */
public class Lpopup extends BaseListPopupStep{

    private String selectedBranchName;
    private Set<GitRepository> choosedRepositories;
    private GitBrancher gitBrancher;
    private boolean isRemote;


    public Lpopup(List values, GitBrancher gitBrancher, String selectedBranchName, Set<GitRepository> choosedRepositories, boolean isRemote) {
        super(null, values);
        this.selectedBranchName = selectedBranchName;
        this.choosedRepositories = choosedRepositories;
        this.isRemote = isRemote;
    }

    @Override
    public @Nullable PopupStep<?> onChosen(Object selectedValue, boolean finalChoice) {
        if (StringUtils.equalsIgnoreCase(selectedValue.toString(), Bundle.message("newBranchFromSelected"))) {
            new CheckOutDialog("New Branch from " + selectedValue.toString(),
                    choosedRepositories,
                    gitBrancher,
                    selectedValue.toString()).showAndGet();
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
                && org.apache.commons.lang3.StringUtils.isNotEmpty(selectedBranchName)) {
            //gitBrancher.checkoutNewBranch(jMenuItem.getText(), choosedRepositories);

        }
        if (StringUtils.equalsIgnoreCase(Bundle.message("delete"), selectedValue.toString()) && popupConfirmDialog(isRemote, selectedBranchName)) {
            if (isRemote){
                gitBrancher.deleteRemoteBranch(selectedBranchName, new ArrayList<>(choosedRepositories));
            } else {
                gitBrancher.deleteBranch(selectedBranchName, new ArrayList<>(choosedRepositories));
            }
        }

        if (org.apache.commons.lang3.StringUtils.equalsIgnoreCase(Bundle.message("update"), selectedValue.toString())) {
            // gitBrancher
        }
        return super.onChosen(selectedValue, finalChoice);
    }

    private boolean popupConfirmDialog(boolean isRemote, String branchName) {
        int res = JOptionPane.showConfirmDialog(null,
                "Continue to delete" + (isRemote ? " remote " : " local ") + "branch " + branchName, ":)", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
        return res==JOptionPane.YES_OPTION;
    }
}
