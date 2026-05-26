package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.FuturesAsset
import com.example.model.PositionType
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(asset: FuturesAsset, onBack: () -> Unit, onTrade: (PositionType, Int, Int) -> Unit) {
    val timeframes = listOf("5m", "15m", "60m", "4h", "Daily")
    var selectedTimeframe by remember { mutableStateOf("Daily") }
    var showTradeSheet by remember { mutableStateOf(false) }
    var tradeType by remember { mutableStateOf(PositionType.LONG) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("${asset.symbol} - ${asset.name}", fontWeight = FontWeight.Bold)
                        val df = remember { java.text.DecimalFormat("#,##0.00") }
                        Text("现价: ${df.format(asset.currentPrice)}", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { tradeType = PositionType.LONG; showTradeSheet = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("买入 (做多)", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Button(
                        onClick = { tradeType = PositionType.SHORT; showTradeSheet = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("卖出 (做空)", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = timeframes.indexOf(selectedTimeframe),
                edgePadding = 16.dp
            ) {
                timeframes.forEach { tf ->
                    Tab(
                        selected = selectedTimeframe == tf,
                        onClick = { selectedTimeframe = tf },
                        text = { Text(tf) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            CandlestickChart(timeframe = selectedTimeframe, modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp))
        }
    }

    if (showTradeSheet) {
        TradeBottomSheet(
            asset = asset,
            initialType = tradeType,
            onDismiss = { showTradeSheet = false },
            onConfirm = { type, qty, lev ->
                onTrade(type, qty, lev)
                showTradeSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeBottomSheet(
    asset: FuturesAsset,
    initialType: PositionType,
    onDismiss: () -> Unit,
    onConfirm: (PositionType, Int, Int) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var qtyText by remember { mutableStateOf("1") }
    var leverageText by remember { mutableStateOf("10") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 16.dp)) {
                Text("交易 ${asset.symbol}", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { type = PositionType.LONG },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (type == PositionType.LONG) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.Transparent,
                        contentColor = if (type == PositionType.LONG) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, if (type == PositionType.LONG) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline)
                ) {
                    Text("做多 (Long)")
                }
                OutlinedButton(
                    onClick = { type = PositionType.SHORT },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (type == PositionType.SHORT) Color(0xFFE53935).copy(alpha = 0.2f) else Color.Transparent,
                        contentColor = if (type == PositionType.SHORT) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, if (type == PositionType.SHORT) Color(0xFFE53935) else MaterialTheme.colorScheme.outline)
                ) {
                    Text("做空 (Short)")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = qtyText,
                onValueChange = { qtyText = it },
                label = { Text("数量 (手)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = leverageText,
                onValueChange = { leverageText = it },
                label = { Text("杠杆倍数 (x)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            val isLong = type == PositionType.LONG
            Button(
                onClick = {
                    val qty = qtyText.toIntOrNull() ?: 1
                    val lev = leverageText.toIntOrNull() ?: 1
                    onConfirm(type, qty, lev)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isLong) Color(0xFF4CAF50) else Color(0xFFE53935))
            ) {
                Text(if (isLong) "确认做多" else "确认做空", color = Color.White)
            }
        }
        }
    }
}

@Composable
fun CandlestickChart(timeframe: String, modifier: Modifier = Modifier) {
    val candles = remember(timeframe) {
        var lastClose = 100f
        List(40) {
            val open = lastClose + (Random.nextFloat() - 0.5f) * 5f
            val close = open + (Random.nextFloat() - 0.5f) * 20f
            val high = maxOf(open, close) + Random.nextFloat() * 10f
            val low = minOf(open, close) - Random.nextFloat() * 10f
            lastClose = close
            Candle(open, close, high, low)
        }
    }

    Canvas(modifier = modifier) {
        val candleWidth = size.width / candles.size
        val maxHigh = candles.maxOf { it.high }
        val minLow = candles.minOf { it.low }
        val range = maxHigh - minLow

        candles.forEachIndexed { index, candle ->
            val x = index * candleWidth + candleWidth / 2
            
            val highY = size.height - ((candle.high - minLow) / range) * size.height
            val lowY = size.height - ((candle.low - minLow) / range) * size.height
            val openY = size.height - ((candle.open - minLow) / range) * size.height
            val closeY = size.height - ((candle.close - minLow) / range) * size.height
            
            val isUp = candle.close >= candle.open
            val color = if (isUp) Color(0xFF4CAF50) else Color(0xFFE53935)
            
            drawLine(
                color = color,
                start = Offset(x, highY),
                end = Offset(x, lowY),
                strokeWidth = 2f
            )
            
            val topY = minOf(openY, closeY)
            val bottomY = maxOf(openY, closeY)
            val rectHeight = maxOf(2f, bottomY - topY)
            
            drawRect(
                color = color,
                topLeft = Offset(x - candleWidth * 0.35f, topY),
                size = androidx.compose.ui.geometry.Size(candleWidth * 0.7f, rectHeight)
            )
        }
    }
}

data class Candle(val open: Float, val close: Float, val high: Float, val low: Float)
