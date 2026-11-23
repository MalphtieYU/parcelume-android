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

    @Test
    fun `rejects payment credential notifications`() {
        val parsed = ParcelNotificationParser.parse(
            sourcePackage = "com.taobao.taobao",
            sourceLabel = "淘宝",
            notificationTitle = "订单支付",
            notificationText = "验证码 123456，请勿告诉他人",
            observedAt = 100L
        )

        assertNull(parsed)
    }

    @Test
    fun `does not use address or phone as a title`() {
        val parsed = ParcelNotificationParser.parse(
            sourcePackage = "com.jingdong.app.mall",
            sourceLabel = "京东",
            notificationTitle = "收货地址：北京市朝阳区",
            notificationText = "包裹已发货，运单号：SF1234567890",
            observedAt = 100L
        )

        assertEquals("包裹已发货，运单号：SF1234567890", parsed?.title)
    }

    @Test
    fun `ignores food delivery notifications from a shopping platform`() {
        val parsed = ParcelNotificationParser.parse(
            sourcePackage = "com.jingdong.app.mall",
            sourceLabel = "京东",
            notificationTitle = "京东外卖",
            notificationText = "外卖订单 JD202609270001 骑手正在配送"
        )

        assertNull(parsed)
    }

    @Test
    fun `ignores refund and cancelled order notifications`() {
        val parsed = ParcelNotificationParser.parse(
            sourcePackage = "com.taobao.taobao",
            sourceLabel = "淘宝",
            notificationTitle = "退款成功",
            notificationText = "订单号：TB202609270001 已全额退款"
        )

        assertNull(parsed)
    }

    @Test
    fun `does not mistake a promotion code for a tracking number`() {
        val parsed = ParcelNotificationParser.parse(
            sourcePackage = "com.taobao.taobao",
            sourceLabel = "淘宝",
            notificationTitle = "包裹好物推荐",
            notificationText = "物流会员优惠码 AB1234567890，限时领取"
        )

        assertNull(parsed)
    }

    @Test
    fun `accepts an unlabelled tracking number beside delivery context`() {
        val parsed = ParcelNotificationParser.parse(
            sourcePackage = "com.taobao.taobao",
            sourceLabel = "淘宝",
            notificationTitle = "包裹运输中",
            notificationText = "物流更新\nSF1234567890"
        )

        assertEquals("SF1234567890", parsed?.trackingNumber)
        assertEquals(ParcelStatus.IN_TRANSIT, parsed?.status)
    }
}
