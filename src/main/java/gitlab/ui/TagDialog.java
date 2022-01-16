package gitlab.ui;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/16 18:12
 */
public class TagDialog extends DialogWrapper {
    private JPanel contentPane;
    private JButton buttonOK;
    private JTextField tagName;
    private JComboBox createFrom;
    private JTextField message;

    public TagDialog() {
        super(true);
        init();
        getRootPane().setDefaultButton(buttonOK);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
