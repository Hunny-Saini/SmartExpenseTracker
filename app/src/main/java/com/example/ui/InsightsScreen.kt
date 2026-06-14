package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.TransactionUiState
import com.example.ui.theme.*
import com.example.viewmodel.InsightsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit,
    viewModel: InsightsViewModel = viewModel()
) {
    val timeframe by viewModel.timeframe.collectAsState()
    val statePair by viewModel.insightsUiState.collectAsState()

    val (uiState, insightsData) = statePair

    val timeframes = listOf("Weekly", "Monthly", "Yearly")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights") },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = timeframes.indexOf(timeframe),
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (timeframes.indexOf(timeframe) < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[timeframes.indexOf(timeframe)]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                timeframes.forEachIndexed { index, title ->
                    Tab(
                        selected = timeframe == title,
                        onClick = { viewModel.setTimeframe(title) },
                        text = { Text(title, color = if (timeframe == title) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                when (uiState) {
                    is TransactionUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                    }
                    is TransactionUiState.Error -> {
                        Text("Error loading data", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                    }
                    is TransactionUiState.Success -> {
                        if (insightsData.categoryTotals.isEmpty()) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No expenses found", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
                                Text("Add some transactions to see insights.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                item {
                                    SpentPerDayCard(insightsData.spentPerDay, insightsData.currency)
                                }
                                item {
                                    PieChartCard(insightsData.categoryTotals, insightsData.currency)
                                }
                                item {
                                    BarChartCard(insightsData.dailyTotals, insightsData.currency, timeframe)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieChartCard(categoryTotals: Map<String, Double>, currencyCode: String) {
    val total = categoryTotals.values.sum()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    val colors = listOf(
        if (isSystemDark) Color(0xFF4FA0F6) else Color(0xFF1B81F5), // Blue
        if (isSystemDark) Color(0xFFFF8582) else Color(0xFFFF6D6A), // Pink
        if (isSystemDark) Color(0xFF36D691) else Color(0xFF26C281), // Green
        if (isSystemDark) Color(0xFFFBA438) else Color(0xFFFBA438), // Orange
        if (isSystemDark) Color(0xFFA374F6) else Color(0xFF8B5AEE)  // Purple
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isSystemDark) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Expenses by Category \n(Current Period)", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    var colorIndex = 0
                    
                    if (total > 0) {
                        categoryTotals.toList().sortedByDescending { it.second }.forEach { (_, amount) ->
                            val sweepAngle = ((amount / total) * 360f).toFloat()
                            val sliceColor = colors[colorIndex % colors.size]
                            
                            drawArc(
                                color = sliceColor,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round),
                                size = Size(size.width, size.height)
                            )
                            startAngle += sweepAngle
                            colorIndex++
                        }
                    } else {
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round),
                            size = Size(size.width, size.height)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Scope", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text(
                        text = format.format(total),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.offset(y = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                var colorIndex = 0
                categoryTotals.toList().sortedByDescending { it.second }.forEach { (cat, amount) ->
                    val c = colors[colorIndex % colors.size]
                    val percentage = if (total > 0) (amount / total) * 100 else 0.0
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(c))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(cat, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(" (${"%.0f".format(percentage)}%)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f), fontSize = 12.sp)
                        }
                        Text(format.format(amount), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    }
                    colorIndex++
                }
            }
        }
    }
}

@Composable
fun SpentPerDayCard(spentPerDay: Double, currencyCode: String) {
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isSystemDark) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Average Spent / Day", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(format.format(spentPerDay), color = MaterialTheme.colorScheme.onPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary.copy(alpha=0.5f), modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun BarChartCard(dailyTotals: Map<String, Double>, currencyCode: String, timeframe: String) {
    val maxTotal = dailyTotals.values.maxOrNull() ?: 1.0
    // Sort logic to make graph chronological and sensible
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    
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
    
    val dateSortedKeys = dailyTotals.keys.sortedWith { a, b ->
        try {
            val formatStr = if (timeframe == "Yearly") "MMM yy" else "dd/MM"
            val sdf = java.text.SimpleDateFormat(formatStr, java.util.Locale.getDefault())
            val dateA = sdf.parse(a)
            val dateB = sdf.parse(b)
            dateA?.compareTo(dateB) ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    val recordsList = dateSortedKeys.map { it to (dailyTotals[it] ?: 0.0) }.takeLast(7)

    Card(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isSystemDark) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Spends Timeline ($timeframe)", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("Tracks spending across the selected period", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(24.dp))
            
            if (recordsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions in this period", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    recordsList.forEach { (date, amount) ->
                        val barHeightFraction = (amount / maxTotal).toFloat()
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight().weight(1f)) {
                            // Show amount at top
                            if (amount > 0) {
                                Text(
                                    text = if (amount >= 1000) "${"%.1f".format(amount/1000)}k" else "%.0f".format(amount), 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(barHeightFraction.coerceAtLeast(0.05f))
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}


