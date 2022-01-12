package gitlab.ui;

import com.github.lvlifeng.githelper.icons.GitHelperIcons;
import com.intellij.dvcs.ui.CloneDvcsValidationUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import gitlab.common.GitCheckoutProvider;
import gitlab.bean.ProjectDto;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.util.Set;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/8 10:59
 */
@Getter
public class CloneDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(CloneDialog.class);
    private JPanel contentPane;
    private JPanel clonePane;
    private JTextField directory;
    private JButton HIHIHIButton;
    private Set<ProjectDto> selectedProjectList;
    private CheckoutProvider.Listener checkoutListener;
    private Project project;

    protected CloneDialog(Project project, Set<ProjectDto> selectedProjectList) {
        super(true);
        init();
        setTitle("Clone Settings");
        this.selectedProjectList = selectedProjectList;
        this.project = project;
        initDefaultDirectory();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        ValidationInfo destinationValidation = CloneDvcsValidationUtils.createDestination(directory.getText());
        if (destinationValidation != null) {
            LOG.error("Unable to create destination directory", destinationValidation.message);
            return;
        }

        LocalFileSystem lfs = LocalFileSystem.getInstance();
        File file = new File(directory.getText());
        VirtualFile destinationParent = lfs.findFileByIoFile(file);
        if (destinationParent == null) {
            destinationParent = lfs.refreshAndFindFileByIoFile(file);
        }
        if (destinationParent == null) {
            LOG.error("Clone Failed. Destination doesn't exist");
            return;
        }
        dispose();

        VirtualFile finalDestinationParent = destinationParent;
        checkoutListener = ProjectLevelVcsManager.getInstance(project).getCompositeCheckoutListener();
        selectedProjectList.stream().forEach(s -> {
            GitCheckoutProvider.clone(project, Git.getInstance(), checkoutListener, finalDestinationParent,
                    s.getSshUrl(), s.getName(), directory.getText());
        });
    }

    private void initDefaultDirectory(){
        HIHIHIButton.setIcon(GitHelperIcons.CloneDir);
        directory.setText(System.getProperty("user.home") + File.separator + "IdeaProjects");
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        HIHIHIButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (chooser.showOpenDialog(null) != 1) {
                    directory.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
