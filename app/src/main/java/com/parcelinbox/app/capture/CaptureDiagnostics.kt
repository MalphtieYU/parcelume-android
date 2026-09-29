package com.parcelinbox.app.capture

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CaptureDiagnosticCode {
    CAPTURED,
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
    private val _latest = MutableStateFlow(load())
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
        preferences.edit {
            putLong(KEY_TIME, diagnostic.observedAt)
            putString(KEY_SOURCE, diagnostic.sourceLabel)
            putString(KEY_CODE, diagnostic.code.name)
            putString(KEY_PAGE_KIND, diagnostic.pageKind)
            putInt(KEY_VISIBLE_NODES, diagnostic.visibleNodeCount)
            putInt(KEY_TEXT_NODES, diagnostic.textNodeCount)
            putInt(KEY_CANDIDATES, diagnostic.candidateCount)
            putString(KEY_DEVICE_FAMILY, diagnostic.deviceFamily)
            putString(KEY_ANDROID_VERSION, diagnostic.androidVersion)
            putString(KEY_SOURCE_VERSION, diagnostic.sourceVersion)
            putString(KEY_PARSER_VERSION, diagnostic.parserVersion)
        }
        _latest.value = diagnostic
    }

    @Synchronized
    fun clear() {
        preferences.edit { clear() }
        _latest.value = null
    }

    private fun load(): CaptureDiagnostic? {
        if (!preferences.contains(KEY_TIME)) return null
        return CaptureDiagnostic(
            observedAt = preferences.getLong(KEY_TIME, 0L),
            sourceLabel = preferences.getString(KEY_SOURCE, "").orEmpty(),
            code = runCatching {
                CaptureDiagnosticCode.valueOf(preferences.getString(KEY_CODE, "").orEmpty())
            }.getOrDefault(CaptureDiagnosticCode.UNSUPPORTED_PAGE),
            pageKind = preferences.getString(KEY_PAGE_KIND, "unknown").orEmpty(),
            visibleNodeCount = preferences.getInt(KEY_VISIBLE_NODES, 0),
            textNodeCount = preferences.getInt(KEY_TEXT_NODES, 0),
            candidateCount = preferences.getInt(KEY_CANDIDATES, 0),
            deviceFamily = preferences.getString(KEY_DEVICE_FAMILY, "Unknown Android device").orEmpty(),
            androidVersion = preferences.getString(KEY_ANDROID_VERSION, "Unknown").orEmpty(),
            sourceVersion = preferences.getString(KEY_SOURCE_VERSION, "Unknown").orEmpty(),
            parserVersion = preferences.getString(KEY_PARSER_VERSION, ChinaPlatformRules.PARSER_VERSION).orEmpty()
        )
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
        const val KEY_TIME = "time"
        const val KEY_SOURCE = "source"
        const val KEY_CODE = "code"
        const val KEY_PAGE_KIND = "page_kind"
        const val KEY_VISIBLE_NODES = "visible_nodes"
        const val KEY_TEXT_NODES = "text_nodes"
        const val KEY_CANDIDATES = "candidates"
        const val KEY_DEVICE_FAMILY = "device_family"
        const val KEY_ANDROID_VERSION = "android_version"
        const val KEY_SOURCE_VERSION = "source_version"
        const val KEY_PARSER_VERSION = "parser_version"
    }
}
