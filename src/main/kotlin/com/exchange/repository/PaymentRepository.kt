package com.exchange.repository

import com.exchange.model.Payment
import com.exchange.model.Quote
import com.exchange.model.Refund

/**
 * In-memory repository for payments and quotes.
 *
 */
class PaymentRepository {

    private val quotes = HashMap<String, Quote>()
    private val payments = HashMap<String, Payment>()
    private val refunds = HashMap<String, Refund>()

    fun saveQuote(quote: Quote) {
        quotes[quote.id] = quote
    }

    fun findQuote(id: String): Quote? {
        return quotes[id]
    }

    fun savePayment(payment: Payment) {
        payments[payment.id] = payment
    }

    fun findPayment(id: String): Payment? {
        return payments[id]
    }

    fun updatePayment(payment: Payment) {
        payments[payment.id] = payment
    }

    fun findRefund(id: String): Refund? {
        return refunds[id]
    }

    fun updateRefund(refund: Refund) {
        refunds[refund.id] = refund
    }
}
