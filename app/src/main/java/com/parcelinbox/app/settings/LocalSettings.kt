package com.parcelinbox.app.settings

import android.content.Context
import androidx.core.content.edit

data class ShoppingSource(
    val packageName: String,
    val label: String
)

enum class RetentionPolicy(val label: String, val days: Int?) {
    ONE_WEEK("一周", 7),
    ONE_MONTH("一个月", 30),
    ONE_YEAR("一年", 365),
    FOREVER("永久保留", null)
}

enum class AppLanguage {
    CHINESE,
    ENGLISH
}

class LocalSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val availableSources = listOf(
        ShoppingSource("com.taobao.taobao", "淘宝"),
        ShoppingSource("com.jingdong.app.mall", "京东"),
        ShoppingSource("com.xunmeng.pinduoduo", "拼多多"),
        ShoppingSource("com.cainiao.wireless", "菜鸟"),
        ShoppingSource("com.sf.activity", "顺丰"),
        ShoppingSource("com.amazon.mShop.android.shopping", "Amazon")
    )

    fun isSourceEnabled(packageName: String): Boolean =
        preferences.getBoolean("source.$packageName", false)

    fun setSourceEnabled(packageName: String, enabled: Boolean) {
        preferences.edit { putBoolean("source.$packageName", enabled) }
    }

    fun sourceLabel(packageName: String): String? =
        availableSources.firstOrNull { it.packageName == packageName }?.label

    var retentionPolicy: RetentionPolicy
        get() {
            val stored = preferences.getString(KEY_RETENTION, RetentionPolicy.FOREVER.name)
            return runCatching { RetentionPolicy.valueOf(stored.orEmpty()) }
                .getOrDefault(RetentionPolicy.FOREVER)
        }
        set(value) {
            preferences.edit { putString(KEY_RETENTION, value.name) }
        }

    var appLanguage: AppLanguage
        get() {
            val stored = preferences.getString(KEY_LANGUAGE, AppLanguage.CHINESE.name)
            return runCatching { AppLanguage.valueOf(stored.orEmpty()) }
                .getOrDefault(AppLanguage.CHINESE)
        }
        set(value) {
            preferences.edit { putString(KEY_LANGUAGE, value.name) }
        }

    var hasCompletedOnboarding: Boolean
        get() = preferences.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) {
            preferences.edit { putBoolean(KEY_ONBOARDING_COMPLETE, value) }
        }

    var hasCompletedPermissionIntro: Boolean
        get() = preferences.getBoolean(KEY_PERMISSION_INTRO_COMPLETE, false)
        set(value) {
            preferences.edit { putBoolean(KEY_PERMISSION_INTRO_COMPLETE, value) }
        }

    var hasAcceptedScreenCaptureDisclosure: Boolean
        get() = preferences.getBoolean(KEY_SCREEN_CAPTURE_DISCLOSURE, false)
        set(value) {
            preferences.edit { putBoolean(KEY_SCREEN_CAPTURE_DISCLOSURE, value) }
        }

    private companion object {
        const val PREFERENCES_NAME = "local_settings"
        const val KEY_RETENTION = "retention_policy"
        const val KEY_LANGUAGE = "app_language"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_PERMISSION_INTRO_COMPLETE = "permission_intro_complete"
        const val KEY_SCREEN_CAPTURE_DISCLOSURE = "screen_capture_disclosure_v1"
    }
}
