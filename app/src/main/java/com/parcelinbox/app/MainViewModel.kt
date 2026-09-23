package com.parcelinbox.app

import android.app.Application
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import com.parcelinbox.app.capture.ParcelAccessibilityService
import com.parcelinbox.app.data.CaptureMethod
import com.parcelinbox.app.data.ParcelItem
import com.parcelinbox.app.data.ParcelStatus
import com.parcelinbox.app.data.ParsedParcel
import com.parcelinbox.app.settings.ShoppingSource
import com.parcelinbox.app.settings.AppLanguage
import com.parcelinbox.app.settings.RetentionPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SourceChoice(
    val source: ShoppingSource,
    val enabled: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ParcelInboxApplication

    val parcels: StateFlow<List<ParcelItem>> = app.repository.parcels

    private val _hasNotificationAccess = MutableStateFlow(false)
    val hasNotificationAccess = _hasNotificationAccess.asStateFlow()

    private val _hasScreenCaptureAccess = MutableStateFlow(false)
    val hasScreenCaptureAccess = _hasScreenCaptureAccess.asStateFlow()

    private val _sources = MutableStateFlow(loadSources())
    val sources: StateFlow<List<SourceChoice>> = _sources.asStateFlow()

    private val _retention = MutableStateFlow(app.settings.retentionPolicy)
    val retention: StateFlow<RetentionPolicy> = _retention.asStateFlow()

    private val _language = MutableStateFlow(app.settings.appLanguage)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(app.settings.hasCompletedOnboarding)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _hasCompletedPermissionIntro = MutableStateFlow(app.settings.hasCompletedPermissionIntro)
    val hasCompletedPermissionIntro: StateFlow<Boolean> = _hasCompletedPermissionIntro.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _hasNotificationAccess.value = NotificationManagerCompat
            .getEnabledListenerPackages(app)
            .contains(app.packageName)
        _hasScreenCaptureAccess.value = isScreenCaptureServiceEnabled()
        app.repository.refresh()
        _sources.value = loadSources()
        _retention.value = app.settings.retentionPolicy
        _language.value = app.settings.appLanguage
        _hasCompletedOnboarding.value = app.settings.hasCompletedOnboarding
        _hasCompletedPermissionIntro.value = app.settings.hasCompletedPermissionIntro
    }

    fun setSourceEnabled(packageName: String, enabled: Boolean) {
        app.settings.setSourceEnabled(packageName, enabled)
        _sources.value = loadSources()
    }

    fun setRetention(policy: RetentionPolicy) {
        app.settings.retentionPolicy = policy
        _retention.value = policy
    }

    fun setLanguage(language: AppLanguage) {
        app.settings.appLanguage = language
        _language.value = language
    }

    fun markCompleted(id: Long) = app.repository.markCompleted(id)

    fun deleteAll() = app.repository.deleteAll()

    fun completeOnboarding() {
        app.settings.hasCompletedOnboarding = true
        _hasCompletedOnboarding.value = true
    }

    fun showOnboardingAgain() {
        app.settings.hasCompletedOnboarding = false
        _hasCompletedOnboarding.value = false
    }

    fun completePermissionIntro() {
        app.settings.hasCompletedPermissionIntro = true
        _hasCompletedPermissionIntro.value = true
    }

    fun acceptScreenCaptureDisclosure() {
        app.settings.hasAcceptedScreenCaptureDisclosure = true
    }

    fun addDemoData() {
        val now = System.currentTimeMillis()
        listOf(
            ParsedParcel(
                sourcePackage = "demo.cainiao",
                sourceLabel = "菜鸟",
                title = "日常用品包裹",
                trackingNumber = "DEMO10000001",
                pickupCode = "3-2-1056",
                status = ParcelStatus.READY_FOR_PICKUP,
                observedAt = now,
                captureMethod = CaptureMethod.DEMO
            ),
            ParsedParcel(
                sourcePackage = "demo.jd",
                sourceLabel = "京东",
                title = "数码配件",
                trackingNumber = "DEMO10000002",
                pickupCode = null,
                status = ParcelStatus.OUT_FOR_DELIVERY,
                observedAt = now - 45L * 60L * 1000L,
                captureMethod = CaptureMethod.DEMO
            ),
            ParsedParcel(
                sourcePackage = "demo.taobao",
                sourceLabel = "淘宝",
                title = "家居用品",
                trackingNumber = "DEMO10000003",
                pickupCode = null,
                status = ParcelStatus.IN_TRANSIT,
                observedAt = now - 3L * 60L * 60L * 1000L,
                captureMethod = CaptureMethod.DEMO
            )
        ).forEach(app.repository::accept)
    }

    private fun loadSources(): List<SourceChoice> = app.settings.availableSources.map {
        SourceChoice(it, app.settings.isSourceEnabled(it.packageName))
    }

    private fun isScreenCaptureServiceEnabled(): Boolean {
        val manager = app.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { service ->
                val info = service.resolveInfo.serviceInfo
                info.packageName == app.packageName && info.name == ParcelAccessibilityService::class.java.name
            }
    }
}
