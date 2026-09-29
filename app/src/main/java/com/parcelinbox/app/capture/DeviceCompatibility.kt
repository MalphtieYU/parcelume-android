package com.parcelinbox.app.capture

import android.os.Build

enum class AndroidVendorFamily {
    XIAOMI_REDMI,
    HUAWEI,
    HONOR,
    SAMSUNG,
    OTHER
}

data class DeviceCompatibilityInfo(
    val vendorFamily: AndroidVendorFamily,
    val deviceLabel: String,
    val androidLabel: String
)

object DeviceCompatibility {
    fun current(): DeviceCompatibilityInfo {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val brand = Build.BRAND.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        val family = classify(manufacturer, brand, model)
        val device = listOf(manufacturer, model)
            .filter(String::isNotBlank)
            .distinctBy(String::lowercase)
            .joinToString(" ")
            .ifBlank { "Android device" }
            .take(80)
        return DeviceCompatibilityInfo(
            vendorFamily = family,
            deviceLabel = device,
            androidLabel = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        )
    }

    internal fun classify(manufacturer: String, brand: String, model: String): AndroidVendorFamily {
        val searchable = "$manufacturer $brand $model".lowercase()
        return when {
            listOf("xiaomi", "redmi", "poco").any(searchable::contains) -> AndroidVendorFamily.XIAOMI_REDMI
            searchable.contains("honor") -> AndroidVendorFamily.HONOR
            searchable.contains("huawei") -> AndroidVendorFamily.HUAWEI
            searchable.contains("samsung") -> AndroidVendorFamily.SAMSUNG
            else -> AndroidVendorFamily.OTHER
        }
    }
}
