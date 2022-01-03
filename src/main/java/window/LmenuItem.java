package window;

import javax.swing.*;
import java.awt.*;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/2 16:34
 */
public class LmenuItem extends JMenuItem implements ListCellRenderer{
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        this.setText(value.toString());
        this.setSelected(isSelected);
        this.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        this.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
        this.setEnabled(true);
        return this;
    }
}
