package window;

import cn.hutool.core.collection.CollectionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SingleSelectionModel;
import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.GitUtil;
import git4idea.branch.GitBranchUtil;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/2 15:56
 */
public class GitHelperWindow {
    private JTextField searchText;
    private JList repositoryList;
    private JList commonLocalBranchList;
    private JList commonRemoteBranchList;
    private JPanel gitHelperPanel;
    private JCheckBox allCheckBox;
    private JLabel localDefaultText;
    private JLabel remoteDefaultText;
    private JLabel repositoryDefaultText;

    private List<GitRepository> gitRepositories;
    private List<GitLocalBranch> commonLocalBranches;
    private List<GitRemoteBranch> commonRemoteBranches;

    private Set<GitRepository> choosedRepositories = new HashSet<>();

    private GitBrancher gitBrancher;


    public GitHelperWindow(Project project) {

        List<GitRepository> repositories = GitUtil.getRepositories(project).stream().collect(Collectors.toList());
        System.out.println("repositories->size=" + repositories.size());
        if (CollectionUtil.isEmpty(repositories)) {
            allCheckBox.setVisible(false);
            return;
        }
        this.gitRepositories = repositories;
        this.gitBrancher = GitBrancher.getInstance(project);
        initRepositoryList();
        initAllCheckBox();
        initSearchText();
    }

