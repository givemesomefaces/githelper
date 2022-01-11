package gitlab.ui;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/11 19:17
 */
public class TestDialog extends DialogWrapper {



    protected TestDialog() {
        super(true);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return null;
    }
}
