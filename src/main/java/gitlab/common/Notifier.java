package gitlab.common;

import cn.hutool.core.util.ObjectUtil;
import com.github.lvlifeng.githelper.Bundle;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.sun.istack.Nullable;
import gitlab.enums.StatusEnum;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Lv LiFeng
 * @date 2022/1/18 23:04
 */
public class Notifier {

    public static void notifyError(@Nullable Project project, String content) {
        NotificationGroupManager.getInstance().getNotificationGroup(Bundle.message("notifierGroup"))
                .createNotification(content, NotificationType.ERROR)
                .notify(project);
    }

    public static void notifyInfo(@Nullable Project project, String content) {
        NotificationGroupManager.getInstance().getNotificationGroup(Bundle.message("notifierGroup"))
                .createNotification(content, NotificationType.INFORMATION)
                .notify(project);
    }

    public static void notifyWarn(@Nullable Project project, String content) {
        NotificationGroupManager.getInstance().getNotificationGroup(Bundle.message("notifierGroup"))
                .createNotification(content, NotificationType.WARNING)
                .notify(project);
    }

    public static void notify(@Nullable Project project, StringBuilder info, StringBuilder error, StringBuilder warn) {
        if (ObjectUtil.isNotNull(info) && StringUtils.isNotEmpty(info)) {
            info.insert(0, StatusEnum.SUCCESSED.getDesc() + "\n");
            notifyInfo(project, info.toString());
        }
        if (ObjectUtil.isNotNull(error) && StringUtils.isNotEmpty(error)) {
            error.insert(0, StatusEnum.FAILED.getDesc() + "\n");
            notifyError(project, error.toString());
        }
        if (ObjectUtil.isNotNull(warn) && StringUtils.isNotEmpty(warn)) {
            notifyWarn(project, warn.toString());
        }
    }
}
