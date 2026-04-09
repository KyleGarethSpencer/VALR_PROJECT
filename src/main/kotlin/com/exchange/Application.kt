package com.exchange

import com.exchange.router.PaymentRouter
import com.exchange.handler.QuoteHandler
import com.exchange.handler.PaymentHandler
import com.exchange.client.ValrClient
import com.exchange.handler.RefundHandler
import com.exchange.repository.PaymentHistoryRepository
import com.exchange.repository.PaymentRepository
import com.exchange.validation.PaymentValidator
import com.exchange.validation.RequestValidator
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

fun main() {
    val vertx = Vertx.vertx()

    val valrClient = ValrClient()
    val paymentRepository = PaymentRepository()
    val paymentValidator = PaymentValidator()
    val paymentHistoryRepository = PaymentHistoryRepository()
    val quoteHandler = QuoteHandler(valrClient, paymentRepository)
    val paymentHandler = PaymentHandler(paymentRepository, paymentHistoryRepository, paymentValidator)
    val refundHandler = RefundHandler(paymentRepository, paymentHistoryRepository, paymentValidator)

    val router = PaymentRouter.create(vertx, quoteHandler, paymentHandler, refundHandler)

    vertx.createHttpServer()
        .requestHandler(router)
        .listen(8080)

    logger.info("Server started on port 8080")
}
