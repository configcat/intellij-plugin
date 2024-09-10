package com.configcat.intellij.plugin

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType


private const val CONFIGCAT_NOTIFICATION_GROUP = "ConfigCat Notification Group"
private const val CONFIGCAT_TITLE = "ConfigCat Plugin"

class ConfigCatNotifier {

    internal object Notify {

        fun error( content: String) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(CONFIGCAT_NOTIFICATION_GROUP)
                .createNotification(CONFIGCAT_TITLE, content, NotificationType.ERROR)
                .notify(null)
        }

        fun info( content: String) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(CONFIGCAT_NOTIFICATION_GROUP)
                .createNotification(CONFIGCAT_TITLE, content, NotificationType.INFORMATION)
                .notify(null)
        }
    }

}