package com.exchange.validation

import io.vertx.core.json.JsonObject

class RequestValidator {


    companion object {

        fun validateQuoteRequest(request: JsonObject): List<String> {
            val errors = mutableListOf<String>()


            if (request.getString("currencyPair").isNullOrBlank()) errors.add("currencyPair is required")
            if (request.getString("payAmount").isNullOrBlank()) errors.add("payAmount is required")
            if (request.getString("currencyPair").isNullOrBlank()) errors.add("payCurrency is required")
            if (request.getString("side").isNullOrBlank()) errors.add("side is required")

            return errors
        }

        fun validatePaymentRequest(request: JsonObject): List<String> {
            val errors = mutableListOf<String>()


            if (request.getString("quoteId").isNullOrBlank()) errors.add("quoteId is required")
            if (request.getString("customerReference").isNullOrBlank()) errors.add("customerReference is required")

            return errors
        }
    }
}