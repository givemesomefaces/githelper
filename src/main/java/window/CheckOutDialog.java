package window;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/22 18:54
 */
public class CheckOutDialog extends DialogWrapper {
    private JPanel contentPane;
    private JCheckBox checkoutCheckBox;
    private JTextField newBranchName;
    Set<GitRepository> choosedRepositories;
    private GitBrancher gitBrancher;
    private String  startPoint;

    public CheckOutDialog(String title, Set<GitRepository> choosedRepositories, GitBrancher gitBrancher, String statPoint) {
        super(true);
        this.choosedRepositories = choosedRepositories;
        this.gitBrancher = gitBrancher;
        this.startPoint = statPoint;
        setTitle(title);
        init();
        setModal(true);

    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        if (checkoutCheckBox.isSelected()) {
            gitBrancher.checkoutNewBranchStartingFrom(newBranchName.getText(), startPoint, false, new ArrayList<>(choosedRepositories), null);
        } else {
            gitBrancher.createBranch(newBranchName.getText(), choosedRepositories.stream().collect(Collectors.toMap(Function.identity(), o -> startPoint)));
        }
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (StringUtils.isNotBlank(newBranchName.getText())) {
            return new ValidationInfo("New branch name cannot be empty!", newBranchName);
        }
        if (StringUtils.isNotBlank(startPoint)) {
            return new ValidationInfo("Startpoint cannot be empty!", contentPane);
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
