package com.parcelinbox.app.capture

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.parcelinbox.app.ParcelInboxApplication
import com.parcelinbox.app.data.ParsedParcel
import java.util.concurrent.Executors

/**
 * Reads visible text only while a user-selected shopping app is in the
 * foreground. It never performs clicks, gestures, scrolling, or text input.
 * Multiple delayed samples cover shopping pages that render asynchronously.
 */
class ParcelAccessibilityService : AccessibilityService() {
    private val databaseExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val lastScheduledAt = HashMap<String, Long>()
    private val recentFingerprints = LinkedHashMap<String, Long>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val currentEvent = event ?: return
        val application = applicationContext as ParcelInboxApplication
        if (!application.settings.hasAcceptedScreenCaptureDisclosure) return
        if (application.settings.capturePaused) return

        val packageName = currentEvent.packageName?.toString().orEmpty()
        if (packageName.isBlank() || !application.settings.isSourceEnabled(packageName)) return
        if (currentEvent.eventType !in CAPTURE_EVENTS) return

        val now = System.currentTimeMillis()
        val previousSchedule = lastScheduledAt[packageName] ?: 0L
        if (now - previousSchedule < SCHEDULE_DEBOUNCE_MS) return
        lastScheduledAt[packageName] = now

        SAMPLE_DELAYS_MS.forEach { delay ->
            mainHandler.postDelayed({ captureVisiblePage(packageName) }, delay)
        }
    }

    private fun captureVisiblePage(expectedPackage: String) {
        val application = applicationContext as ParcelInboxApplication
        if (!application.settings.hasAcceptedScreenCaptureDisclosure || application.settings.capturePaused) return
        if (!application.settings.isSourceEnabled(expectedPackage)) return

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != expectedPackage) return

        val observedAt = System.currentTimeMillis()
        val snapshot = collectVisibleSnapshot(root)
        val sourceLabel = application.settings.sourceLabel(expectedPackage) ?: expectedPackage
        val outcome = ParcelScreenParser.parseSnapshot(
            sourcePackage = expectedPackage,
            sourceLabel = sourceLabel,
            snapshot = snapshot,
            observedAt = observedAt,
            includeItemTitle = !application.settings.hideItemNames
        )

        if (application.settings.diagnosticsEnabled) {
            application.diagnostics.record(
                application.diagnostics.create(
                    observedAt = observedAt,
                    sourcePackage = expectedPackage,
                    sourceLabel = sourceLabel,
                    code = outcome.diagnosticCode,
                    pageKind = outcome.pageKind,
                    visibleNodeCount = snapshot.visibleNodeCount,
                    textNodeCount = outcome.textNodeCount,
                    candidateCount = outcome.parcels.size
                )
            )
        }

        val fresh = outcome.parcels.filter(::isFreshObservation)
        if (fresh.isEmpty()) return
        databaseExecutor.execute { application.repository.acceptAll(fresh) }
    }

    private fun isFreshObservation(parcel: ParsedParcel): Boolean {
        val now = System.currentTimeMillis()
        recentFingerprints.entries.removeAll { now - it.value > DUPLICATE_WINDOW_MS }
        val fingerprint = listOf(
            parcel.sourcePackage,
            parcel.orderReference,
            parcel.trackingNumber,
            parcel.pickupCode,
            parcel.status.name
        ).joinToString("|")
        val previous = recentFingerprints[fingerprint]
        if (previous != null && now - previous < DUPLICATE_WINDOW_MS) return false
        recentFingerprints[fingerprint] = now
        return true
    }

    private fun collectVisibleSnapshot(root: AccessibilityNodeInfo): VisiblePageSnapshot {
        var visited = 0
        val candidateBlocks = ArrayList<List<String>>(MAX_BLOCKS)

        fun visit(node: AccessibilityNodeInfo, depth: Int): List<String> {
            if (visited >= MAX_NODES || depth > MAX_DEPTH || !node.isVisibleToUser) return emptyList()
            visited += 1
            val lines = ArrayList<String>(16)
            if (!node.isPassword) {
                node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(lines::add)
                node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(lines::add)
            }

            for (index in 0 until node.childCount) {
                if (visited >= MAX_NODES || lines.size >= MAX_RETURNED_LINES) break
                node.getChild(index)?.let { child -> lines.addAll(visit(child, depth + 1)) }
            }

            // Repeated labels and identifiers are meaningful card boundaries.
            // De-duplicating a parent subtree can collapse two split shipments
            // into one flat block and let a later status leak into the first.
            val normalized = lines.asSequence().map(String::trim).filter(String::isNotBlank)
                .take(MAX_RETURNED_LINES).toList()
            if (
                depth > 0 && normalized.size in MIN_BLOCK_LINES..MAX_BLOCK_LINES &&
                candidateBlocks.size < MAX_BLOCKS && normalized.any(CANDIDATE_ANCHOR::containsMatchIn)
            ) {
                candidateBlocks += normalized
            }
            return normalized
        }

        val fullText = visit(root, 0)
        val blocks = candidateBlocks.asSequence()
            .distinctBy { it.joinToString("\u001f") }
            .sortedBy { it.size }
            .take(MAX_BLOCKS)
            .toList()
        return VisiblePageSnapshot(fullText, blocks, visited)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        databaseExecutor.shutdown()
        super.onDestroy()
    }

    private companion object {
        val CAPTURE_EVENTS = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED
        )
        val SAMPLE_DELAYS_MS = longArrayOf(180L, 750L, 1_600L)
        val CANDIDATE_ANCHOR = Regex(
            "(订单|运单|物流|快递|取件|包裹|待发货|已发货|运输|派送|签收|order|tracking|delivery)",
            RegexOption.IGNORE_CASE
        )
        const val SCHEDULE_DEBOUNCE_MS = 450L
        const val DUPLICATE_WINDOW_MS = 20_000L
        const val MAX_NODES = 900
        const val MAX_DEPTH = 28
        const val MAX_RETURNED_LINES = 280
        const val MIN_BLOCK_LINES = 2
        const val MAX_BLOCK_LINES = 100
        const val MAX_BLOCKS = 36
    }
}
