package com.parcelinbox.app.capture

internal enum class PlatformProfile {
    TAOBAO,
    TMALL,
    JD,
    PINDUODUO,
    CAINIAO,
    SF_EXPRESS,
    GENERIC
}

internal data class PlatformRule(
    val profile: PlatformProfile,
    val packageName: String,
    val evidence: List<String>,
    val orderLabels: List<String>,
    val trackingLabels: List<String>,
    val explicitStatusLabels: List<String>
)

/**
 * Package-scoped parsing vocabulary for the China-region stabilization track.
 * Keeping these rules independent prevents a wording change in one shopping
 * app from silently changing the behavior of the other core apps.
 */
internal object ChinaPlatformRules {
    const val PARSER_VERSION = "CN-0.3.3-1"

    private val rules = listOf(
        PlatformRule(
            profile = PlatformProfile.TAOBAO,
            packageName = "com.taobao.taobao",
            evidence = listOf("查看物流", "延长收货", "确认收货", "卖家已发货", "交易号", "包裹正在"),
            orderLabels = listOf("订单编号", "订单号", "主订单号", "子订单号", "交易号"),
            trackingLabels = listOf("运单号", "快递单号", "物流单号"),
            explicitStatusLabels = listOf("当前状态", "订单状态", "物流状态", "最新物流", "最新进展")
        ),
        PlatformRule(
            profile = PlatformProfile.TMALL,
            packageName = "com.tmall.wireless",
            evidence = listOf("查看物流", "延长收货", "确认收货", "卖家已发货", "交易号", "天猫订单"),
            orderLabels = listOf("订单编号", "订单号", "主订单号", "子订单号", "交易号"),
            trackingLabels = listOf("运单号", "快递单号", "物流单号"),
            explicitStatusLabels = listOf("当前状态", "订单状态", "物流状态", "最新物流", "最新进展")
        ),
        PlatformRule(
            profile = PlatformProfile.JD,
            packageName = "com.jingdong.app.mall",
            evidence = listOf("订单跟踪", "京东快递", "配送员", "正在出库", "已完成", "运单详情"),
            orderLabels = listOf("订单编号", "订单号", "京东订单号", "主订单号"),
            trackingLabels = listOf("运单号", "快递单号", "配送单号", "物流单号"),
            explicitStatusLabels = listOf("当前状态", "订单状态", "物流状态", "订单跟踪", "最新进展")
        ),
        PlatformRule(
            profile = PlatformProfile.PINDUODUO,
            packageName = "com.xunmeng.pinduoduo",
            evidence = listOf("拼单", "查看物流", "平台已出单", "商家已发货", "快递已揽件"),
            orderLabels = listOf("订单编号", "订单号", "拼单编号", "主订单号"),
            trackingLabels = listOf("运单号", "快递单号", "物流单号"),
            explicitStatusLabels = listOf("当前状态", "订单状态", "物流状态", "最新物流", "最新进展")
        ),
        PlatformRule(
            profile = PlatformProfile.CAINIAO,
            packageName = "com.cainiao.wireless",
            evidence = listOf("菜鸟驿站", "包裹详情", "物流轨迹", "最新物流", "丰巢", "驿站", "快递员"),
            orderLabels = listOf("订单编号", "订单号"),
            trackingLabels = listOf("运单号", "快递单号", "物流单号", "包裹单号"),
            explicitStatusLabels = listOf("当前状态", "物流状态", "最新物流", "最新进展")
        ),
        PlatformRule(
            profile = PlatformProfile.SF_EXPRESS,
            packageName = "com.sf.activity",
            evidence = listOf("顺丰速运", "顺丰运单", "快件详情", "路由信息", "丰巢", "派件员"),
            orderLabels = listOf("订单编号", "订单号"),
            trackingLabels = listOf("顺丰运单号", "运单号", "快递单号", "母单号", "子单号"),
            explicitStatusLabels = listOf("当前状态", "快件状态", "物流状态", "最新进展")
        )
    ).associateBy(PlatformRule::packageName)

    fun forPackage(packageName: String): PlatformRule = rules[packageName] ?: PlatformRule(
        profile = PlatformProfile.GENERIC,
        packageName = packageName,
        evidence = emptyList(),
        orderLabels = emptyList(),
        trackingLabels = emptyList(),
        explicitStatusLabels = listOf("当前状态", "订单状态", "物流状态", "最新物流", "最新进展")
    )

    fun isCorePackage(packageName: String): Boolean = rules.containsKey(packageName)
}
