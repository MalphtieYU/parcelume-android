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

data class ParcelItem(
    val id: Long,
    val sourcePackage: String,
    val sourceLabel: String,
    val title: String,
    val trackingNumber: String?,
    val pickupCode: String?,
    val status: ParcelStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val archived: Boolean
)

data class ParsedParcel(
    val sourcePackage: String,
    val sourceLabel: String,
    val title: String,
    val trackingNumber: String?,
    val pickupCode: String?,
    val status: ParcelStatus,
    val observedAt: Long
)
