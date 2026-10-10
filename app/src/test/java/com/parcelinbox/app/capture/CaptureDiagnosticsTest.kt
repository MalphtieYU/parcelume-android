package com.parcelinbox.app.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureDiagnosticsTest {
    @Test
    fun historyKeepsOnlyNewestTwentyRecords() {
        var history = emptyList<CaptureDiagnostic>()

        repeat(25) { index ->
            history = appendDiagnostic(
                history,
                diagnostic(
                    observedAt = index * 3_000L,
                    source = "source-$index",
                    code = CaptureDiagnosticCode.CAPTURED
                )
            )
        }

        assertEquals(MAX_DIAGNOSTIC_HISTORY, history.size)
        assertEquals("source-24", history.first().sourceLabel)
        assertEquals("source-5", history.last().sourceLabel)
    }

    @Test
    fun repeatedDelayedSamplesAreCoalesced() {
        val first = diagnostic(1_000L, "淘宝", CaptureDiagnosticCode.LOW_CONFIDENCE)
        val repeated = first.copy(observedAt = 2_500L, visibleNodeCount = 40)

        val history = appendDiagnostic(appendDiagnostic(emptyList(), first), repeated)

        assertEquals(1, history.size)
        assertEquals(2_500L, history.single().observedAt)
        assertEquals(40, history.single().visibleNodeCount)
    }

    @Test
    fun aChangedResultIsNotCoalesced() {
        val waiting = diagnostic(1_000L, "京东", CaptureDiagnosticCode.ROOT_UNAVAILABLE)
        val captured = waiting.copy(observedAt = 1_500L, code = CaptureDiagnosticCode.CAPTURED)

        val history = appendDiagnostic(appendDiagnostic(emptyList(), waiting), captured)

        assertEquals(2, history.size)
        assertTrue(history.first().code == CaptureDiagnosticCode.CAPTURED)
    }

    private fun diagnostic(
        observedAt: Long,
        source: String,
        code: CaptureDiagnosticCode
    ) = CaptureDiagnostic(
        observedAt = observedAt,
        sourceLabel = source,
        code = code,
        pageKind = "order_list",
        visibleNodeCount = 10,
        textNodeCount = 6,
        candidateCount = 1
    )
}
