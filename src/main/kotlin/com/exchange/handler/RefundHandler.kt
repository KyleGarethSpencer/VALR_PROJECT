package com.exchange.handler

import com.exchange.model.ApiResponse
import com.exchange.model.PaymentStatus
import com.exchange.model.PaymentStatusHistory
import com.exchange.model.Refund
import com.exchange.model.RefundStatus
import com.exchange.model.StatusHistoryEntry
import com.exchange.model.TransactionStatus
import com.exchange.repository.PaymentHistoryRepository
import com.exchange.repository.PaymentRepository
import com.exchange.validation.PaymentValidator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant

class RefundHandler(    private val repository: PaymentRepository,
                        private val paymentHistoryRepository: PaymentHistoryRepository,
                        private val validator: PaymentValidator) {

    private val logger = LoggerFactory.getLogger(PaymentHandler::class.java)
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun executeRefund(ctx: RoutingContext) {

        var refund: Refund? = null

        try {

            val paymentId = ctx.pathParam("id") ?: run {
                ctx.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Invalid request, no paymentId provided")))
                return
            }

            val customerReference = ctx.pathParam("customerReference") ?: run {
                ctx.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Invalid request, no customerReference provided")))
                return
            }

            val payment = repository.findPayment(paymentId)
            if (payment == null) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Payment not found")))
                return
            }

            if (payment.status == PaymentStatus.REFUNDED) {
                ctx.response()
                    .setStatusCode(409)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Payment is already REFUNDED")))
                return
            }

            if (payment.status != PaymentStatus.COMPLETED) {
                ctx.response()
                    .setStatusCode(409)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Payment is not in COMPLETED status")))
                return
            }

             refund = Refund(
                paymentId = paymentId,
                customerReference = customerReference,
                refundAmount = repository.findQuote(payment.quoteId)?.payAmount ?: BigDecimal.ZERO,
            )

            refund.status = RefundStatus.PROCESSING
            refund.updatedAt = Instant.now()
            repository.updateRefund(refund)

            // Simulate async processing — in real life this would be an async operation
            refund.status = RefundStatus.COMPLETED
            refund.updatedAt = Instant.now()
            repository.updateRefund(refund)

            payment.status = PaymentStatus.REFUNDED
            repository.updatePayment(payment)

            val statusHistoryEntryProcessing = StatusHistoryEntry(
                status = TransactionStatus.REFUNDED,
            )

            val paymentHistory: PaymentStatusHistory? = paymentHistoryRepository.findPaymentHistory(paymentId);

            paymentHistory?.transactionHistory?.add(statusHistoryEntryProcessing);

            paymentHistoryRepository.updatePaymentHistory(paymentHistory!!)

            logger.info("Executed refund {}", refund.id)

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.ok(refund)))

        } catch (e: Exception) {
            logger.error("Failed to execute refund", e)

            refund?.status = RefundStatus.FAILED

            repository.updateRefund(refund!!)

            val statusHistoryEntryFailed = StatusHistoryEntry(
                status = TransactionStatus.FAILED,
            )

            val paymentHistory: PaymentStatusHistory? = paymentHistoryRepository.findPaymentHistory(refund.paymentId);
            paymentHistory?.transactionHistory?.add(statusHistoryEntryFailed)

            paymentHistoryRepository.updatePaymentHistory(paymentHistory!!)

            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Failed to execute refund")))
        }
    }
}