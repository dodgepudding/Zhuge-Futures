package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.network.generateTradingAdvice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

data class MainState(
    val watchlist: List<String> = emptyList(),
    val allAssets: List<FuturesAsset> = emptyList(),
    val positions: List<Position> = emptyList(),
    val alerts: List<AlertCondition> = emptyList(),
    val triggeredAlertMessage: String? = null,
    val aiAdvice: String? = null,
    val isLoadingAdvice: Boolean = false,
    val searchQuery: String = ""
) {
    val watchlistAssets: List<FuturesAsset>
        get() = allAssets.filter { it.symbol in watchlist }

    val searchResults: List<FuturesAsset>
        get() = if (searchQuery.isBlank()) emptyList() 
                else allAssets.filter { it.symbol.contains(searchQuery, ignoreCase = true) || it.name.contains(searchQuery, ignoreCase = true) }
}

class FuturesViewModel : ViewModel() {
    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        // Broad range of mock assets
        val initialAllAssets = listOf(
            FuturesAsset("NQ1!", "纳斯达克100", 18500.0, 18450.0, 0.25, 20.0),
            FuturesAsset("ES1!", "标普500", 5300.0, 5280.0, 0.25, 50.0),
            FuturesAsset("YM1!", "道琼斯", 39800.0, 39600.0, 1.0, 5.0),
            FuturesAsset("RTY1!", "罗素2000", 2100.0, 2080.0, 0.1, 50.0),
            FuturesAsset("CL1!", "原油", 82.50, 81.20, 0.01, 1000.0),
            FuturesAsset("GC1!", "黄金", 2350.10, 2340.50, 0.1, 100.0),
            FuturesAsset("SI1!", "白银", 30.50, 30.10, 0.005, 5000.0),
            FuturesAsset("HG1!", "铜", 4.80, 4.75, 0.0005, 25000.0),
            FuturesAsset("IF2406", "沪深300", 3600.0, 3580.0, 0.2, 300.0),
            FuturesAsset("IC2406", "中证500", 5200.0, 5150.0, 0.2, 200.0),
            FuturesAsset("IM2406", "中证1000", 5400.0, 5450.0, 0.2, 200.0),
            FuturesAsset("IH2406", "上证50", 2400.0, 2380.0, 0.2, 300.0),
            FuturesAsset("HSI", "恒生指数", 18000.0, 18100.0, 1.0, 50.0)
        )
        
        val initialWatchlist = listOf("NQ1!", "CL1!", "GC1!", "IF2406")

        // Initial mock positions and alerts
        val initialPositions = listOf(
            Position(UUID.randomUUID().toString(), "NQ1!", 18480.0, 1, PositionType.LONG, 20.0, 10),
            Position(UUID.randomUUID().toString(), "IF2406", 3650.0, 2, PositionType.SHORT, 300.0, 5)
        )
        val initialAlerts = listOf(
            AlertCondition(UUID.randomUUID().toString(), "NQ1!", ConditionType.PRICE_ABOVE, 18510.0),
            AlertCondition(UUID.randomUUID().toString(), "CL1!", ConditionType.PRICE_BELOW, 82.00),
            AlertCondition(UUID.randomUUID().toString(), "IF2406", ConditionType.PRICE_BELOW, 3500.0)
        )

        _state.update {
            it.copy(
                allAssets = initialAllAssets,
                watchlist = initialWatchlist,
                positions = initialPositions,
                alerts = initialAlerts
            )
        }

        startPriceSimulation()
    }

    private fun startPriceSimulation() {
        viewModelScope.launch {
            while (true) {
                delay(1500) // update every 1.5s
                
                _state.update { currentState ->
                    val updatedAssets = currentState.allAssets.map { asset ->
                        // random tick simulation
                        val volatility = asset.currentPrice * 0.0002
                        val change = (Random.nextDouble(-volatility, volatility) / asset.tickSize).toInt() * asset.tickSize
                        asset.copy(currentPrice = asset.currentPrice + change)
                    }
                    
                    currentState.copy(allAssets = updatedAssets)
                }
                
                checkAlerts()
            }
        }
    }

    private fun checkAlerts() {
        val currentState = _state.value
        val (alerts) = currentState
        
        var alertWasTriggered = false
        val newAlerts = currentState.alerts.map { alert ->
            if (alert.isTriggered) return@map alert // already triggered
            
            val asset = currentState.allAssets.find { it.symbol == alert.symbol } ?: return@map alert
            
            val triggersAbove = alert.conditionType == ConditionType.PRICE_ABOVE && asset.currentPrice >= alert.targetPrice
            val triggersBelow = alert.conditionType == ConditionType.PRICE_BELOW && asset.currentPrice <= alert.targetPrice
            
            if (triggersAbove || triggersBelow) {
                alertWasTriggered = true
                val directionText = if (alert.conditionType == ConditionType.PRICE_ABOVE) "涨破" else "跌破"
                val position = currentState.positions.find { it.symbol == alert.symbol }
                val positionText = position?.let { " 您当前持有 ${it.quantity} 手${if (it.positionType == PositionType.LONG) "多单" else "空单"}，开仓价 ${it.entryPrice}。" } ?: " 您当前无持仓。"
                
                val message = "${alert.symbol} 已经 $directionText ${alert.targetPrice} (现价: ${String.format("%.2f", asset.currentPrice)})。$positionText"
                triggerAiAdvice(message)
                
                alert.copy(isTriggered = true)
            } else {
                alert
            }
        }
        
        if (alertWasTriggered) {
             _state.update { it.copy(alerts = newAlerts) }
        }
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun addToWatchlist(symbol: String) {
        _state.update {
            if (it.watchlist.contains(symbol)) it else it.copy(watchlist = it.watchlist + symbol, searchQuery = "")
        }
    }

    fun removeFromWatchlist(symbol: String) {
        _state.update {
            it.copy(watchlist = it.watchlist.filter { s -> s != symbol })
        }
    }

    private fun triggerAiAdvice(conditionMessage: String) {
        if (_state.value.isLoadingAdvice) return // Avoid overloading
        
        _state.update { it.copy(
            triggeredAlertMessage = conditionMessage,
            isLoadingAdvice = true,
            aiAdvice = null
        ) }
        
        viewModelScope.launch {
            val advice = generateTradingAdvice(conditionMessage)
            _state.update {
                it.copy(
                    isLoadingAdvice = false,
                    aiAdvice = advice
                )
            }
        }
    }

    fun dismissAlert() {
        _state.update { it.copy(
            triggeredAlertMessage = null,
            aiAdvice = null,
            isLoadingAdvice = false
        ) }
    }
    
    fun addAlert(symbol: String, type: ConditionType, price: Double) {
        val newAlert = AlertCondition(UUID.randomUUID().toString(), symbol, type, price)
        _state.update { it.copy(alerts = it.alerts + newAlert) }
    }
    
    fun removeAlert(id: String) {
        _state.update { it.copy(alerts = it.alerts.filter { it.id != id }) }
    }
    
    fun addPosition(symbol: String, type: PositionType, entry: Double, qty: Int, pointValue: Double = 20.0, leverage: Int = 1) {
        val newPos = Position(UUID.randomUUID().toString(), symbol, entry, qty, type, pointValue, leverage)
        _state.update { it.copy(positions = it.positions + newPos) }
    }
    
    fun removePosition(id: String) {
        _state.update { it.copy(positions = it.positions.filter { it.id != id }) }
    }
}
