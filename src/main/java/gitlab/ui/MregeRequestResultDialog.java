package gitlab.ui;

import com.intellij.openapi.ui.DialogWrapper;
import gitlab.bean.MergeRequestResult;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/11 23:24
 */
public class MregeRequestResultDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextArea result;
    private List<MergeRequestResult> mergeRequestResults;

    public MregeRequestResultDialog(List<MergeRequestResult> mergeRequestResults) {
        super(true);
        this.mergeRequestResults = mergeRequestResults;
        init();
        result.setBorder(null);
        result.setSelectedTextColor(Color.GREEN);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setText(mergeRequestResults.stream()
                .map(MergeRequestResult::toString)
                .reduce((a, b) -> a + "\n" + b)
                .get()
        );
        result.setSelectionStart(0);
        result.setSelectionEnd(result.getText().length());
        result.copy();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
