package com.parcelinbox.app.data

enum class ParcelStatus(val label: String) {
    PENDING("待发货"),
    SHIPPED("已发货"),
    IN_TRANSIT("运输中"),
    OUT_FOR_DELIVERY("派送中"),
    READY_FOR_PICKUP("待取件"),
    DELIVERED("已送达"),
    COMPLETED("已取件"),
    EXCEPTION("异常")
}

fun ParcelStatus.progressRank(): Int = when (this) {
    ParcelStatus.PENDING -> 0
    ParcelStatus.SHIPPED -> 1
    ParcelStatus.IN_TRANSIT -> 2
    ParcelStatus.OUT_FOR_DELIVERY -> 3
    ParcelStatus.DELIVERED -> 4
    ParcelStatus.READY_FOR_PICKUP -> 5
    ParcelStatus.COMPLETED -> 6
    ParcelStatus.EXCEPTION -> -1
}

fun resolveParcelStatus(existing: ParcelStatus?, incoming: ParcelStatus): ParcelStatus {
    if (existing == null) return incoming
    if (existing == ParcelStatus.COMPLETED) return ParcelStatus.COMPLETED
    if (incoming == ParcelStatus.EXCEPTION) return ParcelStatus.EXCEPTION
    if (existing == ParcelStatus.EXCEPTION) return incoming
    return if (incoming.progressRank() >= existing.progressRank()) incoming else existing
}

enum class CaptureMethod {
    SCREEN,
    NOTIFICATION,
    DEMO
}

data class ParcelItem(
    val id: Long,
    val sourcePackage: String,
    val sourceLabel: String,
    val title: String,
    val orderReference: String?,
    val trackingNumber: String?,
    val pickupCode: String?,
    val status: ParcelStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val archived: Boolean,
    val captureMethod: CaptureMethod
)

data class ParsedParcel(
    val sourcePackage: String,
    val sourceLabel: String,
    val title: String,
    val orderReference: String? = null,
    val trackingNumber: String?,
    val pickupCode: String?,
    val status: ParcelStatus,
    val observedAt: Long,
    val captureMethod: CaptureMethod = CaptureMethod.NOTIFICATION,
    val confidence: Int = 100
)
