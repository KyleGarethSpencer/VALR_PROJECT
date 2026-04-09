package com.exchange.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class RefundStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
}

data class Refund(
    val id: String = UUID.randomUUID().toString(),
    val paymentId: String,
    val refundAmount: BigDecimal,
    val customerReference: String,
    var status: RefundStatus = RefundStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
)