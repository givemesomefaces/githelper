package window;

import com.github.lvlifeng.githelper.Bundle;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.NlsContexts;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/22 20:23
 */
public class Lpopup extends BaseListPopupStep{

    private String selectedBranchName;
//    private JBPopupFactory jbPopupFactory = JBPopupFactory.getInstance();
//
//
//    private void test(){
//        jbPopupFactory.createListPopup(new BaseListPopupStep())
//    }


    public Lpopup(List values, String selectedBranchName) {
        super(null, values);
        this.selectedBranchName = selectedBranchName;
    }

    @Override
    public @Nullable PopupStep<?> onChosen(Object selectedValue, boolean finalChoice) {
        if (StringUtils.equalsIgnoreCase(selectedValue.toString(), Bundle.message("newBranchFromSelected"))) {

        }
        return super.onChosen(selectedValue, finalChoice);
    }


}
