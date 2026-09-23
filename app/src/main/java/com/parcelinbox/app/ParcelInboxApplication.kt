package com.parcelinbox.app

import android.app.Application
import com.parcelinbox.app.data.ParcelDatabase
import com.parcelinbox.app.data.ParcelRepository
import com.parcelinbox.app.settings.LocalSettings
import com.parcelinbox.app.work.CleanupScheduler

class ParcelInboxApplication : Application() {
    val settings by lazy { LocalSettings(this) }
    val database by lazy { ParcelDatabase(this) }
    val repository by lazy { ParcelRepository(database) }

    override fun onCreate() {
        super.onCreate()
        CleanupScheduler.schedule(this)
    }
}
