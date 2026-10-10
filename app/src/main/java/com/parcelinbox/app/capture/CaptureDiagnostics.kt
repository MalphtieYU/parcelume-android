package com.parcelinbox.app.capture

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class CaptureDiagnosticCode {
    CAPTURED,
    DUPLICATE_IGNORED,
    ROOT_UNAVAILABLE,
    STORAGE_ERROR,
    NO_VISIBLE_TEXT,
    UNSUPPORTED_PAGE,
    LOW_CONFIDENCE,
    SENSITIVE_PAGE_BLOCKED,
    FOOD_OR_INSTANT_DELIVERY_BLOCKED,
    AFTER_SALES_PAGE_BLOCKED
}

data class CaptureDiagnostic(
    val observedAt: Long,
    val sourceLabel: String,
    val code: CaptureDiagnosticCode,
    val pageKind: String,
    val visibleNodeCount: Int,
    val textNodeCount: Int,
    val candidateCount: Int,
    val deviceFamily: String = "Unknown Android device",
    val androidVersion: String = "Unknown",
    val sourceVersion: String = "Unknown",
    val parserVersion: String = ChinaPlatformRules.PARSER_VERSION
)

/**
 * Stores only operational metadata. It never stores page text, identifiers,
 * item titles, notification bodies, addresses, phone numbers, or screenshots.
 */
class CaptureDiagnostics(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _history = MutableStateFlow(loadHistory())
    val history: StateFlow<List<CaptureDiagnostic>> = _history.asStateFlow()

    private val _latest = MutableStateFlow(_history.value.firstOrNull())
    val latest: StateFlow<CaptureDiagnostic?> = _latest.asStateFlow()

    fun create(
        observedAt: Long,
        sourcePackage: String,
        sourceLabel: String,
        code: CaptureDiagnosticCode,
        pageKind: String,
        visibleNodeCount: Int,
        textNodeCount: Int,
        candidateCount: Int
    ): CaptureDiagnostic {
        val device = DeviceCompatibility.current()
        return CaptureDiagnostic(
            observedAt = observedAt,
            sourceLabel = sourceLabel,
            code = code,
            pageKind = pageKind,
            visibleNodeCount = visibleNodeCount,
            textNodeCount = textNodeCount,
            candidateCount = candidateCount,
            deviceFamily = device.deviceLabel,
            androidVersion = device.androidLabel,
            sourceVersion = packageVersion(sourcePackage),
            parserVersion = ChinaPlatformRules.PARSER_VERSION
        )
    }

    @Synchronized
    fun record(diagnostic: CaptureDiagnostic) {
        val updated = appendDiagnostic(_history.value, diagnostic)
        preferences.edit { putString(KEY_HISTORY, encode(updated)) }
        _history.value = updated
        _latest.value = updated.firstOrNull()
    }

    @Synchronized
    fun clear() {
        preferences.edit { clear() }
        _history.value = emptyList()
        _latest.value = null
    }

    private fun loadHistory(): List<CaptureDiagnostic> = runCatching {
        val payload = preferences.getString(KEY_HISTORY, null) ?: return@runCatching emptyList()
        val array = JSONArray(payload)
        buildList {
            for (index in 0 until minOf(array.length(), MAX_HISTORY)) {
                val item = array.getJSONObject(index)
                add(
                    CaptureDiagnostic(
                        observedAt = item.optLong("observedAt"),
                        sourceLabel = safeMetadata(item.optString("sourceLabel")),
                        code = runCatching {
                            CaptureDiagnosticCode.valueOf(item.optString("code"))
                        }.getOrDefault(CaptureDiagnosticCode.UNSUPPORTED_PAGE),
                        pageKind = safeMetadata(item.optString("pageKind", "unknown")),
                        visibleNodeCount = item.optInt("visibleNodeCount").coerceAtLeast(0),
                        textNodeCount = item.optInt("textNodeCount").coerceAtLeast(0),
                        candidateCount = item.optInt("candidateCount").coerceAtLeast(0),
                        deviceFamily = safeMetadata(item.optString("deviceFamily", "Unknown Android device")),
                        androidVersion = safeMetadata(item.optString("androidVersion", "Unknown")),
                        sourceVersion = safeMetadata(item.optString("sourceVersion", "Unknown")),
                        parserVersion = safeMetadata(
                            item.optString("parserVersion", ChinaPlatformRules.PARSER_VERSION)
                        )
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun encode(history: List<CaptureDiagnostic>): String {
        val array = JSONArray()
        history.forEach { diagnostic ->
            array.put(
                JSONObject()
                    .put("observedAt", diagnostic.observedAt)
                    .put("sourceLabel", diagnostic.sourceLabel)
                    .put("code", diagnostic.code.name)
                    .put("pageKind", diagnostic.pageKind)
                    .put("visibleNodeCount", diagnostic.visibleNodeCount)
                    .put("textNodeCount", diagnostic.textNodeCount)
                    .put("candidateCount", diagnostic.candidateCount)
                    .put("deviceFamily", diagnostic.deviceFamily)
                    .put("androidVersion", diagnostic.androidVersion)
                    .put("sourceVersion", diagnostic.sourceVersion)
                    .put("parserVersion", diagnostic.parserVersion)
            )
        }
        return array.toString()
    }

    @Suppress("DEPRECATION")
    private fun packageVersion(packageName: String): String = runCatching {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            appContext.packageManager.getPackageInfo(packageName, 0)
        }
        safeMetadata(info.versionName.orEmpty()).ifBlank { "Unknown" }
    }.getOrDefault("Unknown")

    private fun safeMetadata(value: String): String = value
        .replace(Regex("[\\r\\n\\t]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(80)

    private companion object {
        const val PREFERENCES_NAME = "capture_diagnostics"
        const val KEY_HISTORY = "privacy_safe_history_v2"
    }
}

internal const val MAX_DIAGNOSTIC_HISTORY = 20
private const val DIAGNOSTIC_COALESCE_MS = 2_000L
private const val MAX_HISTORY = MAX_DIAGNOSTIC_HISTORY

/** Keeps a small metadata-only ring buffer and collapses repeated delayed samples. */
internal fun appendDiagnostic(
    history: List<CaptureDiagnostic>,
    incoming: CaptureDiagnostic
): List<CaptureDiagnostic> {
    val current = history.firstOrNull()
    val repeatsCurrent = current != null &&
        incoming.observedAt - current.observedAt in 0..DIAGNOSTIC_COALESCE_MS &&
        current.sourceLabel == incoming.sourceLabel &&
        current.code == incoming.code &&
        current.pageKind == incoming.pageKind
    val tail = if (repeatsCurrent) history.drop(1) else history
    return (listOf(incoming) + tail).take(MAX_DIAGNOSTIC_HISTORY)
}
