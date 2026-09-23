package com.parcelinbox.app.notifications

import com.parcelinbox.app.data.ParcelStatus
import com.parcelinbox.app.data.ParsedParcel

object ParcelNotificationParser {
    private val relevantWords = listOf(
        "订单", "发货", "物流", "快递", "包裹", "运单", "派送", "配送",
        "驿站", "丰巢", "快递柜", "取件", "签收", "投递", "order", "shipped",
        "tracking", "delivery", "package", "pedido", "seguimiento", "rastreamento",
        "pesanan", "resi", "注文", "配送", "追跡"
    )

    private val blockedWords = listOf(
        "支付密码", "付款密码", "银行卡号", "信用卡号", "验证码", "card number", "security code",
        "payment password", "one-time password", "número de tarjeta", "número do cartão",
        "nomor kartu", "カード番号", "セキュリティコード"
    )
    private val privateTitleWords = listOf(
        "收货人", "收件人", "地址", "手机号", "联系电话", "身份证", "支付方式", "付款方式",
        "银行卡", "信用卡", "密码", "验证码", "address", "phone", "recipient", "payment method",
        "credit card", "password", "dirección", "direccion", "teléfono", "telefono", "endereço",
        "endereco", "telefone", "alamat", "nomor telepon", "住所", "電話番号", "支払い方法"
    )
    private val phoneLike = Regex("(?<!\\d)\\+?\\d[\\d ()-]{8,18}\\d(?!\\d)")
    private val emailLike = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)

    private val labelledTrackingNumber = Regex(
        "(?:运单号|快递单号|物流单号|单号|tracking\\s*(?:number|no\\.?|#)|n[uú]mero\\s+de\\s+seguimiento|c[oó]digo\\s+de\\s+rastreio|nomor\\s+resi|追跡番号)[：:\\s#]*([A-Z0-9][A-Z0-9-]{7,29})",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val commonTrackingNumber = Regex("\\b[A-Z]{1,4}[0-9]{8,22}[A-Z0-9]?\\b")
    private val pickupCode = Regex(
        "(?:取件码|提货码|开柜码|pickup code|c[oó]digo\\s+(?:de\\s+)?retirada|kode\\s+pengambilan|受取コード)[：:\\s]*([A-Z0-9-]{3,12})",
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
        if (blockedWords.any { combined.contains(it, ignoreCase = true) }) return null
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
        containsAny(content, "取件码", "待取件", "驿站", "丰巢", "快递柜", "ready for pickup", "listo para retirar", "pronto para retirada", "siap diambil") -> ParcelStatus.READY_FOR_PICKUP
        containsAny(content, "正在派送", "派件中", "派送中", "配送中", "out for delivery", "saiu para entrega", "配達中") -> ParcelStatus.OUT_FOR_DELIVERY
        containsAny(content, "已签收", "已送达", "投递成功", "delivered", "entregado", "entregue", "terkirim", "配達完了") -> ParcelStatus.DELIVERED
        containsAny(content, "运输中", "运输途中", "到达转运", "离开转运", "in transit", "en camino", "dalam perjalanan") -> ParcelStatus.IN_TRANSIT
        containsAny(content, "已发货", "发货成功", "包裹已揽收", "快递已揽收", "shipped", "enviado", "dikirim", "発送済み") -> ParcelStatus.SHIPPED
        else -> ParcelStatus.PENDING
    }

    private fun containsAny(content: String, vararg words: String): Boolean =
        words.any { content.contains(it, ignoreCase = true) }

    private fun displayTitle(title: String, text: String, sourceLabel: String): String {
        val genericTitles = setOf("物流通知", "订单通知", "服务通知", sourceLabel)
        if (title.isNotBlank() && title !in genericTitles && isSafeTitle(title)) {
            return title.take(80)
        }
        return text.lineSequence().firstOrNull { line ->
            line.isNotBlank() && isSafeTitle(line)
        }?.take(80)
            ?: "$sourceLabel 包裹"
    }

    private fun isSafeTitle(value: String): Boolean {
        // Tracking identifiers commonly contain a long numeric run. Remove only
        // positively identified tracking labels/patterns before checking for a
        // phone number so a safe logistics summary is not mistaken for a phone.
        val withoutTracking = commonTrackingNumber.replace(
            labelledTrackingNumber.replace(value, ""),
            ""
        )
        return blockedWords.none { value.contains(it, true) } &&
            privateTitleWords.none { value.contains(it, true) } &&
            !phoneLike.containsMatchIn(withoutTracking) &&
            !emailLike.containsMatchIn(value)
    }
}
