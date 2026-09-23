package com.parcelinbox.app.capture

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.parcelinbox.app.ParcelInboxApplication
import java.util.ArrayDeque
import java.util.concurrent.Executors

/**
 * Reads visible text only while a user-selected shopping app is in the
 * foreground. It never performs clicks, gestures, scrolling, or text input.
 */
class ParcelAccessibilityService : AccessibilityService() {
    private val databaseExecutor = Executors.newSingleThreadExecutor()
    private var lastFingerprint: String? = null
    private var lastAcceptedAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val currentEvent = event ?: return
        val application = applicationContext as ParcelInboxApplication
        if (!application.settings.hasAcceptedScreenCaptureDisclosure) return

        val packageName = currentEvent.packageName?.toString().orEmpty()
        if (packageName.isBlank() || !application.settings.isSourceEnabled(packageName)) return
        if (currentEvent.eventType !in CAPTURE_EVENTS) return

        val now = System.currentTimeMillis()
        if (now - lastAcceptedAt < MIN_CAPTURE_INTERVAL_MS) return

        val root = rootInActiveWindow ?: currentEvent.source ?: return
        val visibleText = collectVisibleText(root)
        val parsed = ParcelScreenParser.parse(
            sourcePackage = packageName,
            sourceLabel = application.settings.sourceLabel(packageName) ?: packageName,
            visibleTexts = visibleText,
            observedAt = now
        ) ?: return

        val fingerprint = listOf(
            parsed.sourcePackage,
            parsed.orderReference,
            parsed.trackingNumber,
            parsed.title,
            parsed.status.name
        ).joinToString("|")
        if (fingerprint == lastFingerprint && now - lastAcceptedAt < DUPLICATE_WINDOW_MS) return

        lastFingerprint = fingerprint
        lastAcceptedAt = now
        databaseExecutor.execute { application.repository.accept(parsed) }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        databaseExecutor.shutdown()
        super.onDestroy()
    }

    private fun collectVisibleText(root: AccessibilityNodeInfo): List<String> {
        val output = ArrayList<String>(64)
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES && output.size < MAX_TEXT_NODES) {
            val node = queue.removeFirst()
            visited += 1

            if (node.isVisibleToUser) {
                node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(output::add)
                node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(output::add)
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::addLast)
            }
        }
        return output
    }

    private companion object {
        val CAPTURE_EVENTS = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED
        )
        const val MIN_CAPTURE_INTERVAL_MS = 900L
        const val DUPLICATE_WINDOW_MS = 15_000L
        const val MAX_NODES = 600
        const val MAX_TEXT_NODES = 240
    }
}
