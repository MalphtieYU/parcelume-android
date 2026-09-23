package com.parcelinbox.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ParcelRepository(private val database: ParcelDatabase) {
    private val _parcels = MutableStateFlow(database.getAll())
    val parcels: StateFlow<List<ParcelItem>> = _parcels.asStateFlow()

    @Synchronized
    fun accept(parsed: ParsedParcel) {
        database.upsert(parsed)
        refresh()
    }

    @Synchronized
    fun markCompleted(id: Long) {
        database.markCompleted(id)
        refresh()
    }

    @Synchronized
    fun deleteAll() {
        database.deleteAll()
        refresh()
    }

    @Synchronized
    fun refresh() {
        _parcels.value = database.getAll()
    }
}
