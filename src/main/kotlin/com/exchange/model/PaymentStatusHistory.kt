package com.exchange.model

import java.time.Instant
import java.util.UUID

public enum class TransactionType {
    REFUND,
    PAYMENT,
}

public enum class TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED
}

data class StatusHistoryEntry(
    var status: TransactionStatus,
    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
)

data class PaymentStatusHistory (
    val id: String = UUID.randomUUID().toString(),
    val transactionType: TransactionType,
    val transactionId: String,
    val transactionHistory: MutableList<StatusHistoryEntry>
)

data class PaymentStatusHistoryResponse (
    val paymentStatusHistory: PaymentStatusHistory,
    val quote: Quote?,
    val currentStatus: PaymentStatus
)