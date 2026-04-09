package com.exchange.handler;

import com.exchange.client.ValrClient;
import com.exchange.model.Payment
import com.exchange.model.PaymentStatus
import com.exchange.model.PaymentStatusHistory
import com.exchange.model.Quote
import com.exchange.model.TransactionType
import com.exchange.repository.PaymentHistoryRepository;
import com.exchange.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import com.exchange.router.PaymentRouter
import com.exchange.validation.PaymentValidator
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant
import kotlin.collections.mutableListOf

import org.mockito.kotlin.*

@ExtendWith(VertxExtension::class)
class RefundHandlerTest {
    private lateinit var client: WebClient

    private val repository: PaymentRepository = PaymentRepository()
    private val paymentHistoryRepository: PaymentHistoryRepository = PaymentHistoryRepository()
    private val validator: PaymentValidator = PaymentValidator()

    @BeforeEach
    fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        val valrClient = ValrClient()
        val repository = PaymentRepository()
        val paymentHistoryRepository = PaymentHistoryRepository()
        val validator = PaymentValidator()
        val quoteHandler = QuoteHandler(valrClient, repository)
        val paymentHandler = PaymentHandler(repository, paymentHistoryRepository, validator)
        val refundHandler = RefundHandler(repository, paymentHistoryRepository, validator)

        val router = PaymentRouter.create(
            vertx, quoteHandler, paymentHandler,
            refundHandler
        )

        client = WebClient.create(vertx)

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(0)
            .onComplete(testContext.succeeding { server ->
                client = WebClient.create(vertx)
                testContext.completeNow()
            })
    }

    @Test
    fun `should return 400 for missing currency pair`(vertx: Vertx, testContext: VertxTestContext) {
        // This test intentionally left with a basic structure
        // It connects to a real API so may be flaky — candidate should notice this
        testContext.completeNow()
    }

    @Test
    fun `should create quote for valid request`(vertx: Vertx, testContext: VertxTestContext) {
        // This test connects to VALR API — candidate should consider mocking
        testContext.completeNow()
    }

    @Test
    fun `should execute refund successfully`() {
        val paymentId = "p1"
        val customerRef = "cust1"

        val repository = mock<PaymentRepository>()
        val paymentHistoryRepository = mock<PaymentHistoryRepository>()
        val validator = mock<PaymentValidator>()

        val payment = Payment(
            id = paymentId,
            quoteId = "q1",
            status = PaymentStatus.COMPLETED,
            customerReference = "customer123",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val quote = Quote(
            id = "q1",
            payAmount = BigDecimal("100.00"),
            currencyPair = "BTCZAR",
            price = BigDecimal.valueOf(1191057),
            receiveAmount = BigDecimal.valueOf(0.00082),
            fee = BigDecimal.valueOf(15),
            side = "BUY",
            createdAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(120)
        )

        val routingContext = mock<RoutingContext>()
        val response = mock<HttpServerResponse>()

        whenever(routingContext.pathParam("id")).thenReturn(paymentId)
        whenever(routingContext.pathParam("customerReference")).thenReturn(customerRef)
        whenever(routingContext.response()).thenReturn(response)

        whenever(repository.findPayment(paymentId)).thenReturn(payment)
        whenever(repository.findQuote("q1")).thenReturn(quote)

        val history = PaymentStatusHistory(
            transactionHistory = mutableListOf(),
            transactionType = TransactionType.PAYMENT,
            transactionId = "Q1"
        )

        whenever(paymentHistoryRepository.findPaymentHistory(paymentId))
            .thenReturn(history)

        whenever(response.setStatusCode(any())).thenReturn(response)
        whenever(response.putHeader(org.mockito.kotlin.any<String>(), org.mockito.kotlin.any<String>())).thenReturn(response)

        val handler = RefundHandler(repository, paymentHistoryRepository, validator)

        handler.executeRefund(routingContext)

        verify(repository, times(2)).updateRefund(any())
        verify(repository).updatePayment(any())
        verify(paymentHistoryRepository).updatePaymentHistory(any())

        verify(response).setStatusCode(200)
        verify(response).end(any<String>())
    }

    @Test
    fun `should return 404 when payment not found`() {
        val repository = mock<PaymentRepository>()
        val paymentHistoryRepository = mock<PaymentHistoryRepository>()
        val validator = mock<PaymentValidator>()

        val routingContext = mock<RoutingContext>()
        val response = mock<HttpServerResponse>()

        whenever(routingContext.pathParam("id")).thenReturn("p1")
        whenever(routingContext.pathParam("customerReference")).thenReturn("cust1")
        whenever(routingContext.response()).thenReturn(response)

        whenever(repository.findPayment("p1")).thenReturn(null)

        whenever(response.setStatusCode(any())).thenReturn(response)
        whenever(response.putHeader(anyString(), anyString())).thenReturn(response)

        val handler = RefundHandler(repository, paymentHistoryRepository, validator)

        handler.executeRefund(routingContext)

        verify(response).setStatusCode(404)
        verify(response).end(anyString())

        verify(repository, never()).updateRefund(any())
    }

    @Test
    fun `should return 500 when exception occurs`() {
        val paymentId = "p1"

        val repository = mock<PaymentRepository>()
        val paymentHistoryRepository = mock<PaymentHistoryRepository>()
        val validator = mock<PaymentValidator>()

        val payment = Payment(
            id = paymentId,
            quoteId = "q1",
            status = PaymentStatus.COMPLETED,
            customerReference = "cust1",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val quote = Quote(
            id = "q1",
            payAmount = BigDecimal("100.00"),
            currencyPair = "BTCZAR",
            price = BigDecimal.valueOf(1191057),
            receiveAmount = BigDecimal.valueOf(0.00082),
            fee = BigDecimal.valueOf(15),
            side = "BUY",
            createdAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(120)
        )

        val history = PaymentStatusHistory(
            transactionHistory = mutableListOf(),
            transactionType = TransactionType.PAYMENT,
            transactionId = paymentId
        )

        val routingContext = mock<RoutingContext>()
        val response = mock<HttpServerResponse>()

        whenever(routingContext.pathParam("id")).thenReturn(paymentId)
        whenever(routingContext.pathParam("customerReference")).thenReturn("cust1")
        whenever(routingContext.response()).thenReturn(response)

        whenever(repository.findPayment(paymentId)).thenReturn(payment)
        whenever(repository.findQuote("q1")).thenReturn(quote)
        whenever(paymentHistoryRepository.findPaymentHistory(paymentId)).thenReturn(history)

        doThrow(RuntimeException("DB error"))
            .doNothing()
            .whenever(repository)
            .updateRefund(any())

        whenever(response.setStatusCode(any())).thenReturn(response)
        whenever(response.putHeader(anyString(), anyString())).thenReturn(response)

        val handler = RefundHandler(repository, paymentHistoryRepository, validator)

        handler.executeRefund(routingContext)

        verify(response).setStatusCode(500)
        verify(response).end(anyString())

        verify(paymentHistoryRepository).updatePaymentHistory(any())
    }
}
