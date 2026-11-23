package com.parcelinbox.app

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parcelinbox.app.data.ParcelDatabase
import com.parcelinbox.app.data.CaptureMethod
import com.parcelinbox.app.data.ParsedParcel
import com.parcelinbox.app.data.ParcelStatus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParcelDatabaseRecoveryTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun resetDatabase() {
        ParcelDatabase(context).use { it.deleteAll() }
    }

    @Test
    fun unknownStoredStatusDoesNotCrashStartup() {
        ParcelDatabase(context).use { database ->
            database.writableDatabase.insertOrThrow(
                "parcels",
                null,
                ContentValues().apply {
                    put("source_package", "test.source")
                    put("source_label", "测试平台")
                    put("title", "受保护的测试包裹")
                    put("status", "FUTURE_UNKNOWN_STATUS")
                    put("created_at", 100L)
                    put("updated_at", 100L)
                    put("capture_method", "SCREEN")
                    put("title_fingerprint", "test-fingerprint")
                }
            )

            val item = database.getAll().single()
            assertEquals(ParcelStatus.EXCEPTION, item.status)
        }
    }

    @Test
    fun staleNotificationCannotMoveAParcelBackwards() {
        ParcelDatabase(context).use { database ->
            database.upsert(
                ParsedParcel(
                    sourcePackage = "test.source",
                    sourceLabel = "测试平台",
                    title = "测试包裹",
                    trackingNumber = "SF1234567890",
                    pickupCode = null,
                    status = ParcelStatus.DELIVERED,
                    observedAt = 200L,
                    captureMethod = CaptureMethod.SCREEN
                )
            )
            database.upsert(
                ParsedParcel(
                    sourcePackage = "test.source",
                    sourceLabel = "测试平台",
                    title = "测试包裹",
                    trackingNumber = "SF1234567890",
                    pickupCode = null,
                    status = ParcelStatus.EXCEPTION,
                    observedAt = 100L,
                    captureMethod = CaptureMethod.NOTIFICATION
                )
            )

            val item = database.getAll().single()
            assertEquals(ParcelStatus.DELIVERED, item.status)
            assertEquals(200L, item.updatedAt)
            assertEquals(CaptureMethod.SCREEN, item.captureMethod)
        }
    }
}
