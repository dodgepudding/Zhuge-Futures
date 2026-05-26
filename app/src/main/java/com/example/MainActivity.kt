package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.model.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FuturesViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FuturesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        FuturesDashboard(viewModel = viewModel, onNavigateToChart = { symbol -> 
                            navController.navigate("chart/$symbol") 
                        })
                    }
                    composable("chart/{symbol}") { backStackEntry ->
                        val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        val asset = state.allAssets.find { it.symbol == symbol }
                        if (asset != null) {
                            ChartScreen(
                                asset = asset,
                                onBack = { navController.popBackStack() },
                                onTrade = { type, qty, leverage ->
                                    viewModel.addPosition(asset.symbol, type, asset.currentPrice, qty, asset.pointValue, leverage)
                                }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("未找到合约")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuturesDashboard(viewModel: FuturesViewModel, onNavigateToChart: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddAlertSheet by remember { mutableStateOf(false) }

    if (showAddAlertSheet) {
        AddAlertDialog(
            assets = state.watchlistAssets,
            onDismiss = { showAddAlertSheet = false },
            onAdd = { symbol, type, price ->
                viewModel.addAlert(symbol, type, price)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("诸葛期指", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddAlertSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加监控")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索合约代码 (如 GC, NQ)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "清除")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }

            if (state.searchResults.isNotEmpty()) {
                item { SectionHeader("搜索结果") }
                items(state.searchResults, key = { "search_" + it.symbol }) { asset ->
                    SearchResultCard(asset, onAdd = { viewModel.addToWatchlist(asset.symbol) })
                }
            } else {
                item {
                    SectionHeader("自选市场行情")
                }
                items(state.watchlistAssets, key = { it.symbol }) { asset ->
                    MarketCard(
                        asset = asset, 
                        onClick = { onNavigateToChart(asset.symbol) },
                        onRemove = { viewModel.removeFromWatchlist(asset.symbol) }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    SectionHeader("我的持仓")
                }
                if (state.positions.isEmpty()) {
                    item { Text("暂无持仓。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(state.positions, key = { it.id }) { position ->
                        val asset = state.allAssets.find { it.symbol == position.symbol }
                        val currentPrice = asset?.currentPrice ?: position.entryPrice
                        PositionCard(position, currentPrice, onClose = { viewModel.removePosition(position.id) })
                    }
                }
                
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    SectionHeader("当前监控")
                }
                if (state.alerts.isEmpty()) {
                    item { Text("暂无监控警报。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(state.alerts, key = { it.id }) { alert ->
                        AlertCard(alert, onRemove = { viewModel.removeAlert(it) })
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (state.triggeredAlertMessage != null) {
        AlertDialogAdvice(
            message = state.triggeredAlertMessage!!,
            aiAdvice = state.aiAdvice,
            isLoading = state.isLoadingAdvice,
            onDismiss = { viewModel.dismissAlert() }
        )
    }
}

@Composable
fun SearchResultCard(asset: FuturesAsset, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onAdd() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = asset.symbol, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(text = asset.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Add, contentDescription = "添加", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketCard(asset: FuturesAsset, onClick: () -> Unit, onRemove: () -> Unit) {
    val df = remember { java.text.DecimalFormat("#,##0.00") }
    val isUp = asset.currentPrice >= asset.previousClose
    val color = if (isUp) Color(0xFF4CAF50) else Color(0xFFE53935)
    val diff = asset.currentPrice - asset.previousClose
    val pct = (diff / asset.previousClose) * 100

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = asset.symbol, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(text = asset.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = df.format(asset.currentPrice), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${if (isUp) "+" else ""}${df.format(diff)} (${df.format(pct)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
        }
    }
}

@Composable
fun PositionCard(position: Position, currentPrice: Double, onClose: () -> Unit) {
    val df = remember { java.text.DecimalFormat("#,##0.00") }
    val pnl = position.calculateUnrealizedPnL(currentPrice)
    val pnlColor = if (pnl >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${if (position.positionType == PositionType.LONG) "多单" else "空单"} ${position.quantity}手 ${position.symbol}",
                    fontWeight = FontWeight.Bold,
                    color = if (position.positionType == PositionType.LONG) Color(0xFF1976D2) else Color(0xFFE53935)
                )
                Text(
                    text = "${if (pnl >= 0) "+" else ""}$${df.format(pnl)}",
                    fontWeight = FontWeight.Bold,
                    color = pnlColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "开仓： ${df.format(position.entryPrice)}")
                Text(text = "现价： ${df.format(currentPrice)}")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "杠杆: ${position.leverage}x", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onClose, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Text("平仓", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun AlertCard(alert: AlertCondition, onRemove: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val symbol = alert.symbol
                val conditionString = if (alert.conditionType == ConditionType.PRICE_ABOVE) ">" else "<"
                Text(text = "$symbol $conditionString ${alert.targetPrice}", fontWeight = FontWeight.SemiBold)
                if (alert.isTriggered) {
                     Text(text = "已触发", color = Color(0xFFE53935), style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = { onRemove(alert.id) }) {
                Icon(Icons.Default.Close, contentDescription = "移除监控")
            }
        }
    }
}

@Composable
fun AlertDialogAdvice(
    message: String,
    aiAdvice: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "触发警报！", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "AI 交易建议", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (aiAdvice != null) {
                    Text(text = aiAdvice, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(text = "加载建议失败。")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("关闭")
                }
            }
        }
    }
}

