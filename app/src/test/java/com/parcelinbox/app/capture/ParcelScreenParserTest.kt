package com.parcelinbox.app.capture

import com.parcelinbox.app.data.CaptureMethod
import com.parcelinbox.app.data.ParcelStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParcelScreenParserTest {
    @Test
    fun parsesChineseOrderDetailsIntoStructuredFields() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.jingdong.app.mall",
            sourceLabel = "京东",
            visibleTexts = listOf(
                "订单详情",
                "商品：降噪蓝牙耳机",
                "订单号：123456789012",
                "已发货",
                "运单号：SF1234567890",
                "收货地址：北京市朝阳区"
            ),
            observedAt = 100L
        )!!

        assertEquals("降噪蓝牙耳机", result.title)
        assertEquals("123456789012", result.orderReference)
        assertEquals("SF1234567890", result.trackingNumber)
        assertEquals(ParcelStatus.SHIPPED, result.status)
        assertEquals(CaptureMethod.SCREEN, result.captureMethod)
        assertTrue(!result.title.contains("北京市"))
    }

    @Test
    fun parsesEnglishDeliveryScreen() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.amazon.mShop.android.shopping",
            sourceLabel = "Amazon",
            visibleTexts = listOf(
                "Order details",
                "Item: USB-C travel charger",
                "Order # 123-1234567-1234567",
                "Out for delivery",
                "Tracking number TBA123456789000"
            )
        )!!

        assertEquals("USB-C travel charger", result.title)
        assertEquals("123-1234567-1234567", result.orderReference)
        assertEquals("TBA123456789000", result.trackingNumber)
        assertEquals(ParcelStatus.OUT_FOR_DELIVERY, result.status)
    }

    @Test
    fun ignoresOrdinaryShoppingHomeScreen() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.taobao.taobao",
            sourceLabel = "淘宝",
            visibleTexts = listOf("首页", "猜你喜欢", "购物车", "限时优惠")
        )

        assertNull(result)
    }

    @Test
    fun ignoresGenericOrderPageWithoutAnyUsefulField() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.taobao.taobao",
            sourceLabel = "淘宝",
            visibleTexts = listOf("订单详情", "返回", "客服", "更多")
        )

        assertNull(result)
    }
}
