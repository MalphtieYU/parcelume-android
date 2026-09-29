package com.parcelinbox.app.capture

import com.parcelinbox.app.data.CaptureMethod
import com.parcelinbox.app.data.ParcelStatus
import com.parcelinbox.app.data.ParsedParcel
import com.parcelinbox.app.data.progressRank

data class VisiblePageSnapshot(
    val fullText: List<String>,
    val candidateBlocks: List<List<String>>,
    val visibleNodeCount: Int
)

data class ScreenParseOutcome(
    val parcels: List<ParsedParcel>,
    val diagnosticCode: CaptureDiagnosticCode,
    val pageKind: String,
    val textNodeCount: Int
)

/**
 * Local, deterministic parsing for the visible portion of supported apps.
 * China-region apps have dedicated vocabulary profiles. Candidate page blocks
 * are parsed independently so an order list can produce multiple parcels
 * without mixing identifiers and statuses from unrelated cards.
 */
object ParcelScreenParser {
    private val generalEvidence = listOf(
        "下单成功", "订单详情", "订单编号", "订单号", "待发货", "待收货",
        "已发货", "查看物流", "物流详情", "物流信息", "运单号", "快递单号",
        "运输中", "派送中", "待取件", "取件码", "已签收", "已送达",
        "order details", "order number", "order #", "shipped", "track package", "tracking number",
        "out for delivery", "ready for pickup", "delivered", "detalles del pedido", "número de pedido",
        "numero de pedido", "número de seguimiento", "enviado", "en camino", "listo para retirar",
        "entregado", "detalhes do pedido", "número do pedido", "numero do pedido", "código de rastreio",
        "rastreamento", "saiu para entrega", "pronto para retirada", "entregue", "detail pesanan",
        "nomor pesanan", "nomor resi", "dalam perjalanan", "siap diambil", "注文詳細", "注文番号",
        "発送済み", "配送状況", "追跡番号", "配達中", "配達完了"
    )

    private val blockedPageEvidence = listOf(
        "收银台", "支付密码", "付款密码", "银行卡号", "信用卡号", "短信验证码", "身份验证码",
        "card number", "security code", "payment password", "one-time password", "enter cvv",
        "checkout payment", "confirm payment", "billing information", "número de tarjeta",
        "codigo de seguridad", "código de seguridad", "contraseña de pago", "número do cartão",
        "codigo de segurança", "código de segurança", "senha de pagamento", "nomor kartu",
        "kata sandi pembayaran", "kode keamanan", "カード番号", "セキュリティコード", "支払いパスワード"
    )

    private val foodOrInstantDeliveryEvidence = listOf(
        "外卖订单", "京东外卖", "淘宝闪购", "闪购订单", "饿了么订单", "美食外卖",
        "餐饮订单", "餐品", "骑手正在配送", "骑手已接单", "到店自取", "即时配送订单",
        "小时达订单", "秒送订单"
    )

    private val afterSalesEvidence = listOf(
        "售后详情", "退款详情", "退款成功", "已全额退款", "交易关闭", "订单已取消",
        "取消成功", "退货退款", "退货物流", "售后单号"
    )

    private val sensitiveWords = listOf(
        "收货人", "收件人", "收货地址", "详细地址", "联系电话", "手机号", "身份证",
        "支付方式", "付款方式", "银行卡", "信用卡", "验证码", "密码", "address", "phone", "email",
        "recipient", "payment method", "bank account", "credit card", "billing", "password",
        "verification code", "dirección", "direccion", "teléfono", "telefono", "método de pago",
        "metodo de pago", "tarjeta", "endereço", "endereco", "telefone", "forma de pagamento",
        "cartão", "cartao", "alamat", "nomor telepon", "metode pembayaran", "kartu", "住所", "電話番号",
        "支払い方法", "クレジットカード"
    )

    private val operationalWords = listOf(
        "订单", "物流", "快递", "运单", "取件", "待付款", "待发货", "已发货", "运输中",
        "派送", "签收", "送达", "退款", "客服", "购物车", "首页", "我的", "消息", "确认收货",
        "order", "shipping", "tracking", "delivery", "delivered", "refund", "customer service", "cart",
        "home", "account", "pedido", "seguimiento", "enviado", "entregado", "rastreamento", "entregue",
        "pesanan", "dikirim", "terkirim", "注文", "配送", "発送"
    )

