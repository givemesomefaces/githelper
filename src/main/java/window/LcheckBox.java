package window;

import com.github.lvlifeng.githelper.bean.GitlabServer;
import org.apache.commons.lang3.BooleanUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * @author Lv LiFeng
 * @date 2022/1/3 17:03
 */
public class LcheckBox extends JCheckBox implements ListCellRenderer {

    public LcheckBox() {
        super();
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus) {
        this.setText(value.toString());
        this.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        this.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
        this.setSelected(isSelected);
        this.setEnabled(true);
        if (value instanceof GitlabServer) {
            this.setEnabled(BooleanUtils.isTrue(((GitlabServer) value).getValidFlag())
                    || Objects.isNull(((GitlabServer) value).getValidFlag()));
        }
        return this;
    }
}
