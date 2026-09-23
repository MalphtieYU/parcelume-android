package com.parcelinbox.app.capture

import com.parcelinbox.app.data.CaptureMethod
import com.parcelinbox.app.data.ParcelStatus
import com.parcelinbox.app.data.ParsedParcel

/**
 * Converts text that is already visible in a user-selected shopping app into
 * minimal structured parcel fields. The caller must discard the raw text after
 * this method returns.
 */
object ParcelScreenParser {
    private val screenEvidence = listOf(
        "下单成功", "支付成功", "订单详情", "订单编号", "订单号", "待付款", "待发货",
        "已发货", "查看物流", "物流详情", "运单号", "快递单号", "运输中", "派送中",
        "待取件", "取件码", "已签收", "已送达", "order details", "order number",
        "order #", "shipped", "track package", "tracking number", "out for delivery",
        "ready for pickup", "delivered"
    )

    private val operationalWords = listOf(
        "订单", "物流", "快递", "运单", "取件", "待付款", "待发货", "已发货", "运输中",
        "派送", "签收", "送达", "退款", "客服", "购物车", "首页", "我的", "消息",
        "order", "shipping", "tracking", "delivery", "delivered", "refund", "customer service",
        "cart", "home", "account"
    )
    private val sensitiveWords = listOf(
        "收货人", "收件人", "收货地址", "详细地址", "联系电话", "手机号", "address", "phone"
    )
    private val orderReference = Regex(
        "(?:订单编号|订单号|order\\s*(?:number|no\\.?|#|id))[：:\\s#]*([A-Z0-9-]{6,40})",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val labelledTrackingNumber = Regex(
        "(?:运单号|快递单号|物流单号|tracking\\s*(?:number|no\\.?|#))[：:\\s#]*([A-Z0-9][A-Z0-9-]{7,29})",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val commonTrackingNumber = Regex("\\b[A-Z]{1,4}[0-9]{8,22}[A-Z0-9]?\\b")
    private val pickupCode = Regex(
        "(?:取件码|提货码|开柜码|pickup code)[：:\\s]*([A-Z0-9-]{3,12})",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val explicitProduct = Regex(
        "(?:商品(?:名称)?|宝贝|product|item)[：:\\s]+(.{3,80})",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val phoneLike = Regex("(?<!\\d)1[3-9]\\d{9}(?!\\d)")
    private val priceLike = Regex("^[¥￥$€£]?\\s*\\d+(?:[.,]\\d{1,2})?\\s*$")

    fun parse(
        sourcePackage: String,
        sourceLabel: String,
        visibleTexts: List<String>,
        observedAt: Long = System.currentTimeMillis()
    ): ParsedParcel? {
        val lines = visibleTexts
            .asSequence()
            .flatMap { it.lineSequence() }
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_LINES)
            .toList()
        val combined = lines.joinToString("\n").take(MAX_TEXT_LENGTH)

        if (combined.isBlank() || screenEvidence.none { combined.contains(it, ignoreCase = true) }) {
            return null
        }

        val tracking = labelledTrackingNumber.find(combined)?.groupValues?.getOrNull(1)
            ?: commonTrackingNumber.find(combined)?.value
        val order = orderReference.find(combined)?.groupValues?.getOrNull(1)
        val code = pickupCode.find(combined)?.groupValues?.getOrNull(1)
        val title = findProductTitle(lines, combined, sourceLabel)
        if (order == null && tracking == null && code == null && title == "$sourceLabel 订单") {
            return null
        }

        return ParsedParcel(
            sourcePackage = sourcePackage,
            sourceLabel = sourceLabel,
            title = title,
            orderReference = order,
            trackingNumber = tracking,
            pickupCode = code,
            status = detectStatus(combined),
            observedAt = observedAt,
            captureMethod = CaptureMethod.SCREEN
        )
    }

    private fun findProductTitle(lines: List<String>, combined: String, sourceLabel: String): String {
        explicitProduct.find(combined)?.groupValues?.getOrNull(1)?.let { value ->
            sanitizeTitle(value)?.let { return it }
        }

        return lines
            .asSequence()
            .mapNotNull(::sanitizeTitle)
            .filter { candidate ->
                candidate.length in 4..64 &&
                    !candidate.equals(sourceLabel, ignoreCase = true) &&
                    operationalWords.none { candidate.contains(it, ignoreCase = true) } &&
                    sensitiveWords.none { candidate.contains(it, ignoreCase = true) } &&
                    !priceLike.matches(candidate) &&
                    candidate.any { it.isLetter() }
            }
            .maxByOrNull { candidate ->
                (if (candidate.length in 8..36) 4 else 0) +
                    (if (candidate.any { it.code > 127 }) 2 else 0) -
                    candidate.count { it.isDigit() }
            }
            ?: "$sourceLabel 订单"
    }

    private fun sanitizeTitle(value: String): String? {
        val withoutPhone = phoneLike.replace(value, "")
            .substringBefore("收货地址")
        val addressIndex = withoutPhone.indexOf("address", ignoreCase = true)
        val withoutAddress = if (addressIndex >= 0) withoutPhone.substring(0, addressIndex) else withoutPhone
        val sanitized = withoutAddress
            .trim(' ', '：', ':', '-', '|')
            .take(80)
        return sanitized.takeIf { it.length >= 3 }
    }

    private fun detectStatus(content: String): ParcelStatus = when {
        containsAny(content, "取件成功", "已取出", "已领取", "picked up") -> ParcelStatus.COMPLETED
        containsAny(content, "物流异常", "派送失败", "投递失败", "运输异常", "退回", "delivery exception") -> ParcelStatus.EXCEPTION
        containsAny(content, "取件码", "待取件", "驿站", "丰巢", "快递柜", "ready for pickup") -> ParcelStatus.READY_FOR_PICKUP
        containsAny(content, "正在派送", "派件中", "派送中", "配送中", "out for delivery") -> ParcelStatus.OUT_FOR_DELIVERY
        containsAny(content, "已签收", "已送达", "投递成功", "delivered") -> ParcelStatus.DELIVERED
        containsAny(content, "运输中", "运输途中", "到达转运", "离开转运", "in transit") -> ParcelStatus.IN_TRANSIT
        containsAny(content, "已发货", "发货成功", "包裹已揽收", "快递已揽收", "shipped") -> ParcelStatus.SHIPPED
        else -> ParcelStatus.PENDING
    }

    private fun containsAny(content: String, vararg words: String): Boolean =
        words.any { content.contains(it, ignoreCase = true) }

    private const val MAX_LINES = 220
    private const val MAX_TEXT_LENGTH = 12_000
}
