package com.parcelinbox.app.capture

import com.parcelinbox.app.data.CaptureMethod
import com.parcelinbox.app.data.ParsedParcel

data class ManualImportOutcome(
    val parcels: List<ParsedParcel>,
    val diagnosticCode: CaptureDiagnosticCode
)

/**
 * Parses text the user explicitly pasted or shared into Parcelume. This is a
 * permission-free fallback: the original text is kept only for this call and
 * only the same structured fields used by automatic capture are returned.
 */
object ManualImportParser {
    fun parse(
        rawText: String,
        genericSourceLabel: String,
        includeItemTitle: Boolean,
        observedAt: Long = System.currentTimeMillis()
    ): ManualImportOutcome {
        val lines = rawText.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .take(MAX_LINES)
            .map { it.take(MAX_LINE_LENGTH) }
            .toList()
        if (lines.isEmpty()) {
            return ManualImportOutcome(emptyList(), CaptureDiagnosticCode.NO_VISIBLE_TEXT)
        }

        val normalizedText = lines.joinToString("\n").take(MAX_IMPORT_LENGTH)
        val source = SOURCE_HINTS.firstOrNull { hint ->
            hint.markers.any { normalizedText.contains(it, ignoreCase = true) }
        }
        val outcome = ParcelScreenParser.parseSnapshot(
            sourcePackage = source?.packageName ?: MANUAL_SOURCE_PACKAGE,
            sourceLabel = source?.label ?: genericSourceLabel,
            snapshot = VisiblePageSnapshot(lines, emptyList(), lines.size),
            observedAt = observedAt,
            includeItemTitle = includeItemTitle
        )
        val englishUi = genericSourceLabel == "Manual import"
        return ManualImportOutcome(
            parcels = outcome.parcels.map {
                it.copy(
                    title = if (includeItemTitle) {
                        it.title
                    } else if (englishUi) {
                        "${it.sourceLabel} parcel"
                    } else {
                        "${it.sourceLabel} 包裹"
                    },
                    captureMethod = CaptureMethod.MANUAL
                )
            },
            diagnosticCode = outcome.diagnosticCode
        )
    }

    private data class SourceHint(
        val packageName: String,
        val label: String,
        val markers: List<String>
    )

    private val SOURCE_HINTS = listOf(
        SourceHint("com.taobao.taobao", "淘宝", listOf("淘宝", "taobao")),
        SourceHint("com.tmall.wireless", "天猫", listOf("天猫", "tmall")),
        SourceHint("com.jingdong.app.mall", "京东", listOf("京东", "jd.com", "jd物流")),
        SourceHint("com.xunmeng.pinduoduo", "拼多多", listOf("拼多多", "拼单编号")),
        SourceHint("com.cainiao.wireless", "菜鸟", listOf("菜鸟", "菜鸟驿站")),
        SourceHint("com.sf.activity", "顺丰", listOf("顺丰", "sf express")),
        SourceHint("com.amazon.mShop.android.shopping", "Amazon", listOf("amazon")),
        SourceHint("com.einnovation.temu", "Temu", listOf("temu")),
        SourceHint("com.alibaba.aliexpresshd", "AliExpress", listOf("aliexpress"))
    )

    private const val MANUAL_SOURCE_PACKAGE = "com.parcelume.manual"
    private const val MAX_LINES = 240
    private const val MAX_LINE_LENGTH = 320
    private const val MAX_IMPORT_LENGTH = 24_000
}
