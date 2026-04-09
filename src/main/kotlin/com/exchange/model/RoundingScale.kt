package com.exchange.model

enum class RoundingScale(val precision: Int) {
    //    BTCZAR(8),
    BTCZAR(5), //As per readMe
    ETHZAR(18);

    companion object {
        fun getPrecisionFor(name: String): Int? {
            return RoundingScale.entries.find { it.name == name }?.precision
        }
    }
}
