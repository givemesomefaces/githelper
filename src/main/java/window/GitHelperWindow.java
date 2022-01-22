package window;

import cn.hutool.core.collection.CollectionUtil;
import com.github.lvlifeng.githelper.Bundle;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SingleSelectionModel;
import com.intellij.ui.awt.RelativePoint;
import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.GitUtil;
import git4idea.branch.GitBranchUtil;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import gitlab.helper.RepositoryHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
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
    private JLabel choosedSum;

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
            choosedSum.setVisible(false);
            return;
        }
        this.gitRepositories = repositories;
        this.gitBrancher = GitBrancher.getInstance(project);

        RepositoryHelper.sortRepositoriesByName(gitRepositories);

        initRepositoryList(null);

        initAllCheckBox();

        initSearchText();
    }

    private void initSearchText() {
        searchText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                searchRepository(e);
            }
        });

    }

    private void searchRepository(KeyEvent e) {
        String searchWord = ((JTextField) e.getSource()).getText();
        initRepositoryList(searchWord);
    }

    private void initAllCheckBox() {
        allCheckBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                if (StringUtils.equalsIgnoreCase(" ", Character.toString(e.getKeyChar()))) {
                    initAllCheckData((JCheckBox) e.getSource());
                }
            }
        });
        allCheckBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                initAllCheckData((JCheckBox) e.getSource());
            }
        });
    }

    private void initAllCheckData(JCheckBox checkBox) {
        System.out.println("allCheckBox="+ checkBox.isSelected());
        if (checkBox.isSelected()) {
            choosedRepositories.addAll(gitRepositories);
            repositoryList.addSelectionInterval(0, gitRepositories.size());
        } else {
            choosedRepositories.clear();
            repositoryList.clearSelection();
        }
        setChoosedSum();
        assembleCommonLocalBranchDataList();
        assembleCommonRemoteBranchDataList();
        System.out.println("choosedRepositories=" + choosedRepositories.size());
    }


    private void initRepositoryList(String searchWord) {

        if (CollectionUtil.isEmpty(gitRepositories)) {
            hideRepositoryRelMenu();
        } else {
            showRepositoryRelMenu(searchWord);
            List<GitRepository> filterRepositories = gitRepositories .stream()
                    .filter(o ->
                            (StringUtils.isNotEmpty(searchWord)
                                    && o.getRoot().getName().toLowerCase().contains(searchWord.toLowerCase()))
                                    || StringUtils.isEmpty(searchWord)
                    ).collect(Collectors.toList());
            repositoryList.setListData(filterRepositories.stream()
                    .map(GitRepository::getRoot)
                    .map(VirtualFile::getName)
                    .collect(Collectors.toList())
                    .toArray());

            repositoryList.setCellRenderer(new LcheckBox());
            repositoryList.setEnabled(true);
            repositoryList.setSelectionModel(new DefaultListSelectionModel() {
                @Override
                public void setSelectionInterval(int index0, int index1) {
                    if (super.isSelectedIndex(index0)) {
                        super.removeSelectionInterval(index0, index1);
                        choosedRepositories.remove(filterRepositories.get(index0));
                        allCheckBox.setSelected(false);
                    } else {
                        super.addSelectionInterval(index0, index1);
                        choosedRepositories.add(filterRepositories.get(index0));
                        checkAll(filterRepositories);
                    }
                    setChoosedSum();
                    assembleCommonLocalBranchDataList();
                    assembleCommonRemoteBranchDataList();
                    System.out.println("choosedRepositories=" + choosedRepositories.size());
                }
            });
            if (CollectionUtil.isNotEmpty(choosedRepositories)) {
                repositoryList.setSelectedIndices(choosedRepositories.stream()
                        .map(o -> filterRepositories.indexOf(o))
                        .mapToInt(Integer::valueOf)
                        .toArray());
                checkAll(filterRepositories);
            } else {
                repositoryList.clearSelection();
            }
        }
    }

    private void checkAll(List<GitRepository> filterRepositories) {
        if (choosedRepositories.size() == gitRepositories.size()
                && filterRepositories.size() == gitRepositories.size()) {
            allCheckBox.setSelected(true);
        }
    }

    private void hideRepositoryRelMenu() {
        repositoryDefaultText.setVisible(true);
        repositoryList.setVisible(false);
        allCheckBox.setVisible(false);
        choosedSum.setVisible(false);
    }

    private void showRepositoryRelMenu(String searchWord) {
        repositoryDefaultText.setVisible(false);
        repositoryList.setVisible(true);
        allCheckBox.setVisible(true);
        if (StringUtils.isEmpty(searchWord)) {
            allCheckBox.setEnabled(true);
        } else {
            allCheckBox.setSelected(false);
            allCheckBox.setEnabled(false);
        }
        choosedSum.setVisible(true);
    }

    private void setChoosedSum() {
        choosedSum.setText(String.format("(%s selected)", choosedRepositories.size()));
    }

    private void assembleCommonRemoteBranchDataList() {
        commonRemoteBranches = GitBranchUtil.getCommonRemoteBranches(choosedRepositories);
        if (CollectionUtil.isEmpty(commonRemoteBranches)) {
            remoteDefaultText.setVisible(true);
            commonRemoteBranchList.setVisible(false);
            commonRemoteBranchList.setListData(new Object[]{});
        } else {
            remoteDefaultText.setVisible(false);
            commonRemoteBranchList.setVisible(true);
            JPopupMenu jPopupMenu = getjPopupMenu(Bundle.message("remote"));
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
                    jPopupMenu.setToolTipText(Bundle.message("remote"));
                    jPopupMenu.show(commonRemoteBranchList, commonRemoteBranchList.getX() - jPopupMenu.getWidth(), (int) commonRemoteBranchList.getMousePosition().getY());
                }
            });
        }
    }

    @NotNull
    private JPopupMenu getjPopupMenu(String remote) {
        JPopupMenu jPopupMenu = new JPopupMenu();
        JMenuItem checkout = new JMenuItem(Bundle.message("checkout"));
        //JMenuItem update = new JMenuItem(Bundle.message("update"));
        JMenuItem checkoutNew = new JMenuItem(Bundle.message("newBranchFromSelected"));
        JMenuItem delete = new JMenuItem(Bundle.message("delete"));
        jPopupMenu.add(checkout);
//        if (StringUtils.equalsIgnoreCase(Bundle.message("local"), remote)) {
//            jPopupMenu.add(update);
//            addMouseListener(update);
//        }
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
            commonLocalBranchList.setListData(new Object[]{});
        } else {
            localDefaultText.setVisible(false);
            commonLocalBranchList.setVisible(true);
            commonLocalBranchList.setListData(commonLocalBranches.stream()
                    .map(GitLocalBranch::getName)
                    .collect(Collectors.toList())
                    .toArray());
            commonLocalBranchList.setCellRenderer(new LmenuItem());
            commonLocalBranchList.setSelectionModel(new SingleSelectionModel() {

                @Override
                public void setSelectionInterval(int index0, int index1) {
                    super.setSelectionInterval(index0, index1);
                    JBPopupFactory.getInstance()
                            .createListPopup(new Lpopup(Lists.newArrayList(Bundle.message("checkout"),
                                    Bundle.message("newBranchFromSelected"),
                                    Bundle.message("delete")),
                                    commonLocalBranches.get(index0).getName()));

//                    jPopupMenu.setName(commonLocalBranches.get(index0).getName());
//                    jPopupMenu.setToolTipText(Bundle.message("local"));
//                    jPopupMenu.show(commonLocalBranchList, commonLocalBranchList.getX() - jPopupMenu.getWidth(), (int) commonLocalBranchList.getMousePosition().getY());
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
                    executGit(j.getText(), popupMenu.getName(), StringUtils.equalsIgnoreCase(Bundle.message("remote"), popupMenu.getToolTipText()));
                }
            });
        });
    }

    private void popupDialog(JMenuItem j) {
        if (StringUtils.equalsIgnoreCase(j.getText(), Bundle.message("newBranchFromSelected"))) {
            String startPoint = j.getParent().getName();
            new CheckOutDialog("New Branch from " + startPoint,
                    choosedRepositories,
                    gitBrancher,
                    startPoint).showAndGet();
            assembleCommonLocalBranchDataList();
            assembleCommonRemoteBranchDataList();
        }
    }

    private void executGit(String operation, String branchName, Boolean isRemote) {
        if (StringUtils.equalsIgnoreCase(Bundle.message("checkout"), operation)
                && StringUtils.isNotEmpty(branchName)) {
            if (isRemote) {
                final String startPointfinal = branchName;
                final String finalBranchName = branchName.split("/")[1];
                choosedRepositories.stream().forEach(o -> gitBrancher.checkoutNewBranchStartingFrom(finalBranchName, startPointfinal, Lists.newArrayList(o), null));
            } else {
                gitBrancher.checkout(branchName, false, new ArrayList<>(choosedRepositories), null);
            }
        }
        if (StringUtils.equalsIgnoreCase("Checkout New branch", operation)
                && StringUtils.isNotEmpty(branchName)) {
            //gitBrancher.checkoutNewBranch(jMenuItem.getText(), choosedRepositories);

        }
        if (StringUtils.equalsIgnoreCase(Bundle.message("delete"), operation) && popupConfirmDialog(isRemote, branchName)) {
            if (isRemote){
                gitBrancher.deleteRemoteBranch(branchName, new ArrayList<>(choosedRepositories));
            } else {
                gitBrancher.deleteBranch(branchName, new ArrayList<>(choosedRepositories));
            }
        }

        if (StringUtils.equalsIgnoreCase(Bundle.message("update"), operation)) {
            // gitBrancher
        }


        assembleCommonLocalBranchDataList();
        assembleCommonRemoteBranchDataList();
    }

    private boolean popupConfirmDialog(boolean isRemote, String branchName) {
        int res = JOptionPane.showConfirmDialog(this.getGitHelperPanel().getRootPane(),
                "Continue to delete" + (isRemote ? " remote " : " local ") + "branch " + branchName, ":)", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
        return res==JOptionPane.YES_OPTION;
    }

    public JPanel getGitHelperPanel() {
        return gitHelperPanel;
    }
}