    private val orderReference = Regex(
        "(?:订单编号|订单号|主订单号|子订单号|交易号|order\\s*(?:number|no\\.?|#|id)|n[uú]mero\\s+(?:de|do)\\s+pedido|nomor\\s+pesanan|注文番号)[：:\\s#-]*([A-Z0-9][A-Z0-9-]{5,39})",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val labelledTrackingNumber = Regex(
        "(?:运单号|快递单号|物流单号|快递单编号|运单编号|物流编号|快递编号|tracking\\s*(?:number|no\\.?|#)|n[uú]mero\\s+de\\s+seguimiento|c[oó]digo\\s+de\\s+rastreio|nomor\\s+resi|追跡番号)[：:\\s#-]*([A-Z0-9][A-Z0-9-]{7,31})",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val commonTrackingNumber = Regex("\\b[A-Z]{1,4}[0-9]{8,24}[A-Z0-9]?\\b")
    private val pickupCode = Regex(
        "(?:取件码|提货码|开柜码|凭取件码|pickup code|c[oó]digo\\s+(?:de\\s+)?retirada|kode\\s+pengambilan|受取コード)[：:\\s]*([A-Z0-9-]{3,16})",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val explicitProduct = Regex(
        "(?:商品(?:名称|信息|名)?|宝贝(?:名称)?|product|item|producto|produto|barang|商品名)[：:\\s]+(.{3,80})",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val phoneLike = Regex("(?<!\\d)1[3-9]\\d{9}(?!\\d)")
    private val internationalPhoneLike = Regex("(?<!\\d)\\+?\\d[\\d ()-]{8,18}\\d(?!\\d)")
    private val emailLike = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)
    private val cardLike = Regex("(?<!\\d)\\d{4}[ -]\\d{4}[ -]\\d{4}(?:[ -]\\d{1,7})?(?!\\d)")
    private val priceLike = Regex("^[¥￥$€£]?\\s*\\d+(?:[.,]\\d{1,2})?\\s*$")

    fun parseSnapshot(
        sourcePackage: String,
        sourceLabel: String,
        snapshot: VisiblePageSnapshot,
        observedAt: Long = System.currentTimeMillis(),
        includeItemTitle: Boolean = true
    ): ScreenParseOutcome {
        val fullLines = normalize(snapshot.fullText)
        if (fullLines.isEmpty()) {
            return ScreenParseOutcome(emptyList(), CaptureDiagnosticCode.NO_VISIBLE_TEXT, "empty", 0)
        }

        val fullEvidence = fullLines.joinToString("\n").take(MAX_TEXT_LENGTH)
        if (blockedPageEvidence.any { fullEvidence.contains(it, ignoreCase = true) }) {
            return ScreenParseOutcome(emptyList(), CaptureDiagnosticCode.SENSITIVE_PAGE_BLOCKED, "sensitive", fullLines.size)
        }

        val rule = ChinaPlatformRules.forPackage(sourcePackage)
        val fullPageExclusion = excludedPage(fullEvidence)
        if (fullPageExclusion != null && identityAnchorCount(fullLines, rule) <= 1) {
            val (code, kind) = fullPageExclusion
            return ScreenParseOutcome(emptyList(), code, kind, fullLines.size)
        }
        val inputs = buildList {
            snapshot.candidateBlocks.asSequence()
                .map(::normalize)
                .filter { it.size in 2..MAX_BLOCK_LINES }
                .distinctBy { it.joinToString("\u001f") }
                .sortedBy { it.size }
                .flatMap { block -> segmentByIdentifiers(block, rule).asSequence() }
                .forEach(::add)
            segmentByIdentifiers(fullLines, rule).forEach(::add)
            if (isEmpty()) add(fullLines)
        }

        val byIdentity = LinkedHashMap<String, ParsedParcel>()
        inputs.forEach { lines ->
            val parsed = parseLines(
                sourcePackage, sourceLabel, lines, observedAt, includeItemTitle, rule
            ) ?: return@forEach
            addCandidate(byIdentity, parsed)
        }

        val parcels = byIdentity.values.toList()
        if (parcels.isEmpty() && fullPageExclusion != null) {
            val (code, kind) = fullPageExclusion
            return ScreenParseOutcome(emptyList(), code, kind, fullLines.size)
        }
        val kind = detectPageKind(fullEvidence, rule, parcels.size)
        val hasEvidence = evidenceFor(rule).any { fullEvidence.contains(it, ignoreCase = true) }
        val code = when {
            parcels.isNotEmpty() -> CaptureDiagnosticCode.CAPTURED
            hasEvidence -> CaptureDiagnosticCode.LOW_CONFIDENCE
            else -> CaptureDiagnosticCode.UNSUPPORTED_PAGE
        }
        return ScreenParseOutcome(parcels, code, kind, fullLines.size)
    }

    private fun identityAnchorCount(lines: List<String>, rule: PlatformRule): Int {
        val platformOrder = platformOrderReference(rule)
        val platformTracking = platformTrackingNumber(rule)
        return lines.count { line ->
            platformOrder.containsMatchIn(line) || orderReference.containsMatchIn(line) ||
                platformTracking.containsMatchIn(line) || labelledTrackingNumber.containsMatchIn(line) ||
                pickupCode.containsMatchIn(line)
        }
    }

    /** Compatibility entry point for parser fixtures and simple single-card input. */
    fun parse(
        sourcePackage: String,
        sourceLabel: String,
        visibleTexts: List<String>,
        observedAt: Long = System.currentTimeMillis(),
        includeItemTitle: Boolean = true
    ): ParsedParcel? = parseSnapshot(
        sourcePackage,
        sourceLabel,
        VisiblePageSnapshot(visibleTexts, emptyList(), visibleTexts.size),
        observedAt,
        includeItemTitle
    ).parcels.firstOrNull()

    private fun parseLines(
        sourcePackage: String,
        sourceLabel: String,
        lines: List<String>,
        observedAt: Long,
        includeItemTitle: Boolean,
        rule: PlatformRule
    ): ParsedParcel? {
        val rawEvidence = lines.joinToString("\n").take(MAX_TEXT_LENGTH)
        if (rawEvidence.isBlank() || blockedPageEvidence.any { rawEvidence.contains(it, ignoreCase = true) }) return null
        if (excludedPage(rawEvidence) != null) return null

        val safeLines = lines.filterNot(::containsSensitiveText)
        val combined = safeLines.joinToString("\n").take(MAX_TEXT_LENGTH)
        if (combined.isBlank() || evidenceFor(rule).none { combined.contains(it, ignoreCase = true) }) return null

        val order = platformOrderReference(rule).find(combined)?.groupValues?.getOrNull(1)
            ?: orderReference.find(combined)?.groupValues?.getOrNull(1)
        val tracking = platformTrackingNumber(rule).find(combined)?.groupValues?.getOrNull(1)
            ?: labelledTrackingNumber.find(combined)?.groupValues?.getOrNull(1)
            ?: commonTrackingNumber.findAll(combined).map { it.value }
                .firstOrNull { !it.equals(order, ignoreCase = true) }
        val code = pickupCode.find(combined)?.groupValues?.getOrNull(1)
        if (order == null && tracking == null && code == null) return null

        val explicitTitle = explicitProduct.find(combined)?.groupValues?.getOrNull(1)?.let(::sanitizeTitle)
        val title = when {
            !includeItemTitle -> "$sourceLabel 包裹"
            explicitTitle != null -> explicitTitle
            else -> findProductTitle(safeLines, sourceLabel)
        }
        val detectedStatus = detectStatus(safeLines, rule)
        // A clipped order card commonly exposes its order number before its
        // status or tracking row enters the viewport. Do not create a false
        // "pending" parcel from that partial card; a later scroll event will
        // capture it once a real status, tracking number, or pickup code is visible.
        if (tracking == null && code == null && detectedStatus == null) return null
        val status = detectedStatus ?: ParcelStatus.PENDING
        val confidence = calculateConfidence(order, tracking, code, explicitTitle, status, rule, combined)
        if (confidence < MIN_CONFIDENCE) return null

        return ParsedParcel(
            sourcePackage = sourcePackage,
            sourceLabel = sourceLabel,
            title = title,
            orderReference = order,
            trackingNumber = tracking,
            pickupCode = code,
            status = status,
            observedAt = observedAt,
            captureMethod = CaptureMethod.SCREEN,
            confidence = confidence
        )
    }

    private fun segmentByIdentifiers(lines: List<String>, rule: PlatformRule): List<List<String>> {
        val platformOrder = platformOrderReference(rule)
        val platformTracking = platformTrackingNumber(rule)
        val orderAnchors = lines.indices.filter {
            platformOrder.containsMatchIn(lines[it]) || orderReference.containsMatchIn(lines[it])
        }
        val trackingAnchors = lines.indices.filter {
            platformTracking.containsMatchIn(lines[it]) || labelledTrackingNumber.containsMatchIn(lines[it])
        }
        val pickupAnchors = lines.indices.filter { pickupCode.containsMatchIn(lines[it]) }
        val anchors = when {
            trackingAnchors.size > 1 -> trackingAnchors
            orderAnchors.size > 1 -> orderAnchors
            pickupAnchors.size > 1 -> pickupAnchors
            else -> return listOf(lines)
        }
        return anchors.mapIndexed { position, anchor ->
            val previous = anchors.getOrNull(position - 1)
            val next = anchors.getOrNull(position + 1)
            val start = previous?.let { (it + anchor) / 2 + 1 } ?: maxOf(0, anchor - SEGMENT_CONTEXT)
            val endExclusive = next?.let { (anchor + it) / 2 + 1 } ?: minOf(lines.size, anchor + SEGMENT_CONTEXT + 1)
            lines.subList(start.coerceAtMost(anchor), endExclusive.coerceAtLeast(anchor + 1))
        }.distinctBy { it.joinToString("\u001f") }
    }

    private fun calculateConfidence(
        order: String?, tracking: String?, pickup: String?, explicitTitle: String?,
        status: ParcelStatus, rule: PlatformRule, content: String
    ): Int {
        var score = 0
        if (order != null) score += 38
        if (tracking != null) score += 45
        if (pickup != null) score += 45
        if (explicitTitle != null) score += 8
        if (status != ParcelStatus.PENDING) score += 10
        if (rule.profile != PlatformProfile.GENERIC && rule.evidence.any {
                content.contains(it, ignoreCase = true)
            }) score += 8
        return score.coerceAtMost(100)
    }

    private fun findProductTitle(lines: List<String>, sourceLabel: String): String = lines.asSequence()
        .mapNotNull(::sanitizeTitle)
        .filter { candidate ->
            candidate.length in 4..64 &&
                !candidate.equals(sourceLabel, ignoreCase = true) &&
                operationalWords.none { candidate.contains(it, ignoreCase = true) } &&
                sensitiveWords.none { candidate.contains(it, ignoreCase = true) } &&
                !orderReference.containsMatchIn(candidate) &&
                !labelledTrackingNumber.containsMatchIn(candidate) &&
                !pickupCode.containsMatchIn(candidate) &&
                !priceLike.matches(candidate) && candidate.any { it.isLetter() }
        }
        .maxByOrNull { candidate ->
            (if (candidate.length in 8..36) 4 else 0) +
                (if (candidate.any { it.code > 127 }) 2 else 0) - candidate.count { it.isDigit() }
        } ?: "$sourceLabel 包裹"

    private fun detectStatus(lines: List<String>, rule: PlatformRule): ParcelStatus? {
        lines.firstNotNullOfOrNull { line ->
            if (rule.explicitStatusLabels.any { line.contains(it, ignoreCase = true) }) {
                statusFromText(line)
            } else {
                null
            }
        }?.let { return it }
        val detected = lines.mapNotNull(::statusFromText)
        if (detected.contains(ParcelStatus.READY_FOR_PICKUP)) return ParcelStatus.READY_FOR_PICKUP
        if (detected.contains(ParcelStatus.EXCEPTION)) return ParcelStatus.EXCEPTION
        return detected.maxByOrNull(ParcelStatus::progressRank)
    }

    private fun statusFromText(content: String): ParcelStatus? = when {
        containsAny(content, "取件成功", "已取出", "已领取", "已取件", "picked up") -> ParcelStatus.COMPLETED
        containsAny(content, "取件码", "待取件", "请取件", "驿站待取", "ready for pickup", "listo para retirar", "pronto para retirada", "siap diambil", "受け取り") -> ParcelStatus.READY_FOR_PICKUP
        containsAny(content, "物流异常", "派送失败", "投递失败", "运输异常", "包裹退回", "delivery exception") -> ParcelStatus.EXCEPTION
        containsAny(content, "正在派送", "派件中", "派送中", "配送中", "正在配送", "out for delivery", "saiu para entrega", "配達中") -> ParcelStatus.OUT_FOR_DELIVERY
        containsAny(content, "已签收", "已送达", "投递成功", "本人签收", "delivered", "entregado", "entregue", "terkirim", "配達完了") -> ParcelStatus.DELIVERED
        containsAny(content, "运输中", "运输途中", "在途中", "转运中", "到达转运", "离开转运", "快件到达", "快件离开", "in transit", "en camino", "dalam perjalanan", "配送状況") -> ParcelStatus.IN_TRANSIT
        containsAny(content, "已发货", "卖家已发货", "商家已发货", "发货成功", "已揽收", "揽件成功", "快递已揽件", "仓库已出库", "shipped", "enviado", "dikirim", "発送済み") -> ParcelStatus.SHIPPED
        containsAny(content, "下单成功", "订单提交成功", "待发货", "等待发货", "备货中", "正在出库", "等待揽收") -> ParcelStatus.PENDING
        else -> null
    }

    private fun evidenceFor(rule: PlatformRule): List<String> = generalEvidence + rule.evidence

    private fun detectPageKind(content: String, rule: PlatformRule, count: Int): String = when {
        count > 1 -> "order_list"
        pickupCode.containsMatchIn(content) -> "pickup"
        platformTrackingNumber(rule).containsMatchIn(content) || labelledTrackingNumber.containsMatchIn(content) ||
            containsAny(content, "物流详情", "物流轨迹", "tracking") -> "tracking"
        platformOrderReference(rule).containsMatchIn(content) || orderReference.containsMatchIn(content) -> "order_detail"
        rule.profile != PlatformProfile.GENERIC -> "china_unknown"
        else -> "unknown"
    }

    private fun excludedPage(content: String): Pair<CaptureDiagnosticCode, String>? = when {
        foodOrInstantDeliveryEvidence.any { content.contains(it, ignoreCase = true) } ->
            CaptureDiagnosticCode.FOOD_OR_INSTANT_DELIVERY_BLOCKED to "food_or_instant_delivery"
        afterSalesEvidence.any { content.contains(it, ignoreCase = true) } ->
            CaptureDiagnosticCode.AFTER_SALES_PAGE_BLOCKED to "after_sales"
        else -> null
    }

    private fun platformOrderReference(rule: PlatformRule): Regex = labelledIdentifierRegex(
        labels = rule.orderLabels,
        minimumLength = 6,
        maximumLength = 40
    )

    private fun platformTrackingNumber(rule: PlatformRule): Regex = labelledIdentifierRegex(
        labels = rule.trackingLabels,
        minimumLength = 8,
        maximumLength = 32
    )

    private fun labelledIdentifierRegex(labels: List<String>, minimumLength: Int, maximumLength: Int): Regex {
        if (labels.isEmpty()) return NEVER_MATCH
        val alternatives = labels.joinToString("|") { Regex.escape(it) }
        return Regex(
            "(?:$alternatives)[：:\\s#-]*([A-Z0-9][A-Z0-9-]{${minimumLength - 1},${maximumLength - 1}})",
            RegexOption.IGNORE_CASE
        )
    }

    private fun identityKey(parcel: ParsedParcel): String = when {
        !parcel.trackingNumber.isNullOrBlank() -> "tracking:${parcel.trackingNumber.uppercase()}"
        !parcel.orderReference.isNullOrBlank() -> "order:${parcel.sourcePackage}:${parcel.orderReference.uppercase()}"
        else -> "pickup:${parcel.sourcePackage}:${parcel.pickupCode.orEmpty().uppercase()}"
    }

    private fun addCandidate(target: LinkedHashMap<String, ParsedParcel>, candidate: ParsedParcel) {
        val orderKey = candidate.orderReference?.let {
            "order:${candidate.sourcePackage}:${it.uppercase()}"
        }
        if (candidate.trackingNumber != null && orderKey != null) target.remove(orderKey)
        if (candidate.trackingNumber == null && candidate.orderReference != null) {
            val richerExists = target.values.any {
                it.sourcePackage == candidate.sourcePackage &&
                    it.orderReference.equals(candidate.orderReference, ignoreCase = true) &&
                    it.trackingNumber != null
            }
            if (richerExists) return
        }

        val key = identityKey(candidate)
        val existing = target[key]
        target[key] = if (existing == null) candidate else mergeCandidate(existing, candidate)
    }

    private fun mergeCandidate(first: ParsedParcel, second: ParsedParcel): ParsedParcel {
        val preferred = if (second.confidence > first.confidence) second else first
        val other = if (preferred === first) second else first
        val status = when {
            first.status == ParcelStatus.READY_FOR_PICKUP || second.status == ParcelStatus.READY_FOR_PICKUP -> ParcelStatus.READY_FOR_PICKUP
            first.status == ParcelStatus.EXCEPTION || second.status == ParcelStatus.EXCEPTION -> ParcelStatus.EXCEPTION
            else -> maxOf(first.status, second.status, compareBy(ParcelStatus::progressRank))
        }
        return preferred.copy(
            title = if (preferred.title.endsWith(" 包裹") && !other.title.endsWith(" 包裹")) other.title else preferred.title,
            orderReference = preferred.orderReference ?: other.orderReference,
            trackingNumber = preferred.trackingNumber ?: other.trackingNumber,
            pickupCode = preferred.pickupCode ?: other.pickupCode,
            status = status,
            confidence = maxOf(first.confidence, second.confidence)
        )
    }

    private fun normalize(values: List<String>): List<String> = values.asSequence()
        .flatMap { it.lineSequence() }
        .map { it.trim().replace(Regex("\\s+"), " ") }
        // Preserve repeated order/tracking labels: on split-shipment pages they
        // mark the boundary between cards that can share the same order number.
        .filter { it.isNotBlank() }.take(MAX_LINES).toList()

    private fun sanitizeTitle(value: String): String? {
        if (containsSensitiveText(value) || cardLike.containsMatchIn(value)) return null
        val withoutPhone = internationalPhoneLike.replace(phoneLike.replace(value, ""), "")
            .substringBefore("收货地址")
        val addressIndex = withoutPhone.indexOf("address", ignoreCase = true)
        val withoutAddress = if (addressIndex >= 0) withoutPhone.substring(0, addressIndex) else withoutPhone
        val sanitized = emailLike.replace(withoutAddress, "").trim(' ', '：', ':', '-', '|').take(80)
        return sanitized.takeIf { it.length >= 3 }
    }

    private fun containsSensitiveText(value: String): Boolean =
        sensitiveWords.any { value.contains(it, ignoreCase = true) } ||
            emailLike.containsMatchIn(value) || cardLike.containsMatchIn(value)

    private fun containsAny(content: String, vararg words: String): Boolean =
        words.any { content.contains(it, ignoreCase = true) }

    private const val MIN_CONFIDENCE = 38
    private const val SEGMENT_CONTEXT = 7
    private const val MAX_LINES = 260
    private const val MAX_BLOCK_LINES = 100
    private const val MAX_TEXT_LENGTH = 16_000
    private val NEVER_MATCH = Regex("(?!)")
}
