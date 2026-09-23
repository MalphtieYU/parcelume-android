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
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE parcels (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source_package TEXT NOT NULL,
                source_label TEXT NOT NULL,
                title TEXT NOT NULL,
                tracking_number TEXT,
                pickup_code TEXT,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                completed_at INTEGER,
                archived INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_parcels_tracking ON parcels(tracking_number)")
        db.execSQL("CREATE INDEX idx_parcels_updated ON parcels(updated_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    @Synchronized
    fun upsert(parsed: ParsedParcel): Long {
        val db = writableDatabase
        val existingId = findMatch(db, parsed)
        val completedAt = if (parsed.status == ParcelStatus.COMPLETED) parsed.observedAt else null
        val values = ContentValues().apply {
            put("source_package", parsed.sourcePackage)
            put("source_label", parsed.sourceLabel)
            put("title", parsed.title)
            parsed.trackingNumber?.let { put("tracking_number", it) }
            parsed.pickupCode?.let { put("pickup_code", it) }
            put("status", parsed.status.name)
            put("updated_at", parsed.observedAt)
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
                "tracking_number = ?",
                arrayOf(parsed.trackingNumber),
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
            "source_package = ? AND title = ? AND updated_at >= ? AND status != ?",
            arrayOf(parsed.sourcePackage, parsed.title, cutoff.toString(), ParcelStatus.COMPLETED.name),
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
                            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                            trackingNumber = cursor.getString(cursor.getColumnIndexOrThrow("tracking_number")),
                            pickupCode = cursor.getString(cursor.getColumnIndexOrThrow("pickup_code")),
                            status = ParcelStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
                            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
                            completedAt = cursor.getNullableLong("completed_at"),
                            archived = cursor.getInt(cursor.getColumnIndexOrThrow("archived")) == 1
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

    private fun android.database.Cursor.getNullableLong(column: String): Long? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getLong(index)
    }

    private companion object {
        const val DATABASE_NAME = "parcel_inbox.db"
        const val DATABASE_VERSION = 1
    }
}
