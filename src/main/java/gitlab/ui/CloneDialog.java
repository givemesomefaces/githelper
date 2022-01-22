package gitlab.ui;

import com.github.lvlifeng.githelper.icons.Icons;
import com.intellij.dvcs.ui.CloneDvcsValidationUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.util.Consumer;
import git4idea.commands.Git;
import gitlab.common.GitCheckoutProvider;
import gitlab.bean.ProjectDto;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
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
    private JLabel directoryButton;
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
        selectedProjectList.stream().forEach(s ->
            GitCheckoutProvider.clone(project, Git.getInstance(), checkoutListener, finalDestinationParent,
                    s.getSshUrl(), s.getName(), directory.getText())
        );
    }

    private void initDefaultDirectory(){
        directoryButton.setIcon(Icons.DirectoryDir);
        directoryButton.setBorder(null);
        directory.setText(project.getBasePath());
        directoryButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                FileChooser.chooseFiles(new FileChooserDescriptor(false,
                        true,
                        false,
                        false,
                        false,
                        false),
                        project,
                        null,
                        new Consumer<List<VirtualFile>>() {
                            @Override
                            public void consume(List<VirtualFile> virtualFiles) {
                                directory.setText(virtualFiles.get(0).getPath());
                            }
                        });
            }
        });
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
