package com.exchange.handler

import com.exchange.client.ValrClient
import com.exchange.model.ApiResponse
import com.exchange.model.Quote
import com.exchange.model.RoundingScale
import com.exchange.repository.PaymentRepository
import com.exchange.validation.RequestValidator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode

class QuoteHandler(
    private val valrClient: ValrClient,
    private val repository: PaymentRepository
) {

    private val logger = LoggerFactory.getLogger(QuoteHandler::class.java)
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private val brokerageFeePercent = 0.015 // 1.5% fee

    fun createQuote(ctx: RoutingContext) {
        try {

            val requestBody = ctx.body()

            val body = requestBody.asJsonObject() ?: run {
                ctx.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Invalid JSON body")))
                return
            }

            val errors = RequestValidator.validateQuoteRequest(body)

            if (errors.isNotEmpty()) {
                ctx.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>(errors.first())))
                return
            }

            val currencyPair = body.getString("currencyPair")
            val payAmount = BigDecimal(body.getString("payAmount"))
            val side = body.getString("side")

            val marketPrice = valrClient.getMarketPrice(currencyPair)

            val fee = BigDecimal(payAmount.toDouble() * brokerageFeePercent)
            val netAmount = payAmount.subtract(fee)
            val receiveAmount = netAmount.divide(marketPrice,
                RoundingScale.getPrecisionFor(currencyPair)!!, RoundingMode.DOWN)

            val quote = Quote(
                currencyPair = currencyPair,
                price = marketPrice,
                payAmount = payAmount,
                receiveAmount = receiveAmount,
                fee = fee,
                side = side
            )

            repository.saveQuote(quote)

            logger.info("Created quote {} for {} {}", quote.id, currencyPair, payAmount)

            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.ok(quote)))

        } catch (e: Exception) {
            logger.error("Failed to create quote", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Failed to create quote")))
        }
    }
}
