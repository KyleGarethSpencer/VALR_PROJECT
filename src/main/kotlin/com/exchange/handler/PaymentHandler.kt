package com.exchange.handler

import com.exchange.model.ApiResponse
import com.exchange.model.Payment
import com.exchange.model.PaymentStatus
import com.exchange.model.PaymentStatusHistory
import com.exchange.model.PaymentStatusHistoryResponse
import com.exchange.model.StatusHistoryEntry
import com.exchange.model.TransactionStatus
import com.exchange.model.TransactionType
import com.exchange.repository.PaymentHistoryRepository
import com.exchange.repository.PaymentRepository
import com.exchange.validation.PaymentValidator
import com.exchange.validation.RequestValidator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.time.Instant

class PaymentHandler(
    private val repository: PaymentRepository,
    private val paymentHistoryRepository: PaymentHistoryRepository,
    private val validator: PaymentValidator
) {

    private val logger = LoggerFactory.getLogger(PaymentHandler::class.java)
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun createPayment(ctx: RoutingContext) {

        try {
            val requestBody = ctx.body()

            val body = requestBody.asJsonObject() ?: run {
                ctx.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Invalid JSON body")))
                return
            }

            val errors = RequestValidator.validatePaymentRequest(body)

            if (errors.isNotEmpty()) {
                ctx.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>(errors.first())))
                return
            }

            val quoteId = body.getString("quoteId")
            val customerReference = body.getString("customerReference")

            val quote = repository.findQuote(quoteId)
            if (quote == null) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Quote not found")))
                return
            }

            val validationErrors = validator.validate(quoteId, customerReference)
            if (validationErrors.isNotEmpty()) {
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>(validationErrors.joinToString(", "))))
                return
            }

            val payment = Payment(
                quoteId = quoteId,
                customerReference = customerReference,
                status = PaymentStatus.PENDING,
                )

            val statusHistoryList: MutableList<StatusHistoryEntry> = mutableListOf<StatusHistoryEntry>();

            val statusHistoryEntry = StatusHistoryEntry(
                status = TransactionStatus.PENDING,
            )

             statusHistoryList.add(statusHistoryEntry);

            repository.savePayment(payment)

            val paymentHistory = PaymentStatusHistory(
                transactionType = TransactionType.PAYMENT,
                transactionId = payment.id,
                transactionHistory = statusHistoryList
            )

            paymentHistoryRepository.savePaymentHistory(paymentHistory)

            logger.info("Created payment {} for quote {}", payment.id, quoteId)

            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.ok(payment)))

        } catch (e: Exception) {
            logger.error("Failed to create payment", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Failed to create payment")))
        }
    }

    fun executePayment(ctx: RoutingContext) {

        var payment: Payment? = null

        try {

            val paymentId = ctx.pathParam("id") ?: run {
                ctx.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Invalid JSON body")))
                return
            }

            payment = repository.findPayment(paymentId)
            if (payment == null) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Payment not found")))
                return
            }

            if (payment.status != PaymentStatus.PENDING) {
                ctx.response()
                    .setStatusCode(409)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Payment is not in PENDING status")))
                return
            }

            payment.status = PaymentStatus.PROCESSING
            payment.updatedAt = Instant.now()
            repository.updatePayment(payment)

            val statusHistoryEntryProcessing = StatusHistoryEntry(
                status = TransactionStatus.PROCESSING,
            )

            val paymentHistory: PaymentStatusHistory? = paymentHistoryRepository.findPaymentHistory(paymentId);

            paymentHistory?.transactionHistory?.add(statusHistoryEntryProcessing);

            // Simulate async processing — in real life this would be an async operation
            payment.status = PaymentStatus.COMPLETED
            payment.updatedAt = Instant.now()
            repository.updatePayment(payment)

            val statusHistoryEntryCompleted = StatusHistoryEntry(
                status = TransactionStatus.COMPLETED,
            )

            paymentHistory?.transactionHistory?.add(statusHistoryEntryCompleted)

            paymentHistoryRepository.updatePaymentHistory(paymentHistory!!)

            logger.info("Executed payment {}", paymentId)

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.ok(payment)))

        } catch (e: Exception) {
            logger.error("Failed to execute payment", e)

            payment?.status = PaymentStatus.FAILED

            repository.updatePayment(payment!!)

            val statusHistoryEntryFailed = StatusHistoryEntry(
                status = TransactionStatus.FAILED,
            )

            val paymentHistory: PaymentStatusHistory? = paymentHistoryRepository.findPaymentHistory(payment.id);
            paymentHistory?.transactionHistory?.add(statusHistoryEntryFailed)

            paymentHistoryRepository.updatePaymentHistory(paymentHistory!!)

            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Failed to execute payment")))
        }
    }

    fun getPaymentStatusHistory(ctx: RoutingContext) {
        try {
            val paymentId = ctx.pathParam("id")

            if(paymentId.isNullOrBlank()) {
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("No PaymentId provided")))
                return
            }

            val payment = repository.findPayment(paymentId)
            if (payment == null) {
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Payment not found")))
                return
            }

            val paymentStatusHistory = paymentHistoryRepository.findPaymentHistory(paymentId)

            if(paymentStatusHistory == null) {
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("No PaymentHistory found")))
                return
            }

            val paymentHistoryStatusResponse = PaymentStatusHistoryResponse(
                paymentStatusHistory = paymentStatusHistory as PaymentStatusHistory,
                quote = repository.findQuote(payment.quoteId),
                currentStatus = payment.status
            )

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.ok(paymentHistoryStatusResponse)))

        } catch (e: Exception) {
            logger.error("Failed to get Payment history", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Failed to get Payment History")))
        }
    }

        fun getPayment(ctx: RoutingContext) {
        try {
            val paymentId = ctx.pathParam("id")

            val payment = repository.findPayment(paymentId!!)
            if (payment == null) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Payment not found")))
                return
            }

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.ok(payment)))

        } catch (e: Exception) {
            logger.error("Failed to get payment", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Failed to get payment")))
        }
    }
}
