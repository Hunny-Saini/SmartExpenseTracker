package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.TransactionMapObject
import com.example.model.TransactionUiState
import com.example.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import java.util.Calendar

data class InsightsData(
    val categoryTotals: Map<String, Double> = emptyMap(),
    val dailyTotals: Map<String, Double> = emptyMap(),
    val spentPerDay: Double = 0.0,
    val currency: String = "USD"
)

class InsightsViewModel : ViewModel() {
    private val repository = TransactionRepository()
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    private val _timeframe = MutableStateFlow("Weekly") // "Weekly", "Monthly", "Yearly"
    val timeframe: StateFlow<String> = _timeframe

    private val _currency = MutableStateFlow("USD")
    
    init {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _currency.value = snapshot.getString("default_currency") ?: "USD"
                }
            }
        }
    }

    val insightsUiState: StateFlow<Pair<TransactionUiState, InsightsData>> = kotlinx.coroutines.flow.combine(
        repository.getTransactionsStream(), 
        _timeframe,
        _currency
    ) { state, tf, currencyStr ->
        if (state is TransactionUiState.Success) {
            val allTransactions = state.groupedTransactions.flatMap { it.value }
            val filtered = filterTransactionsByTimeframe(allTransactions, tf)
            
            val expensesOnly = filtered.filter { it.category != "Income" }
            
            // Category Totals for Pie Chart
            val categoryTotals = expensesOnly
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            // Daily/Interval Totals for Bar Chart
            val dailyTotals = expensesOnly
                .groupBy { formatTimestampToShortDate(it.timestamp, tf) }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val totalSpent = expensesOnly.sumOf { it.amount }
            val days = when (tf) {
                "Weekly" -> 7
                "Monthly" -> 30
                "Yearly" -> 365
                else -> 1
            }
            val spentPerDay = totalSpent / days

            Pair(state, InsightsData(categoryTotals, dailyTotals, spentPerDay, currencyStr))
        } else {
            Pair(state, InsightsData(currency = currencyStr))
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, Pair(TransactionUiState.Loading, InsightsData()))

    fun setTimeframe(tf: String) {
        _timeframe.value = tf
    }

    private fun filterTransactionsByTimeframe(txs: List<TransactionMapObject>, timeframe: String): List<TransactionMapObject> {
        val cal = Calendar.getInstance()
        when (timeframe) {
            "Weekly" -> cal.add(Calendar.DAY_OF_YEAR, -7)
            "Monthly" -> cal.add(Calendar.DAY_OF_YEAR, -30)
            "Yearly" -> cal.add(Calendar.DAY_OF_YEAR, -365)
        }
        val threshold = cal.timeInMillis
        return txs.filter { it.timestamp >= threshold }
    }

    private fun formatTimestampToShortDate(timestamp: Long, timeframe: String): String {
        val formatStr = when(timeframe) {
            "Yearly" -> "MMM yy"
            else -> "dd/MM"
        }
        val sdf = java.text.SimpleDateFormat(formatStr, java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
