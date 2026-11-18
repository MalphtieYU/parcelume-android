package com.parcelinbox.app.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceTestSupportTest {
    @Test
    fun readinessListsEveryMissingRequirementInOrder() {
        val readiness = DeviceTestReadiness(
            diagnosticsEnabled = false,
            sourceEnabled = false,
            consentAccepted = false,
            systemAccessEnabled = false,
            capturePaused = true
        )

        assertFalse(readiness.ready)
        assertEquals(DeviceTestBlocker.entries, readiness.blockers)
    }

    @Test
    fun readinessIsReadyOnlyWhenEverySafetyGateIsSatisfied() {
        val readiness = DeviceTestReadiness(
            diagnosticsEnabled = true,
            sourceEnabled = true,
            consentAccepted = true,
            systemAccessEnabled = true,
            capturePaused = false
        )

        assertTrue(readiness.ready)
        assertTrue(readiness.blockers.isEmpty())
    }

    @Test
    fun compatibilityReportContainsMetadataOnly() {
        val report = CompatibilityReportBuilder.build(
            history = listOf(
                CaptureDiagnostic(
                    observedAt = 1234L,
                    sourceLabel = "京东|injected",
                    code = CaptureDiagnosticCode.CAPTURED,
                    pageKind = "tracking",
                    visibleNodeCount = 20,
                    textNodeCount = 8,
                    candidateCount = 1,
                    deviceFamily = "Samsung",
                    androidVersion = "Android 17",
                    sourceVersion = "16.2.1",
                    parserVersion = "CN-test"
                )
            ),
            chinese = true,
            appVersion = "0.3.6",
            testContext = DeviceTestReportContext(
                sourceLabel = "京东",
                startedAt = 1000L,
                readiness = DeviceTestReadiness(true, true, true, true, false)
            )
        )

        assertTrue(report.contains("CAPTURED"))
        assertTrue(report.contains("真机测试：京东"))
        assertFalse(report.contains("|injected"))
        assertFalse(report.contains("JD202611180001"))
        assertFalse(report.contains("13800138000"))
    }
}
