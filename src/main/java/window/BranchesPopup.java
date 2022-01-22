package window;

import com.github.lvlifeng.githelper.Bundle;
import com.google.common.collect.Lists;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BranchesPopup extends BaseListPopupStep {

    public BranchesPopup(List values) {
        super(null, values);
    }

    @Override
    public boolean hasSubstep(Object selectedValue) {
        return true;
    }

    @Override
    public @Nullable PopupStep<?> onChosen(Object selectedValue, boolean finalChoice) {

        return new Lpopup(Lists.newArrayList(Bundle.message("checkout"),
                Bundle.message("newBranchFromSelected"),
                Bundle.message("delete")), selectedValue.toString());
    }
}
