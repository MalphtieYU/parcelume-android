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
        "下单成功", "订单详情", "订单编号", "订单号", "待发货",
        "已发货", "查看物流", "物流详情", "运单号", "快递单号", "运输中", "派送中",
        "待取件", "取件码", "已签收", "已送达", "order details", "order number",
        "order #", "shipped", "track package", "tracking number", "out for delivery",
        "ready for pickup", "delivered",
        "detalles del pedido", "número de pedido", "numero de pedido", "número de seguimiento",
        "enviado", "en camino", "listo para retirar", "entregado",
        "detalhes do pedido", "número do pedido", "numero do pedido", "código de rastreio",
        "rastreamento", "saiu para entrega", "pronto para retirada", "entregue",
        "detail pesanan", "nomor pesanan", "nomor resi", "dalam perjalanan", "siap diambil",
        "注文詳細", "注文番号", "発送済み", "配送状況", "追跡番号", "配達中", "配達完了"
    )

    // Pages that can expose credentials or payment instruments are never parsed,
    // even if order-related words are present elsewhere on the same screen.
    private val blockedPageEvidence = listOf(
        "收银台", "支付密码", "付款密码", "银行卡号", "信用卡号", "短信验证码", "身份验证码",
        "card number", "security code", "payment password", "one-time password", "enter cvv",
        "checkout payment", "confirm payment", "billing information",
        "número de tarjeta", "codigo de seguridad", "código de seguridad", "contraseña de pago",
        "número do cartão", "codigo de segurança", "código de segurança", "senha de pagamento",
        "nomor kartu", "kata sandi pembayaran", "kode keamanan",
        "カード番号", "セキュリティコード", "支払いパスワード"
    )

    private val operationalWords = listOf(
        "订单", "物流", "快递", "运单", "取件", "待付款", "待发货", "已发货", "运输中",
        "派送", "签收", "送达", "退款", "客服", "购物车", "首页", "我的", "消息",
        "order", "shipping", "tracking", "delivery", "delivered", "refund", "customer service",
        "cart", "home", "account", "pedido", "seguimiento", "enviado", "entregado",
        "rastreamento", "entregue", "pesanan", "dikirim", "terkirim", "注文", "配送", "発送"
    )
    private val sensitiveWords = listOf(
        "收货人", "收件人", "收货地址", "详细地址", "联系电话", "手机号", "身份证",
        "支付方式", "付款方式", "银行卡", "信用卡", "验证码", "密码",
        "address", "phone", "email", "recipient", "payment method", "bank account", "credit card",
        "billing", "password", "verification code", "dirección", "direccion", "teléfono", "telefono",
        "método de pago", "metodo de pago", "tarjeta", "endereço", "endereco", "telefone",
        "forma de pagamento", "cartão", "cartao", "alamat", "nomor telepon", "metode pembayaran",
        "kartu", "住所", "電話番号", "支払い方法", "クレジットカード"
    )
    private val orderReference = Regex(
        "(?:订单编号|订单号|order\\s*(?:number|no\\.?|#|id)|n[uú]mero\\s+(?:de|do)\\s+pedido|nomor\\s+pesanan|注文番号)[：:\\s#]*([A-Z0-9-]{6,40})",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val labelledTrackingNumber = Regex(
        "(?:运单号|快递单号|物流单号|tracking\\s*(?:number|no\\.?|#)|n[uú]mero\\s+de\\s+seguimiento|c[oó]digo\\s+de\\s+rastreio|nomor\\s+resi|追跡番号)[：:\\s#]*([A-Z0-9][A-Z0-9-]{7,29})",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val commonTrackingNumber = Regex("\\b[A-Z]{1,4}[0-9]{8,22}[A-Z0-9]?\\b")
    private val pickupCode = Regex(
        "(?:取件码|提货码|开柜码|pickup code|c[oó]digo\\s+(?:de\\s+)?retirada|kode\\s+pengambilan|受取コード)[：:\\s]*([A-Z0-9-]{3,12})",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val explicitProduct = Regex(
        "(?:商品(?:名称)?|宝贝|product|item|producto|produto|barang|商品名)[：:\\s]+(.{3,80})",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val phoneLike = Regex("(?<!\\d)1[3-9]\\d{9}(?!\\d)")
    private val internationalPhoneLike = Regex("(?<!\\d)\\+?\\d[\\d ()-]{8,18}\\d(?!\\d)")
    private val emailLike = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)
    private val cardLike = Regex("(?<!\\d)\\d{4}[ -]\\d{4}[ -]\\d{4}(?:[ -]\\d{1,7})?(?!\\d)")
    private val priceLike = Regex("^[¥￥$€£]?\\s*\\d+(?:[.,]\\d{1,2})?\\s*$")

    fun parse(
        sourcePackage: String,
        sourceLabel: String,
        visibleTexts: List<String>,
        observedAt: Long = System.currentTimeMillis(),
        includeItemTitle: Boolean = true
    ): ParsedParcel? {
        val normalizedLines = visibleTexts
            .asSequence()
            .flatMap { it.lineSequence() }
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_LINES)
            .toList()
        val rawEvidence = normalizedLines.joinToString("\n").take(MAX_TEXT_LENGTH)

        if (blockedPageEvidence.any { rawEvidence.contains(it, ignoreCase = true) }) return null

        // Remove address, contact, authentication and payment rows before any
        // extraction. The original strings are eligible for GC after this call.
        val lines = normalizedLines.filterNot(::containsSensitiveText)
        val combined = lines.joinToString("\n").take(MAX_TEXT_LENGTH)

        if (combined.isBlank() || screenEvidence.none { combined.contains(it, ignoreCase = true) }) {
            return null
        }

        val tracking = labelledTrackingNumber.find(combined)?.groupValues?.getOrNull(1)
            ?: commonTrackingNumber.find(combined)?.value
        val order = orderReference.find(combined)?.groupValues?.getOrNull(1)
        val code = pickupCode.find(combined)?.groupValues?.getOrNull(1)
        val title = if (includeItemTitle) {
            findProductTitle(lines, combined, sourceLabel)
        } else {
            "$sourceLabel 包裹"
        }
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
        if (containsSensitiveText(value) || cardLike.containsMatchIn(value)) return null
        val withoutPhone = internationalPhoneLike.replace(phoneLike.replace(value, ""), "")
            .substringBefore("收货地址")
        val addressIndex = withoutPhone.indexOf("address", ignoreCase = true)
        val withoutAddress = if (addressIndex >= 0) withoutPhone.substring(0, addressIndex) else withoutPhone
        val sanitized = emailLike.replace(withoutAddress, "")
            .trim(' ', '：', ':', '-', '|')
            .take(80)
        return sanitized.takeIf { it.length >= 3 }
    }

    private fun containsSensitiveText(value: String): Boolean =
        sensitiveWords.any { value.contains(it, ignoreCase = true) } ||
            emailLike.containsMatchIn(value) ||
            cardLike.containsMatchIn(value)

    private fun detectStatus(content: String): ParcelStatus = when {
        containsAny(content, "取件成功", "已取出", "已领取", "picked up") -> ParcelStatus.COMPLETED
        containsAny(content, "物流异常", "派送失败", "投递失败", "运输异常", "退回", "delivery exception") -> ParcelStatus.EXCEPTION
        containsAny(content, "取件码", "待取件", "驿站", "丰巢", "快递柜", "ready for pickup", "listo para retirar", "pronto para retirada", "siap diambil", "受け取り") -> ParcelStatus.READY_FOR_PICKUP
        containsAny(content, "正在派送", "派件中", "派送中", "配送中", "out for delivery", "saiu para entrega", "配達中") -> ParcelStatus.OUT_FOR_DELIVERY
        containsAny(content, "已签收", "已送达", "投递成功", "delivered", "entregado", "entregue", "terkirim", "配達完了") -> ParcelStatus.DELIVERED
        containsAny(content, "运输中", "运输途中", "到达转运", "离开转运", "in transit", "en camino", "dalam perjalanan", "配送状況") -> ParcelStatus.IN_TRANSIT
        containsAny(content, "已发货", "发货成功", "包裹已揽收", "快递已揽收", "shipped", "enviado", "dikirim", "発送済み") -> ParcelStatus.SHIPPED
        else -> ParcelStatus.PENDING
    }

    private fun containsAny(content: String, vararg words: String): Boolean =
        words.any { content.contains(it, ignoreCase = true) }

    private const val MAX_LINES = 220
    private const val MAX_TEXT_LENGTH = 12_000
}
