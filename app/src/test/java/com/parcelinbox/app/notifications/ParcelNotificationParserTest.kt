package com.parcelinbox.app.notifications

import com.parcelinbox.app.data.ParcelStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParcelNotificationParserTest {
    @Test
    fun `extracts tracking number from shipping notification`() {
        val parsed = ParcelNotificationParser.parse(
            sourcePackage = "com.jingdong.app.mall",
            sourceLabel = "京东",
            notificationTitle = "订单已发货",
            notificationText = "您的商品已发货，运单号：SF1234567890",
            observedAt = 100L
        )

        assertEquals(ParcelStatus.SHIPPED, parsed?.status)
        assertEquals("SF1234567890", parsed?.trackingNumber)
    }

    @Test
    fun `extracts pickup code and gives pickup priority`() {
        val parsed = ParcelNotificationParser.parse(
            sourcePackage = "com.cainiao.wireless",
            sourceLabel = "菜鸟",
            notificationTitle = "包裹已到站",
            notificationText = "请到菜鸟驿站取件，取件码 3-2-1056",
            observedAt = 100L
        )

        assertEquals(ParcelStatus.READY_FOR_PICKUP, parsed?.status)
        assertEquals("3-2-1056", parsed?.pickupCode)
    }

    @Test
    fun `ignores unrelated notifications`() {
        val parsed = ParcelNotificationParser.parse(
            sourcePackage = "com.taobao.taobao",
            sourceLabel = "淘宝",
            notificationTitle = "限时活动",
            notificationText = "今晚八点开始",
            observedAt = 100L
        )

        assertNull(parsed)
    }
}
