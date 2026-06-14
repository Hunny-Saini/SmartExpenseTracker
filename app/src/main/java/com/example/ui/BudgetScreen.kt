package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import com.example.viewmodel.BudgetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetPlanningScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetViewModel = viewModel()
) {
    val limits by viewModel.limits.collectAsState()
    val spentTotals by viewModel.spentTotals.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Food") }
    var newLimitStr by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Planning") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Monthly Limits", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Set caps on category spending", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
            }

            categories.forEach { cat ->
                item {
                    val limit = limits.limits[cat] ?: 0.0
                    val spent = spentTotals[cat] ?: 0.0
                    val progress = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0).toFloat() else 0f
                    val isExceeded = spent > limit && limit > 0

                    BudgetRowCard(
                        category = cat,
                        limit = limit,
                        spent = spent,
                        progress = progress,
                        isExceeded = isExceeded,
                        currencyCode = currencyCode,
                        onEditClick = {
                            selectedCategory = cat
                            newLimitStr = if (limit > 0) limit.toString() else ""
                            showDialog = true
                        }
                    )
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Set $selectedCategory Limit") },
                text = {
                    OutlinedTextField(
                        value = newLimitStr,
                        onValueChange = { newLimitStr = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val amount = newLimitStr.toDoubleOrNull()
                        if (amount != null) {
                            viewModel.updateLimit(selectedCategory, amount)
                            showDialog = false
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun BudgetRowCard(category: String, limit: Double, spent: Double, progress: Float, isExceeded: Boolean, currencyCode: String, onEditClick: () -> Unit) {
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "progress"
    )

    val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.getDefault())
    try {
        val isoCode = when (currencyCode) {
             "₹" -> "INR"
             "$" -> "USD"
             "€" -> "EUR"
             "£" -> "GBP"
             else -> if (currencyCode.length == 3) currencyCode else "USD"
        }
        format.currency = java.util.Currency.getInstance(isoCode)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onEditClick
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(category, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (limit > 0) {
                    Text(format.format(limit), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 14.sp)
                } else {
                    Text("Set Limit", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val barColor = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Spent: ${format.format(spent)}", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp)
                if (isExceeded) {
                    Text("Exceeded by ${format.format(spent - limit)}", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                } else if (limit > 0) {
                    Text("Remaining: ${format.format(limit - spent)}", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }
    }
}
