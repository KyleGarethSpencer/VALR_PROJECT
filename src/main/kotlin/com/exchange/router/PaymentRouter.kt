package com.exchange.router

import com.exchange.handler.QuoteHandler
import com.exchange.handler.PaymentHandler
import com.exchange.handler.RefundHandler
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

object PaymentRouter {

    fun create(vertx: Vertx, quoteHandler: QuoteHandler, paymentHandler: PaymentHandler, refundHandler: RefundHandler): Router {
        val router = Router.router(vertx)

        router.route().handler(BodyHandler.create())

        router.post("/quotes").handler { ctx -> quoteHandler.createQuote(ctx) }

        router.post("/payments").handler { ctx -> paymentHandler.createPayment(ctx) }
        router.post("/payments/:id/execute").handler { ctx -> paymentHandler.executePayment(ctx) }
        router.get("/payments/:id").handler { ctx -> paymentHandler.getPayment(ctx) }

        router.post("/payments/:id/:customerReference/refund").handler { ctx -> refundHandler.executeRefund(ctx) }

        router.post("/payments/:id/execute").handler { ctx -> paymentHandler.executePayment(ctx) }
        router.post("/payments/:id/status").handler { ctx -> paymentHandler.getPaymentStatusHistory(ctx) }


        return router
    }
}
