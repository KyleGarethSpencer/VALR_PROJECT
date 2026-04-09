package com.exchange.repository

import com.exchange.model.PaymentStatusHistory
import kotlin.collections.set

class PaymentHistoryRepository {

    private val paymentsHistory = HashMap<String, PaymentStatusHistory>()

    fun findPaymentHistory(transactionId: String): PaymentStatusHistory? {
        return paymentsHistory[transactionId]
    }

    fun updatePaymentHistory(paymentHistory: PaymentStatusHistory) {
        paymentsHistory[paymentHistory.transactionId] = paymentHistory
    }

    fun savePaymentHistory(paymentHistory: PaymentStatusHistory) {
        paymentsHistory[paymentHistory.transactionId] = paymentHistory
    }
}