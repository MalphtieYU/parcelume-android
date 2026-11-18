package com.parcelinbox.app.capture

data class DeviceTestReadiness(
    val diagnosticsEnabled: Boolean,
    val sourceEnabled: Boolean,
    val consentAccepted: Boolean,
    val systemAccessEnabled: Boolean,
    val capturePaused: Boolean
) {
    val blockers: List<DeviceTestBlocker>
        get() = buildList {
            if (!diagnosticsEnabled) add(DeviceTestBlocker.DIAGNOSTICS_DISABLED)
            if (!sourceEnabled) add(DeviceTestBlocker.SOURCE_DISABLED)
            if (!consentAccepted) add(DeviceTestBlocker.CONSENT_REQUIRED)
            if (!systemAccessEnabled) add(DeviceTestBlocker.SYSTEM_ACCESS_OFF)
            if (capturePaused) add(DeviceTestBlocker.CAPTURE_PAUSED)
        }

    val ready: Boolean
        get() = blockers.isEmpty()
}

enum class DeviceTestBlocker {
    DIAGNOSTICS_DISABLED,
    SOURCE_DISABLED,
    CONSENT_REQUIRED,
    SYSTEM_ACCESS_OFF,
    CAPTURE_PAUSED
}

data class DeviceTestReportContext(
    val sourceLabel: String,
    val startedAt: Long,
    val readiness: DeviceTestReadiness
)

/**
 * Builds a text-only report from metadata that CaptureDiagnostics has already
 * stripped down. No raw page or notification text can enter this API.
 */
object CompatibilityReportBuilder {
    fun build(
        history: List<CaptureDiagnostic>,
        chinese: Boolean,
        appVersion: String,
        testContext: DeviceTestReportContext? = null
    ): String {
        val latest = history.firstOrNull()
        val heading = if (chinese) {
            """Parcelume 脱敏兼容性报告
Parcelume 版本：${safe(appVersion)}
设备：${latest?.deviceFamily ?: "Unknown"}
系统：${latest?.androidVersion ?: "Unknown"}
记录数量：${history.size}
"""
        } else {
            """Parcelume privacy-safe compatibility report
Parcelume version: ${safe(appVersion)}
Device: ${latest?.deviceFamily ?: "Unknown"}
System: ${latest?.androidVersion ?: "Unknown"}
Record count: ${history.size}
"""
        }
        val session = testContext?.let { context ->
            val status = if (context.readiness.ready) "READY" else {
                context.readiness.blockers.joinToString(",") { it.name }
            }
            if (chinese) {
                "真机测试：${safe(context.sourceLabel)} | 开始时间：${context.startedAt} | 准备状态：$status\n"
            } else {
                "Device test: ${safe(context.sourceLabel)} | Started: ${context.startedAt} | Readiness: $status\n"
            }
        }.orEmpty()
        val records = history.joinToString("\n") { item ->
            listOf(
                item.observedAt,
                safe(item.sourceLabel),
                safe(item.sourceVersion),
                safe(item.parserVersion),
                item.code.name,
                safe(item.pageKind),
                item.visibleNodeCount.coerceAtLeast(0),
                item.textNodeCount.coerceAtLeast(0),
                item.candidateCount.coerceAtLeast(0)
            ).joinToString(" | ")
        }
        val columns = if (chinese) {
            "时间 | 平台 | 应用版本 | 解析器 | 结果 | 页面 | 可见节点 | 文字节点 | 候选数"
        } else {
            "Time | Source | App version | Parser | Result | Page | Visible nodes | Text nodes | Candidates"
        }
        val footer = if (chinese) {
            "本报告不包含页面原文、商品名称、订单号、运单号、取件码、地址、电话或账号信息。"
        } else {
            "This report contains no raw page text, item name, order or tracking number, pickup code, address, phone number, or account data."
        }
        return "$heading$session\n$columns\n$records\n\n$footer"
    }

    private fun safe(value: String): String = value
        .replace(Regex("[\\r\\n\\t|]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(80)
}
