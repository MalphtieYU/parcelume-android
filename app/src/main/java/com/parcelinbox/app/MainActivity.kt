package com.parcelinbox.app

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.parcelinbox.app.notifications.ParcelNotificationListener
import com.parcelinbox.app.ui.ParcelInboxApp
import com.parcelinbox.app.ui.ParcelInboxTheme

class MainActivity : ComponentActivity() {
    private val screenModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ParcelInboxTheme {
                ParcelInboxApp(
                    viewModel = screenModel,
                    onOpenNotificationSettings = ::openSystemNotificationAccess,
                    onOpenScreenCaptureSettings = ::openSystemAccessibilitySettings
                )
            }
        }
    }

    /**
     * Notification-listener access can only be granted by the user in Android's
     * system UI. Newer phones open Parcelume's own detail page; older or heavily
     * customized systems fall back to the listener list, then general settings.
     */
    private fun openSystemNotificationAccess() {
        val listener = ComponentName(this, ParcelNotificationListener::class.java)
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                        .putExtra(
                            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                            listener.flattenToString()
                        )
                )
            }
            add(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            add(Intent(Settings.ACTION_SETTINGS))
        }

        for (intent in candidates) {
            if (intent.resolveActivity(packageManager) == null) continue
            if (runCatching { startActivity(intent) }.isSuccess) return
        }
    }

    /** Opens Android's own accessibility-services screen; Parcelume cannot grant this access. */
    private fun openSystemAccessibilitySettings() {
        val candidates = listOf(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in candidates) {
            if (intent.resolveActivity(packageManager) == null) continue
            if (runCatching { startActivity(intent) }.isSuccess) return
        }
    }

    override fun onResume() {
        super.onResume()
        screenModel.refresh()
    }
}
