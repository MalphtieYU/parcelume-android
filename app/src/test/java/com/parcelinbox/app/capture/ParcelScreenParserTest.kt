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

    @Test
    fun parsesMultipleChineseOrderCardsWithoutMixingThem() {
        val first = listOf(
            "订单详情", "商品：蓝牙耳机", "订单号：TB1234567890",
            "运输中", "运单号：YT1234567890"
        )
        val second = listOf(
            "订单详情", "商品：桌面收纳盒", "订单号：TB0987654321",
            "派送中", "运单号：SF0987654321"
        )

        val outcome = ParcelScreenParser.parseSnapshot(
            sourcePackage = "com.taobao.taobao",
            sourceLabel = "淘宝",
            snapshot = VisiblePageSnapshot(first + second, listOf(first, second), 30)
        )

        assertEquals(2, outcome.parcels.size)
        assertTrue(outcome.parcels.any {
            it.orderReference == "TB1234567890" &&
                it.trackingNumber == "YT1234567890" && it.status == ParcelStatus.IN_TRANSIT
        })
        assertTrue(outcome.parcels.any {
            it.orderReference == "TB0987654321" &&
                it.trackingNumber == "SF0987654321" && it.status == ParcelStatus.OUT_FOR_DELIVERY
        })
    }

    @Test
    fun keepsTwoShipmentsFromTheSameOrderSeparate() {
        val first = listOf("订单号：JD1234567890", "商品：键盘", "已发货", "运单号：JDVA1234567890")
        val second = listOf("订单号：JD1234567890", "商品：鼠标", "运输中", "运单号：SF1234567891")

        val outcome = ParcelScreenParser.parseSnapshot(
            sourcePackage = "com.jingdong.app.mall",
            sourceLabel = "京东",
            snapshot = VisiblePageSnapshot(first + second, listOf(first, second), 24)
        )

        assertEquals(setOf("JDVA1234567890", "SF1234567891"), outcome.parcels.mapNotNull { it.trackingNumber }.toSet())
    }

    @Test
    fun explicitCurrentStatusWinsOverOlderTimelineEntries() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.cainiao.wireless",
            sourceLabel = "菜鸟",
            visibleTexts = listOf(
                "物流详情", "运单号：YT1234567890", "当前状态：运输中", "已发货", "已揽收"
            )
        )!!

        assertEquals(ParcelStatus.IN_TRANSIT, result.status)
    }

    @Test
    fun sensitivePageProducesBlockedDiagnosticWithoutParcel() {
        val outcome = ParcelScreenParser.parseSnapshot(
            sourcePackage = "com.jingdong.app.mall",
            sourceLabel = "京东",
            snapshot = VisiblePageSnapshot(
                listOf("订单号：JD1234567890", "收银台", "请输入支付密码"),
                emptyList(),
                8
            )
        )

        assertTrue(outcome.parcels.isEmpty())
        assertEquals(CaptureDiagnosticCode.SENSITIVE_PAGE_BLOCKED, outcome.diagnosticCode)
    }

    @Test
    fun parsesJdSpecificOrderAndDeliveryLabels() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.jingdong.app.mall",
            sourceLabel = "京东",
            visibleTexts = listOf(
                "订单跟踪", "京东订单号：JD202609270001", "配送单号：JDVA202609270001",
                "当前状态：派送中"
            )
        )!!

        assertEquals("JD202609270001", result.orderReference)
        assertEquals("JDVA202609270001", result.trackingNumber)
        assertEquals(ParcelStatus.OUT_FOR_DELIVERY, result.status)
    }

    @Test
    fun parsesPinduoduoSpecificGroupOrderLabel() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.xunmeng.pinduoduo",
            sourceLabel = "拼多多",
            visibleTexts = listOf(
                "拼单", "拼单编号：PDD202609270001", "物流单号：JT202609270001", "商家已发货"
            )
        )!!

        assertEquals("PDD202609270001", result.orderReference)
        assertEquals("JT202609270001", result.trackingNumber)
        assertEquals(ParcelStatus.SHIPPED, result.status)
    }

    @Test
    fun parsesCainiaoPackageNumberWithoutOrderNumber() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.cainiao.wireless",
            sourceLabel = "菜鸟",
            visibleTexts = listOf("包裹详情", "包裹单号：YT202609270001", "最新物流：运输中")
        )!!

        assertEquals("YT202609270001", result.trackingNumber)
        assertEquals(ParcelStatus.IN_TRANSIT, result.status)
    }

    @Test
    fun parsesSfSpecificWaybillLabel() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.sf.activity",
            sourceLabel = "顺丰",
            visibleTexts = listOf("顺丰速运", "快件详情", "顺丰运单号：SF202609270001", "快件状态：已签收")
        )!!

        assertEquals("SF202609270001", result.trackingNumber)
        assertEquals(ParcelStatus.DELIVERED, result.status)
    }

    @Test
    fun keepsTmallRulesSeparateFromTaobaoPackage() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.tmall.wireless",
            sourceLabel = "天猫",
            visibleTexts = listOf("天猫订单", "交易号：TM202609270001", "运单号：ZTO202609270001", "运输中")
        )!!

        assertEquals("TM202609270001", result.orderReference)
        assertEquals("ZTO202609270001", result.trackingNumber)
    }

    @Test
    fun blocksFoodAndInstantDeliveryEvenWhenOrderIdentifiersExist() {
        val outcome = ParcelScreenParser.parseSnapshot(
            sourcePackage = "com.jingdong.app.mall",
            sourceLabel = "京东",
            snapshot = VisiblePageSnapshot(
                listOf("京东外卖", "外卖订单", "订单号：JD202609270001", "骑手正在配送"),
                emptyList(),
                18
            )
        )

        assertTrue(outcome.parcels.isEmpty())
        assertEquals(CaptureDiagnosticCode.FOOD_OR_INSTANT_DELIVERY_BLOCKED, outcome.diagnosticCode)
        assertEquals("food_or_instant_delivery", outcome.pageKind)
    }

    @Test
    fun blocksRefundCancellationAndAfterSalesPages() {
        val outcome = ParcelScreenParser.parseSnapshot(
            sourcePackage = "com.taobao.taobao",
            sourceLabel = "淘宝",
            snapshot = VisiblePageSnapshot(
                listOf("售后详情", "退款成功", "订单号：TB202609270001", "运单号：YT202609270001"),
                emptyList(),
                15
            )
        )

        assertTrue(outcome.parcels.isEmpty())
        assertEquals(CaptureDiagnosticCode.AFTER_SALES_PAGE_BLOCKED, outcome.diagnosticCode)
        assertEquals("after_sales", outcome.pageKind)
    }

    @Test
    fun refundedCardDoesNotHideAnotherActiveParcelOnTheSameList() {
        val refunded = listOf("退款成功", "订单号：TB202609270001", "运单号：YT202609270001")
        val active = listOf("查看物流", "订单号：TB202609270002", "运单号：SF202609270002", "运输中")
        val outcome = ParcelScreenParser.parseSnapshot(
            sourcePackage = "com.taobao.taobao",
            sourceLabel = "淘宝",
            snapshot = VisiblePageSnapshot(refunded + active, listOf(refunded, active), 32)
        )

        assertEquals(1, outcome.parcels.size)
        assertEquals("TB202609270002", outcome.parcels.single().orderReference)
        assertEquals(CaptureDiagnosticCode.CAPTURED, outcome.diagnosticCode)
    }

    @Test
    fun clippedOrderCardWithoutStatusOrTrackingIsNotSavedAsPending() {
        val outcome = ParcelScreenParser.parseSnapshot(
            sourcePackage = "com.taobao.taobao",
            sourceLabel = "淘宝",
            snapshot = VisiblePageSnapshot(
                fullText = listOf("我的订单", "查看物流", "商品：平台测试包裹", "订单号：TB202609290001"),
                candidateBlocks = listOf(
                    listOf("订单详情", "查看物流", "商品：平台测试包裹", "订单号：TB202609290001")
                ),
                visibleNodeCount = 8
            )
        )

        assertTrue(outcome.parcels.isEmpty())
        assertEquals(CaptureDiagnosticCode.LOW_CONFIDENCE, outcome.diagnosticCode)
    }

    @Test
    fun orderOnlyCaptureRequiresAnExplicitPendingState() {
        val result = ParcelScreenParser.parse(
            sourcePackage = "com.jingdong.app.mall",
            sourceLabel = "京东",
            visibleTexts = listOf("订单详情", "京东订单号：JD202609290001", "订单提交成功", "待发货")
        )

        assertEquals(ParcelStatus.PENDING, result?.status)
        assertEquals("JD202609290001", result?.orderReference)
    }

    @Test
    fun partiallyVisibleSecondJdCardDoesNotChangeFirstCardStatus() {
        val first = listOf(
            "订单详情", "订单跟踪", "商品：平台测试包裹01", "京东订单号：JDORDER2026092901",
            "当前状态：运输中", "配送单号：JDTRACK2026092901"
        )
        val clippedSecond = listOf(
            "订单详情", "订单跟踪", "商品：平台测试包裹02", "京东订单号：JDORDER2026092902",
            "当前状态：派送中"
        )
        val full = listOf(
            "订单跟踪", "订单详情", "商品：平台测试包裹01", "京东订单号：JDORDER2026092901",
            "当前状态：运输中", "配送单号：JDTRACK2026092901", "商品：平台测试包裹02",
            "京东订单号：JDORDER2026092902", "当前状态：派送中"
        )

        val outcome = ParcelScreenParser.parseSnapshot(
            sourcePackage = "com.jingdong.app.mall",
            sourceLabel = "京东",
            snapshot = VisiblePageSnapshot(full, listOf(first, clippedSecond, full), 18)
        )

        assertEquals(2, outcome.parcels.size)
        assertEquals(
            ParcelStatus.IN_TRANSIT,
            outcome.parcels.single { it.trackingNumber == "JDTRACK2026092901" }.status
        )
        assertEquals(
            ParcelStatus.OUT_FOR_DELIVERY,
            outcome.parcels.single { it.orderReference == "JDORDER2026092902" }.status
        )
    }

    @Test
    fun jdSplitShipmentKeepsRepeatedCardBoundariesAndIndependentStatuses() {
        val sharedOrder = "JDORDER900001"
        val full = listOf(
            "订单跟踪",
            "订单详情", "订单跟踪", "京东订单号：$sharedOrder", "当前状态：已发货",
            "配送单号：JDTRACK900001",
            "订单详情", "订单跟踪", "京东订单号：$sharedOrder", "当前状态：运输中",
            "配送单号：JDTRACK900002"
        )

        val outcome = ParcelScreenParser.parseSnapshot(
            sourcePackage = "com.jingdong.app.mall",
            sourceLabel = "京东",
            snapshot = VisiblePageSnapshot(full, listOf(full), 20)
        )

        assertEquals(2, outcome.parcels.size)
        assertEquals(
            ParcelStatus.SHIPPED,
            outcome.parcels.single { it.trackingNumber == "JDTRACK900001" }.status
        )
        assertEquals(
            ParcelStatus.IN_TRANSIT,
            outcome.parcels.single { it.trackingNumber == "JDTRACK900002" }.status
        )
    }
}
