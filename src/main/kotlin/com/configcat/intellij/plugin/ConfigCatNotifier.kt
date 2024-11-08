package com.configcat.intellij.plugin

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import javax.swing.PopupFactory


private const val CONFIGCAT_NOTIFICATION_GROUP = "ConfigCat Notification Group"
private const val CONFIGCAT_TITLE = "ConfigCat Feature Flags"

class ConfigCatNotifier {

    internal object Notify {

        fun error( content: String) {
          error(null, content)
        }

        fun error(project: Project?, content: String) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(CONFIGCAT_NOTIFICATION_GROUP)
                .createNotification(CONFIGCAT_TITLE, content, NotificationType.ERROR)
                .notify(project)
        }

        fun info( content: String) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(CONFIGCAT_NOTIFICATION_GROUP)
                .createNotification(CONFIGCAT_TITLE, content, NotificationType.INFORMATION)
                .notify(null)
        }
    }

}