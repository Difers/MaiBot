package io.github.dshgate

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** 只读通知，不做任何操作。屏幕黑着也能收到。 */
class NotifyService : NotificationListenerService() {
    override fun onListenerConnected() {
        Gate.init(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val s = sbn ?: return
        if (s.packageName !in Gate.packages) return
        val extras = s.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE).orEmpty()
        val long = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val short = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val text = (long ?: short).orEmpty()
        if (title.isBlank() && text.isBlank()) return
        Gate.record(
            applicationContext,
            Event(System.currentTimeMillis(), s.packageName, title, text, s.key ?: ""),
        )
    }
}
