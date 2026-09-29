package com.parcelinbox.app.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceCompatibilityTest {
    @Test
    fun classifiesRedmiAndXiaomiDevices() {
        assertEquals(
            AndroidVendorFamily.XIAOMI_REDMI,
            DeviceCompatibility.classify("Xiaomi", "Redmi", "23013RK75C")
        )
    }

    @Test
    fun classifiesHuaweiHonorAndSamsungSeparately() {
        assertEquals(
            AndroidVendorFamily.HUAWEI,
            DeviceCompatibility.classify("HUAWEI", "HUAWEI", "LIO-AN00")
        )
        assertEquals(
            AndroidVendorFamily.HONOR,
            DeviceCompatibility.classify("HONOR", "HONOR", "Honor 300")
        )
        assertEquals(
            AndroidVendorFamily.HONOR,
            DeviceCompatibility.classify("HUAWEI", "HONOR", "LGE-AN00")
        )
        assertEquals(
            AndroidVendorFamily.SAMSUNG,
            DeviceCompatibility.classify("samsung", "samsung", "SM-S9180")
        )
    }
}
