package gitlab;

import lombok.Getter;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/8 10:59
 */
@Getter
public class CloneDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel clonePane;
    private JTextField directory;
    private JButton HIHIHIButton;

    public CloneDialog() {
        initDefaultDirectory();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void initDefaultDirectory(){
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

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        CloneDialog dialog = new CloneDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
