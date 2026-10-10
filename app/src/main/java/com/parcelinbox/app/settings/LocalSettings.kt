package com.parcelinbox.app.settings

import android.content.Context
import androidx.core.content.edit

data class ShoppingSource(
    val packageName: String,
    val label: String,
    val tier: ShoppingSourceTier
)

enum class ShoppingSourceTier {
    CORE,
    EXPERIMENTAL
}

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
        ShoppingSource("com.taobao.taobao", "淘宝", ShoppingSourceTier.CORE),
        ShoppingSource("com.jingdong.app.mall", "京东", ShoppingSourceTier.CORE),
        ShoppingSource("com.xunmeng.pinduoduo", "拼多多", ShoppingSourceTier.CORE),
        ShoppingSource("com.tmall.wireless", "天猫", ShoppingSourceTier.CORE),
        ShoppingSource("com.cainiao.wireless", "菜鸟", ShoppingSourceTier.CORE),
        ShoppingSource("com.sf.activity", "顺丰", ShoppingSourceTier.CORE),
        ShoppingSource("com.amazon.mShop.android.shopping", "Amazon", ShoppingSourceTier.EXPERIMENTAL),
        ShoppingSource("com.einnovation.temu", "Temu", ShoppingSourceTier.EXPERIMENTAL),
        ShoppingSource("com.alibaba.aliexpresshd", "AliExpress", ShoppingSourceTier.EXPERIMENTAL),
        ShoppingSource("com.ebay.mobile", "eBay", ShoppingSourceTier.EXPERIMENTAL),
        ShoppingSource("com.walmart.android", "Walmart", ShoppingSourceTier.EXPERIMENTAL),
        ShoppingSource("com.zzkko", "SHEIN", ShoppingSourceTier.EXPERIMENTAL),
        ShoppingSource("com.etsy.android", "Etsy", ShoppingSourceTier.EXPERIMENTAL),
        ShoppingSource("com.alibaba.intl.android.apps.poseidon", "Alibaba.com", ShoppingSourceTier.EXPERIMENTAL),
        ShoppingSource("com.lazada.android", "Lazada", ShoppingSourceTier.EXPERIMENTAL),
        ShoppingSource("com.shopee.id", "Shopee Indonesia", ShoppingSourceTier.EXPERIMENTAL),
        ShoppingSource("com.flipkart.android", "Flipkart", ShoppingSourceTier.EXPERIMENTAL),
        ShoppingSource("com.mercadolibre", "Mercado Libre", ShoppingSourceTier.EXPERIMENTAL),
        ShoppingSource("jp.co.rakuten.android", "Rakuten", ShoppingSourceTier.EXPERIMENTAL)
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

    var capturePaused: Boolean
        get() = preferences.getBoolean(KEY_CAPTURE_PAUSED, false)
        set(value) {
            preferences.edit { putBoolean(KEY_CAPTURE_PAUSED, value) }
        }

    var hideItemNames: Boolean
        get() = preferences.getBoolean(KEY_HIDE_ITEM_NAMES, true)
        set(value) {
            preferences.edit { putBoolean(KEY_HIDE_ITEM_NAMES, value) }
        }

    var diagnosticsEnabled: Boolean
        get() = if (preferences.contains(KEY_DIAGNOSTICS_ENABLED)) {
            preferences.getBoolean(KEY_DIAGNOSTICS_ENABLED, true)
        } else {
            true
        }
        set(value) {
            preferences.edit { putBoolean(KEY_DIAGNOSTICS_ENABLED, value) }
        }

    private companion object {
        const val PREFERENCES_NAME = "local_settings"
        const val KEY_RETENTION = "retention_policy"
        const val KEY_LANGUAGE = "app_language"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_PERMISSION_INTRO_COMPLETE = "permission_intro_complete"
        // A new key is used only when the disclosed data scope materially changes.
        // Once accepted, normal launches and permission checks never ask again.
        const val KEY_SCREEN_CAPTURE_DISCLOSURE = "privacy_disclosure_v2"
        const val KEY_CAPTURE_PAUSED = "capture_paused"
        const val KEY_HIDE_ITEM_NAMES = "hide_item_names"
        const val KEY_DIAGNOSTICS_ENABLED = "diagnostics_enabled"
    }
}
