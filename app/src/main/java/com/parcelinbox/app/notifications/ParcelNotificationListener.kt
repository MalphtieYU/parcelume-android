package com.parcelinbox.app.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.parcelinbox.app.ParcelInboxApplication
import java.util.concurrent.Executors

class ParcelNotificationListener : NotificationListenerService() {
    private val databaseExecutor = Executors.newSingleThreadExecutor()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val application = applicationContext as ParcelInboxApplication
        val packageName = sbn.packageName
        if (!application.settings.isSourceEnabled(packageName)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = sequenceOf(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        ).filterNotNull().joinToString("\n") { it.toString() }

        val parsed = ParcelNotificationParser.parse(
            sourcePackage = packageName,
            sourceLabel = application.settings.sourceLabel(packageName) ?: packageName,
            notificationTitle = title,
            notificationText = text,
            observedAt = sbn.postTime
        ) ?: return

        databaseExecutor.execute {
            application.repository.accept(parsed)
        }
    }

    override fun onDestroy() {
        databaseExecutor.shutdown()
        super.onDestroy()
    }
}
