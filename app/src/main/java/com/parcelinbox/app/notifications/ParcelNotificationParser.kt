package com.parcelinbox.app.notifications

import com.parcelinbox.app.data.ParcelStatus
import com.parcelinbox.app.data.ParsedParcel

object ParcelNotificationParser {
    private val relevantWords = listOf(
        "订单", "发货", "物流", "快递", "包裹", "运单", "派送", "配送",
        "驿站", "丰巢", "快递柜", "取件", "签收", "投递"
    )

    private val labelledTrackingNumber = Regex(
        "(?:运单号|快递单号|物流单号|单号)[：:\\s#]*([A-Z0-9][A-Z0-9-]{7,29})",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val commonTrackingNumber = Regex("\\b[A-Z]{1,4}[0-9]{8,22}[A-Z0-9]?\\b")
    private val pickupCode = Regex(
        "(?:取件码|提货码|开柜码)[：:\\s]*([A-Z0-9-]{3,10})",
        setOf(RegexOption.IGNORE_CASE)
    )

    fun parse(
        sourcePackage: String,
        sourceLabel: String,
        notificationTitle: String?,
        notificationText: String?,
        observedAt: Long = System.currentTimeMillis()
    ): ParsedParcel? {
        val title = notificationTitle.orEmpty().trim()
        val text = notificationText.orEmpty().trim()
        val combined = "$title\n$text".trim()
        if (combined.isBlank() || relevantWords.none { combined.contains(it, ignoreCase = true) }) {
            return null
        }

        val status = detectStatus(combined)
        val tracking = labelledTrackingNumber.find(combined)?.groupValues?.getOrNull(1)
            ?: commonTrackingNumber.find(combined)?.value
        val code = pickupCode.find(combined)?.groupValues?.getOrNull(1)

        return ParsedParcel(
            sourcePackage = sourcePackage,
            sourceLabel = sourceLabel,
            title = displayTitle(title, text, sourceLabel),
            trackingNumber = tracking,
            pickupCode = code,
            status = status,
            observedAt = observedAt
        )
    }

    private fun detectStatus(content: String): ParcelStatus = when {
        containsAny(content, "取件成功", "已取出", "已领取") -> ParcelStatus.COMPLETED
        containsAny(content, "物流异常", "派送失败", "投递失败", "运输异常", "退回") -> ParcelStatus.EXCEPTION
        containsAny(content, "取件码", "待取件", "驿站", "丰巢", "快递柜") -> ParcelStatus.READY_FOR_PICKUP
        containsAny(content, "正在派送", "派件中", "派送中", "配送中") -> ParcelStatus.OUT_FOR_DELIVERY
        containsAny(content, "已签收", "已送达", "投递成功") -> ParcelStatus.DELIVERED
        containsAny(content, "运输中", "运输途中", "到达转运", "离开转运") -> ParcelStatus.IN_TRANSIT
        containsAny(content, "已发货", "发货成功", "包裹已揽收", "快递已揽收") -> ParcelStatus.SHIPPED
        else -> ParcelStatus.PENDING
    }

    private fun containsAny(content: String, vararg words: String): Boolean =
        words.any { content.contains(it, ignoreCase = true) }

    private fun displayTitle(title: String, text: String, sourceLabel: String): String {
        val genericTitles = setOf("物流通知", "订单通知", "服务通知", sourceLabel)
        if (title.isNotBlank() && title !in genericTitles) return title.take(80)
        return text.lineSequence().firstOrNull { it.isNotBlank() }?.take(80)
            ?: "$sourceLabel 包裹"
    }
}
