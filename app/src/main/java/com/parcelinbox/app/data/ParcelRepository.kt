package com.parcelinbox.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ParcelRepository(private val database: ParcelDatabase) {
    private val _parcels = MutableStateFlow(database.getAll())
    val parcels: StateFlow<List<ParcelItem>> = _parcels.asStateFlow()

    private val _canUndoLastCapture = MutableStateFlow(false)
    val canUndoLastCapture: StateFlow<Boolean> = _canUndoLastCapture.asStateFlow()
    private var lastCaptureInsertedIds: List<Long> = emptyList()

    @Synchronized
    fun accept(parsed: ParsedParcel) {
        database.upsert(parsed)
        refresh()
    }

    @Synchronized
    fun acceptAll(parsed: List<ParsedParcel>): ParcelBatchResult {
        val result = database.upsertAll(parsed)
        lastCaptureInsertedIds = result.insertedIds
        _canUndoLastCapture.value = result.insertedIds.isNotEmpty()
        refresh()
        return result
    }

    @Synchronized
    fun markCompleted(id: Long) {
        database.markCompleted(id)
        refresh()
    }

    @Synchronized
    fun updateParcel(id: Long, title: String, status: ParcelStatus): Boolean {
        val updated = database.updateParcel(id, title, status)
        if (updated) refresh()
        return updated
    }

    @Synchronized
    fun deleteParcel(id: Long): Boolean {
        val deleted = database.deleteById(id)
        if (deleted) {
            lastCaptureInsertedIds = lastCaptureInsertedIds - id
            _canUndoLastCapture.value = lastCaptureInsertedIds.isNotEmpty()
            refresh()
        }
        return deleted
    }

    @Synchronized
    fun undoLastCapture(): Int {
        val ids = lastCaptureInsertedIds
        if (ids.isEmpty()) return 0
        val deleted = database.deleteByIds(ids)
        lastCaptureInsertedIds = emptyList()
        _canUndoLastCapture.value = false
        refresh()
        return deleted
    }

    @Synchronized
    fun deleteAll() {
        database.deleteAll()
        lastCaptureInsertedIds = emptyList()
        _canUndoLastCapture.value = false
        refresh()
    }

    @Synchronized
    fun refresh() {
        _parcels.value = database.getAll()
    }
}
