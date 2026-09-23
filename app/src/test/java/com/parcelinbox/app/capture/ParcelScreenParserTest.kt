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

    @Test
    fun rejectsPaymentAndCredentialScreens() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.taobao.taobao",
            sourceLabel = "淘宝",
            visibleTexts = listOf(
                "订单号：123456789012",
                "收银台",
                "银行卡号：6222 0000 0000 0000",
                "请输入支付密码"
            )
        )

        assertNull(result)
    }

    @Test
    fun privacyModeDoesNotPersistItemTitle() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.amazon.mShop.android.shopping",
            sourceLabel = "Amazon",
            visibleTexts = listOf(
                "Order details",
                "Item: A very private purchase",
                "Order # 123-1234567-1234567",
                "Shipped"
            ),
            includeItemTitle = false
        )!!

        assertEquals("Amazon 包裹", result.title)
        assertTrue(!result.title.contains("private", ignoreCase = true))
    }

    @Test
    fun filtersContactAndPaymentRowsFromOtherwiseValidOrder() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.jingdong.app.mall",
            sourceLabel = "京东",
            visibleTexts = listOf(
                "订单详情",
                "商品：桌面收纳盒",
                "订单号：123456789012",
                "收货地址：上海市静安区",
                "支付方式：信用卡",
                "已发货"
            )
        )!!

        assertEquals("桌面收纳盒", result.title)
        assertEquals("123456789012", result.orderReference)
    }

    @Test
    fun parsesSpanishTrackingScreen() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.mercadolibre",
            sourceLabel = "Mercado Libre",
            visibleTexts = listOf(
                "Detalles del pedido",
                "Producto: Cargador portátil",
                "Número de pedido: ABC123456789",
                "Número de seguimiento: MELI123456789",
                "En camino"
            )
        )!!

        assertEquals("Cargador portátil", result.title)
        assertEquals("ABC123456789", result.orderReference)
        assertEquals("MELI123456789", result.trackingNumber)
        assertEquals(ParcelStatus.IN_TRANSIT, result.status)
    }

    @Test
    fun parsesIndonesianShopeeTrackingScreen() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.shopee.id",
            sourceLabel = "Shopee Indonesia",
            visibleTexts = listOf(
                "Detail pesanan",
                "Barang: Kabel pengisi daya",
                "Nomor pesanan: SHOPEE123456",
                "Nomor resi: JNT1234567890",
                "Dalam perjalanan"
            )
        )!!

        assertEquals("Kabel pengisi daya", result.title)
        assertEquals("JNT1234567890", result.trackingNumber)
        assertEquals(ParcelStatus.IN_TRANSIT, result.status)
    }

    @Test
    fun parsesJapaneseRakutenTrackingScreen() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "jp.co.rakuten.android",
            sourceLabel = "Rakuten",
            visibleTexts = listOf(
                "注文詳細",
                "商品名：USB充電器セット",
                "注文番号：RKT123456789",
                "追跡番号：JP1234567890",
                "発送済み"
            )
        )!!

        assertEquals("USB充電器セット", result.title)
        assertEquals("JP1234567890", result.trackingNumber)
        assertEquals(ParcelStatus.SHIPPED, result.status)
    }
}
