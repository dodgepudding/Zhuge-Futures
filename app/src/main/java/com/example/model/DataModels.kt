package com.example.model

data class FuturesAsset(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val previousClose: Double,
    val tickSize: Double = 0.25,
    val pointValue: Double = 20.0
)

enum class PositionType { LONG, SHORT }

data class Position(
    val id: String,
    val symbol: String,
    val entryPrice: Double,
    val quantity: Int,
    val positionType: PositionType,
    val pointValue: Double = 20.0,
    val leverage: Int = 1
) {
    fun calculateUnrealizedPnL(currentPrice: Double): Double {
        val points = if (positionType == PositionType.LONG) {
            currentPrice - entryPrice
        } else {
            entryPrice - currentPrice
        }
        return points * quantity * pointValue
    }
}

enum class ConditionType { PRICE_ABOVE, PRICE_BELOW }

data class AlertCondition(
    val id: String,
    val symbol: String,
    val conditionType: ConditionType,
    val targetPrice: Double,
    var isTriggered: Boolean = false
)
