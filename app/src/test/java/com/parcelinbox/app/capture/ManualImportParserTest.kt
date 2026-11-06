package com.parcelinbox.app.capture

import com.parcelinbox.app.data.CaptureMethod
import com.parcelinbox.app.data.ParcelStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualImportParserTest {
    @Test
    fun importsExplicitlySharedJdTrackingText() {
        val outcome = ManualImportParser.parse(
            rawText = """
                京东订单跟踪
                商品：桌面收纳盒
                京东订单号：JD202611030001
                配送单号：JDVA202611030001
                当前状态：运输中
            """.trimIndent(),
            genericSourceLabel = "手动导入",
            includeItemTitle = true,
            observedAt = 100L
        )

        val parcel = outcome.parcels.single()
        assertEquals("京东", parcel.sourceLabel)
        assertEquals("JDVA202611030001", parcel.trackingNumber)
        assertEquals(ParcelStatus.IN_TRANSIT, parcel.status)
        assertEquals(CaptureMethod.MANUAL, parcel.captureMethod)
    }

    @Test
    fun manualImportStillBlocksPaymentAndCredentialText() {
        val outcome = ManualImportParser.parse(
            rawText = "订单号：JD202611030001\n收银台\n请输入支付密码",
            genericSourceLabel = "手动导入",
            includeItemTitle = true
        )

        assertTrue(outcome.parcels.isEmpty())
        assertEquals(CaptureDiagnosticCode.SENSITIVE_PAGE_BLOCKED, outcome.diagnosticCode)
    }

    @Test
    fun manualImportHonorsHiddenItemNames() {
        val outcome = ManualImportParser.parse(
            rawText = "Amazon Order details\nItem: Private gift\nTracking number TBA123456789000\nShipped",
            genericSourceLabel = "Manual import",
            includeItemTitle = false
        )

        assertEquals("Amazon parcel", outcome.parcels.single().title)
    }

    @Test
    fun rejectsUnrelatedPastedText() {
        val outcome = ManualImportParser.parse(
            rawText = "今天记得买牛奶和水果",
            genericSourceLabel = "手动导入",
            includeItemTitle = true
        )

        assertTrue(outcome.parcels.isEmpty())
        assertEquals(CaptureDiagnosticCode.UNSUPPORTED_PAGE, outcome.diagnosticCode)
    }

    @Test
    fun rejectsOversizedSharedTextInsteadOfParsingATruncatedPrefix() {
        val validPrefix = "京东订单号：JD202611030001\n配送单号：JDVA202611030001\n运输中\n"
        val outcome = ManualImportParser.parse(
            rawText = validPrefix + "x".repeat(24_001),
            genericSourceLabel = "手动导入",
            includeItemTitle = true
        )

        assertTrue(outcome.parcels.isEmpty())
        assertEquals(CaptureDiagnosticCode.LOW_CONFIDENCE, outcome.diagnosticCode)
    }
}