    private void initSearchText() {
        searchText.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                try {
                    String text = e.getDocument().getText(e.getDocument().getStartPosition().getOffset(), e.getDocument().getLength());
                    if (StringUtils.isEmpty(text)) {
                        return;
                    }
                } catch (BadLocationException badLocationException) {
                    badLocationException.printStackTrace();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {

            }

            @Override
            public void changedUpdate(DocumentEvent e) {

            }
        });

    }

    private void initAllCheckBox() {
        allCheckBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JCheckBox checkBox = (JCheckBox) e.getSource();
                System.out.println("allCheckBox="+ checkBox.isSelected());
                if (checkBox.isSelected()) {
                    choosedRepositories.addAll(gitRepositories);
                    repositoryList.addSelectionInterval(0, gitRepositories.size());
                } else {
                    choosedRepositories.clear();
                    repositoryList.clearSelection();
                }
                assembleCommonLocalBranchDataList();
                assembleCommonRemoteBranchDataList();
                System.out.println("choosedRepositories=" + choosedRepositories.size());
            }
        });
    }


    private void initRepositoryList() {

        if (CollectionUtil.isEmpty(gitRepositories)) {
            repositoryDefaultText.setVisible(true);
            repositoryList.setVisible(false);
            allCheckBox.setVisible(false);
        } else {
            repositoryDefaultText.setVisible(false);
            repositoryList.setVisible(true);
            allCheckBox.setVisible(true);

            repositoryList.setListData(gitRepositories.stream()
                    .map(GitRepository::getRoot)
                    .map(VirtualFile::getName)
                    .collect(Collectors.toList()).toArray());
            repositoryList.setCellRenderer(new LcheckBox());
            repositoryList.setEnabled(true);
            repositoryList.setSelectionModel(new DefaultListSelectionModel() {
                @Override
                public void setSelectionInterval(int index0, int index1) {
                    if (super.isSelectedIndex(index0)) {
                        choosedRepositories.remove(gitRepositories.get(index0));
                        super.removeSelectionInterval(index0, index1);
                    } else {
                        choosedRepositories.add(gitRepositories.get(index0));
                        super.addSelectionInterval(index0, index1);
                    }
                    assembleCommonLocalBranchDataList();
                    assembleCommonRemoteBranchDataList();
                    System.out.println("choosedRepositories=" + choosedRepositories.size());
                }
            });
        }
    }

    private void assembleCommonRemoteBranchDataList() {
        commonRemoteBranches = GitBranchUtil.getCommonRemoteBranches(choosedRepositories);
        if (CollectionUtil.isEmpty(commonRemoteBranches)) {
            remoteDefaultText.setVisible(true);
            commonRemoteBranchList.setVisible(false);
        } else {
            remoteDefaultText.setVisible(false);
            commonRemoteBranchList.setVisible(true);
            JPopupMenu jPopupMenu = getjPopupMenu();
            commonRemoteBranchList.setListData(commonRemoteBranches.stream()
                    .map(GitRemoteBranch::getName)
                    .collect(Collectors.toList())
                    .toArray());
            commonRemoteBranchList.setCellRenderer(new LmenuItem());
            commonRemoteBranchList.setSelectionModel(new SingleSelectionModel() {

                @Override
                public void setSelectionInterval(int index0, int index1) {
                    super.setSelectionInterval(index0, index1);
                    jPopupMenu.setName(commonRemoteBranches.get(index0).getName());
                    jPopupMenu.setToolTipText("remote");
                    jPopupMenu.show(commonRemoteBranchList, commonRemoteBranchList.getX() - jPopupMenu.getWidth(), (int) commonRemoteBranchList.getMousePosition().getY());
                }
            });
        }
    }

    @NotNull
    private JPopupMenu getjPopupMenu() {
        JPopupMenu jPopupMenu = new JPopupMenu();
        JMenuItem checkout = new JMenuItem("Checkout");
        JMenuItem checkoutNew = new JMenuItem("New Branch from Selected");
        JMenuItem delete = new JMenuItem("Delete");
        jPopupMenu.add(checkout);
        jPopupMenu.add(checkoutNew);
        jPopupMenu.add(delete);
        addMouseListener(checkout, checkoutNew, delete);
        return jPopupMenu;
    }

    private void assembleCommonLocalBranchDataList() {

        commonLocalBranches = GitBranchUtil.getCommonLocalBranches(choosedRepositories);
        if (CollectionUtil.isEmpty(commonLocalBranches)) {
            localDefaultText.setVisible(true);
            commonLocalBranchList.setVisible(false);
        } else {
            localDefaultText.setVisible(false);
            commonLocalBranchList.setVisible(true);
            JPopupMenu jPopupMenu = getjPopupMenu();
            commonLocalBranchList.setListData(commonLocalBranches.stream()
                    .map(GitLocalBranch::getName)
                    .collect(Collectors.toList())
                    .toArray());
            commonLocalBranchList.setCellRenderer(new LmenuItem());
            commonLocalBranchList.setSelectionModel(new SingleSelectionModel() {

                @Override
                public void setSelectionInterval(int index0, int index1) {
                    super.setSelectionInterval(index0, index1);
                    jPopupMenu.setName(commonLocalBranches.get(index0).getName());
                    jPopupMenu.setToolTipText("local");
                    jPopupMenu.show(commonLocalBranchList, commonLocalBranchList.getX() - jPopupMenu.getWidth(), (int) commonLocalBranchList.getMousePosition().getY());
                }
            });
        }

    }

    private void addMouseListener(JMenuItem ...jMenuItem) {
        if (jMenuItem == null) {
            return;
        }
        Arrays.stream(jMenuItem).forEach(j -> {
            j.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    super.mouseClicked(e);
                    JMenuItem j = (JMenuItem) e.getSource();
                    JPopupMenu popupMenu = (JPopupMenu) j.getParent();
                    popupDialog(j);
                    executGit(j.getText(), popupMenu.getName(), null, true,
                            StringUtils.equalsIgnoreCase("remote", popupMenu.getToolTipText()));
                }
            });
        });
    }

    private void popupDialog(JMenuItem j) {
        if (StringUtils.equalsIgnoreCase(j.getText(), "New Branch from Selected")) {
            getNewBranchDialog(j.getParent().getName()).show();
        }
    }

    private JDialog getNewBranchDialog(String startPoint){
        JDialog jd = new JDialog();
        jd.setName(startPoint);
        jd.setTitle("New Branch from " + startPoint);
        jd.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        jd.setLocationByPlatform(true);
        jd.setSize(400, 200);
        jd.setLocationRelativeTo(null);

        JPanel branchName = new JPanel();
        branchName.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel jLabel = new JLabel("New branch name:");
        branchName.add(jLabel);

        JTextField newBranchName = new JTextField(20);
        newBranchName.setLayout(new FlowLayout(FlowLayout.LEFT));
        branchName.add(newBranchName);

        JPanel checkBoxes = new JPanel();
        JCheckBox checkOutcheckBox = new JCheckBox("Checkout", true);
        checkBoxes.setLayout(new FlowLayout(FlowLayout.LEFT));
        checkBoxes.add(checkOutcheckBox);

        JPanel button = new JPanel();
        button.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton cancle = new JButton("Cancel");
        cancle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                jd.dispose();
            }
        });
        cancle.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (StringUtils.equalsIgnoreCase(Character.toString(e.getKeyChar()), "\\u001b")) {
                    jd.dispose();
                }
            }
        });
        JButton create = new JButton("Create");
        button.add(cancle);
        button.add(create);
        create.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                JButton createBtn = (JButton) e.getSource();
                JDialog dialog = (JDialog) createBtn.getRootPane().getParent();
                Container contentPane = createBtn.getRootPane().getContentPane();
                JPanel branchNameJp = (JPanel) contentPane.getComponent(0);
                JTextField newBranchNameJt = (JTextField) branchNameJp.getComponent(1);
                String newBranchName = newBranchNameJt.getText();
                JPanel checkoutCheckBoxjp = (JPanel) contentPane.getComponent(1);
                JCheckBox checkoutCheckBox = (JCheckBox) checkoutCheckBoxjp.getComponent(0);
                boolean isCheckoutFlag = checkoutCheckBox.isSelected();
                executGit("New Branch from Selected", newBranchName, dialog.getName(), isCheckoutFlag, null);
                dialog.dispose();
            }
        });
        create.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (StringUtils.equalsIgnoreCase(Character.toString(e.getKeyChar()), "\\n")) {
                    // TODO
                }
            }
        });
        jd.add(branchName, BorderLayout.NORTH);
        jd.add(checkBoxes, BorderLayout.CENTER);
        jd.add(button, BorderLayout.SOUTH);
        jd.setVisible(true);
        return jd;
    }
    private void executGit(String operation, String branchName, String startPoint, boolean isCheckout, Boolean isRemote) {
        if (StringUtils.equalsIgnoreCase("Checkout", operation)
                && StringUtils.isNotEmpty(branchName)) {
            if (isRemote) {
                final String startPointfinal = branchName;
                branchName = branchName.split("/")[1];
                gitBrancher.checkoutNewBranchStartingFrom(branchName, startPointfinal, false, new ArrayList<>(choosedRepositories), null);
            } else {
                gitBrancher.checkout(branchName, false, new ArrayList<>(choosedRepositories), null);
            }
        }
        if (StringUtils.equalsIgnoreCase("Checkout New branch", operation)
                && StringUtils.isNotEmpty(branchName)) {
            //gitBrancher.checkoutNewBranch(jMenuItem.getText(), choosedRepositories);

        }
        if (StringUtils.equalsIgnoreCase("New Branch from Selected", operation)
                && StringUtils.isNotEmpty(branchName)) {
            //gitBrancher.checkoutNewBranch(jMenuItem.getText(), choosedRepositories);
            if (isCheckout) {
                gitBrancher.checkoutNewBranchStartingFrom(branchName, startPoint, false, new ArrayList<>(choosedRepositories), null);
            } else {
                gitBrancher.createBranch(branchName, choosedRepositories.stream().collect(Collectors.toMap(Function.identity(), o -> startPoint)));
            }
        }
        if (StringUtils.equalsIgnoreCase("Delete", operation) && popupConfirmDialog(isRemote, branchName)) {
            if (isRemote){
                gitBrancher.deleteRemoteBranch(branchName, new ArrayList<>(choosedRepositories));
            } else {
                gitBrancher.deleteBranch(branchName, new ArrayList<>(choosedRepositories));
            }
        }
    }

    private boolean popupConfirmDialog(boolean isRemote, String branchName) {
        int res = JOptionPane.showConfirmDialog(this.getGitHelperPanel().getRootPane(),
                "continue to delete" + (isRemote ? " remote " : " local ") + branchName, ":)", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
        return res==JOptionPane.YES_OPTION;
    }

    public JPanel getGitHelperPanel() {
        return gitHelperPanel;
    }
}
