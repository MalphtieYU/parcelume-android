package com.parcelinbox.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ParcelDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    private val crypto = LocalFieldCrypto()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE parcels (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source_package TEXT NOT NULL,
                source_label TEXT NOT NULL,
                title TEXT NOT NULL,
                order_reference TEXT,
                tracking_number TEXT,
                pickup_code TEXT,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                completed_at INTEGER,
                archived INTEGER NOT NULL DEFAULT 0,
                capture_method TEXT NOT NULL DEFAULT 'NOTIFICATION',
                title_fingerprint TEXT NOT NULL,
                order_fingerprint TEXT,
                tracking_fingerprint TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_parcels_tracking_fingerprint ON parcels(tracking_fingerprint)")
        db.execSQL("CREATE INDEX idx_parcels_order_fingerprint ON parcels(order_fingerprint)")
        db.execSQL("CREATE INDEX idx_parcels_title_fingerprint ON parcels(title_fingerprint)")
        db.execSQL("CREATE INDEX idx_parcels_updated ON parcels(updated_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE parcels ADD COLUMN order_reference TEXT")
            db.execSQL("ALTER TABLE parcels ADD COLUMN capture_method TEXT NOT NULL DEFAULT 'NOTIFICATION'")
            db.execSQL("CREATE INDEX idx_parcels_order_reference ON parcels(order_reference)")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE parcels ADD COLUMN title_fingerprint TEXT")
            db.execSQL("ALTER TABLE parcels ADD COLUMN order_fingerprint TEXT")
            db.execSQL("ALTER TABLE parcels ADD COLUMN tracking_fingerprint TEXT")
            migratePlaintextFields(db)
            db.execSQL("DROP INDEX IF EXISTS idx_parcels_tracking")
            db.execSQL("DROP INDEX IF EXISTS idx_parcels_order_reference")
            db.execSQL("CREATE INDEX idx_parcels_tracking_fingerprint ON parcels(tracking_fingerprint)")
            db.execSQL("CREATE INDEX idx_parcels_order_fingerprint ON parcels(order_fingerprint)")
            db.execSQL("CREATE INDEX idx_parcels_title_fingerprint ON parcels(title_fingerprint)")
        }
    }

    @Synchronized
    fun upsert(parsed: ParsedParcel): Long {
        val db = writableDatabase
        val existingId = findMatch(db, parsed)
        val completedAt = if (parsed.status == ParcelStatus.COMPLETED) parsed.observedAt else null
        val values = ContentValues().apply {
            put("source_package", parsed.sourcePackage)
            put("source_label", parsed.sourceLabel)
            put("title", crypto.encrypt(parsed.title))
            put("title_fingerprint", crypto.fingerprint(parsed.title))
            parsed.orderReference?.let {
                put("order_reference", crypto.encrypt(it))
                put("order_fingerprint", crypto.fingerprint(it))
            }
            parsed.trackingNumber?.let {
                put("tracking_number", crypto.encrypt(it))
                put("tracking_fingerprint", crypto.fingerprint(it))
            }
            parsed.pickupCode?.let { put("pickup_code", crypto.encrypt(it)) }
            put("status", parsed.status.name)
            put("updated_at", parsed.observedAt)
            put("capture_method", parsed.captureMethod.name)
            if (completedAt != null) put("completed_at", completedAt)
        }

        return if (existingId != null) {
            db.update("parcels", values, "id = ?", arrayOf(existingId.toString()))
            existingId
        } else {
            values.put("created_at", parsed.observedAt)
            db.insertOrThrow("parcels", null, values)
        }
    }

    private fun findMatch(db: SQLiteDatabase, parsed: ParsedParcel): Long? {
        if (!parsed.trackingNumber.isNullOrBlank()) {
            db.query(
                "parcels",
                arrayOf("id"),
                "tracking_fingerprint = ?",
                arrayOf(crypto.fingerprint(parsed.trackingNumber)),
                null,
                null,
                "updated_at DESC",
                "1"
            ).use { cursor ->
                if (cursor.moveToFirst()) return cursor.getLong(0)
            }
        }

        if (!parsed.orderReference.isNullOrBlank()) {
            db.query(
                "parcels",
                arrayOf("id"),
                "source_package = ? AND order_fingerprint = ?",
                arrayOf(parsed.sourcePackage, crypto.fingerprint(parsed.orderReference)),
                null,
                null,
                "updated_at DESC",
                "1"
            ).use { cursor ->
                if (cursor.moveToFirst()) return cursor.getLong(0)
            }
        }

        // 没有运单号时，仅合并同一来源最近 48 小时内仍未完成的相似记录。
        val cutoff = parsed.observedAt - 48L * 60L * 60L * 1000L
        db.query(
            "parcels",
            arrayOf("id"),
            "source_package = ? AND title_fingerprint = ? AND updated_at >= ? AND status != ?",
            arrayOf(parsed.sourcePackage, crypto.fingerprint(parsed.title), cutoff.toString(), ParcelStatus.COMPLETED.name),
            null,
            null,
            "updated_at DESC",
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    @Synchronized
    fun getAll(): List<ParcelItem> {
        return readableDatabase.query(
            "parcels",
            null,
            null,
            null,
            null,
            null,
            "updated_at DESC"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        ParcelItem(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                            sourcePackage = cursor.getString(cursor.getColumnIndexOrThrow("source_package")),
                            sourceLabel = cursor.getString(cursor.getColumnIndexOrThrow("source_label")),
                            title = crypto.decrypt(cursor.getString(cursor.getColumnIndexOrThrow("title")))
                                ?: "受保护的包裹",
                            orderReference = crypto.decrypt(cursor.getString(cursor.getColumnIndexOrThrow("order_reference"))),
                            trackingNumber = crypto.decrypt(cursor.getString(cursor.getColumnIndexOrThrow("tracking_number"))),
                            pickupCode = crypto.decrypt(cursor.getString(cursor.getColumnIndexOrThrow("pickup_code"))),
                            status = ParcelStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
                            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
                            completedAt = cursor.getNullableLong("completed_at"),
                            archived = cursor.getInt(cursor.getColumnIndexOrThrow("archived")) == 1,
                            captureMethod = runCatching {
                                CaptureMethod.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("capture_method")))
                            }.getOrDefault(CaptureMethod.NOTIFICATION)
                        )
                    )
                }
            }
        }
    }

    @Synchronized
    fun markCompleted(id: Long, now: Long = System.currentTimeMillis()) {
        val values = ContentValues().apply {
            put("status", ParcelStatus.COMPLETED.name)
            put("completed_at", now)
            put("updated_at", now)
        }
        writableDatabase.update("parcels", values, "id = ?", arrayOf(id.toString()))
    }

    @Synchronized
    fun deleteCompletedBefore(cutoff: Long): Int {
        return writableDatabase.delete(
            "parcels",
            "(status = ? OR archived = 1) AND COALESCE(completed_at, updated_at) < ?",
            arrayOf(ParcelStatus.COMPLETED.name, cutoff.toString())
        )
    }

    @Synchronized
    fun deleteAll(): Int = writableDatabase.delete("parcels", null, null)

    private fun migratePlaintextFields(db: SQLiteDatabase) {
        db.query(
            "parcels",
            arrayOf("id", "title", "order_reference", "tracking_number", "pickup_code"),
            null,
            null,
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                val order = cursor.getString(cursor.getColumnIndexOrThrow("order_reference"))
                val tracking = cursor.getString(cursor.getColumnIndexOrThrow("tracking_number"))
                val pickup = cursor.getString(cursor.getColumnIndexOrThrow("pickup_code"))
                val values = ContentValues().apply {
                    put("title", crypto.encrypt(title))
                    put("title_fingerprint", crypto.fingerprint(title))
                    order?.let {
                        put("order_reference", crypto.encrypt(it))
                        put("order_fingerprint", crypto.fingerprint(it))
                    }
                    tracking?.let {
                        put("tracking_number", crypto.encrypt(it))
                        put("tracking_fingerprint", crypto.fingerprint(it))
                    }
                    pickup?.let { put("pickup_code", crypto.encrypt(it)) }
                }
                db.update("parcels", values, "id = ?", arrayOf(id.toString()))
            }
        }
    }

    private fun android.database.Cursor.getNullableLong(column: String): Long? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getLong(index)
    }

    private companion object {
        const val DATABASE_NAME = "parcel_inbox.db"
        const val DATABASE_VERSION = 3
    }
}
