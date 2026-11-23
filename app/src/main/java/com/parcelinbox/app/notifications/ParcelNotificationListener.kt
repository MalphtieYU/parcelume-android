package com.parcelinbox.app.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.parcelinbox.app.ParcelInboxApplication
import java.util.concurrent.Executors

class ParcelNotificationListener : NotificationListenerService() {
    private val databaseExecutor = Executors.newSingleThreadExecutor()
    @Volatile private var destroyed = false

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val application = applicationContext as ParcelInboxApplication
        if (!application.settings.hasAcceptedScreenCaptureDisclosure) return
        if (application.settings.capturePaused) return
        val packageName = sbn.packageName
        if (!application.settings.isSourceEnabled(packageName)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = sequenceOf(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        ).filterNotNull().joinToString("\n") { it.toString() }

        val parsed = runCatching {
            ParcelNotificationParser.parse(
                sourcePackage = packageName,
                sourceLabel = application.settings.sourceLabel(packageName) ?: packageName,
                notificationTitle = title,
                notificationText = text,
                observedAt = sbn.postTime
            )
        }.getOrNull() ?: return

        val privacySafe = if (application.settings.hideItemNames) {
            parsed.copy(title = "${parsed.sourceLabel} 包裹")
        } else {
            parsed
        }

        if (destroyed) return
        runCatching {
            databaseExecutor.execute {
                if (!destroyed) runCatching { application.repository.accept(privacySafe) }
            }
        }
    }

    override fun onDestroy() {
        destroyed = true
        databaseExecutor.shutdown()
        super.onDestroy()
    }
}
