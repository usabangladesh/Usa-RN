package com.example.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.CopyOnWriteArrayList

data class NotificationItem(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postTime: Long
)

class RashedNotificationListenerService : NotificationListenerService() {

    companion object {
        @Volatile
        var instance: RashedNotificationListenerService? = null
            private set

        val recentNotifications = CopyOnWriteArrayList<NotificationItem>()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        refreshActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return
        recordNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    private fun recordNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val packageName = sbn.packageName ?: ""

        if (title.isBlank() && text.isBlank()) return

        val appName = try {
            val pm = packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            packageName
        }

        val item = NotificationItem(
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            postTime = sbn.postTime
        )

        recentNotifications.add(0, item)
        // Keep last 25
        while (recentNotifications.size > 25) {
            recentNotifications.removeAt(recentNotifications.size - 1)
        }
    }

    fun refreshActiveNotifications() {
        try {
            val active = activeNotifications ?: return
            recentNotifications.clear()
            for (sbn in active) {
                recordNotification(sbn)
            }
        } catch (_: Exception) {}
    }
}
