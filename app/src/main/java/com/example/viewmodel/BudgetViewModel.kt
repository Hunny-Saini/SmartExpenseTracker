package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.TransactionUiState
import com.example.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

data class BudgetLimits(
    val limits: Map<String, Double> = emptyMap()
)

class BudgetViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val repository = TransactionRepository()

    private val _limits = MutableStateFlow(BudgetLimits())
    val limits: StateFlow<BudgetLimits> = _limits

    private val _currencyCode = MutableStateFlow("USD")
    val currencyCode: StateFlow<String> = _currencyCode

    private val _categories = MutableStateFlow(listOf("Food", "Rent", "Shopping", "Transport", "Bills"))
    val categories: StateFlow<List<String>> = _categories

    val spentTotals: StateFlow<Map<String, Double>> = repository.getTransactionsStream().map { state ->
        if (state is TransactionUiState.Success) {
            val allTxs = state.groupedTransactions.flatMap { it.value }
            // Filter current month
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val startOfMonth = cal.timeInMillis
            
            allTxs.filter { it.timestamp >= startOfMonth && it.category != "Income" }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
        } else {
            emptyMap()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    init {
        fetchLimits()
    }

    private fun fetchLimits() {
         val user = auth.currentUser ?: return
         db.collection("users").document(user.uid).addSnapshotListener { snapshot, _ ->
             if (snapshot != null && snapshot.exists()) {
                 val map = snapshot.get("budgets") as? Map<*, *> ?: emptyMap<Any, Any>()
                 val limitsMap = mutableMapOf<String, Double>()
                 for ((k, v) in map) {
                     if (k is String && v is Number) limitsMap[k] = v.toDouble()
                 }
                 _limits.value = BudgetLimits(limits = limitsMap)
                 
                 _currencyCode.value = snapshot.getString("default_currency") ?: "USD"
                 
                 val userCats = (snapshot.get("categories") as? List<*>)?.mapNotNull { it as? String }
                 if (userCats != null && userCats.isNotEmpty()) {
                     _categories.value = userCats.filter { it != "Income" }
                 }
             }
         }
    }

    fun updateLimit(category: String, amount: Double) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(user.uid).set(
                    mapOf("budgets" to mapOf(category to amount)),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
