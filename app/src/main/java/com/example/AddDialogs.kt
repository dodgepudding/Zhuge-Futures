package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.ConditionType
import com.example.model.PositionType
import com.example.model.FuturesAsset
import com.example.viewmodel.FuturesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlertDialog(
    assets: List<FuturesAsset>,
    onDismiss: () -> Unit,
    onAdd: (String, ConditionType, Double) -> Unit
) {
    if (assets.isEmpty()) return
    
    var selectedSymbol by remember { mutableStateOf(assets.first().symbol) }
    var selectedCondition by remember { mutableStateOf(ConditionType.PRICE_ABOVE) }
    var priceText by remember { mutableStateOf("") }
    var expandedSymbol by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("添加监控条件", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
            
            ExposedDropdownMenuBox(expanded = expandedSymbol, onExpandedChange = { expandedSymbol = !expandedSymbol }) {
                OutlinedTextField(
                    value = selectedSymbol,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("合约代码") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSymbol) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expandedSymbol, onDismissRequest = { expandedSymbol = false }) {
                    assets.forEach { asset ->
                        DropdownMenuItem(
                            text = { Text(asset.symbol) },
                            onClick = {
                                selectedSymbol = asset.symbol
                                expandedSymbol = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedCondition == ConditionType.PRICE_ABOVE,
                        onClick = { selectedCondition = ConditionType.PRICE_ABOVE }
                    )
                    Text("涨破 (>=)")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedCondition == ConditionType.PRICE_BELOW,
                        onClick = { selectedCondition = ConditionType.PRICE_BELOW }
                    )
                    Text("跌破 (<=)")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("目标价") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val price = priceText.toDoubleOrNull()
                    if (price != null) {
                        onAdd(selectedSymbol, selectedCondition, price)
                        onDismiss()
                    }
                }) {
                    Text("保存")
                }
            }
        }
        }
    }
}
