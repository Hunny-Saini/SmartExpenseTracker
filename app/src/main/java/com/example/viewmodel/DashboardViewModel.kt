package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.TransactionUiState
import com.example.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val repository = TransactionRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<TransactionUiState>(TransactionUiState.Loading)
    val uiState: StateFlow<TransactionUiState> = _uiState

    private val _userName = MutableStateFlow<String>("User")
    val userName: StateFlow<String> = _userName

    private val _currency = MutableStateFlow<String>("USD")
    val currency: StateFlow<String> = _currency

    private val _totalBudget = MutableStateFlow<Double>(0.0)
    val totalBudget: StateFlow<Double> = _totalBudget

    init {
        fetchUserData()
        viewModelScope.launch {
            repository.getTransactionsStream().collectLatest { state ->
                _uiState.value = state
            }
        }
    }

    private fun fetchUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                _userName.value = snapshot.getString("name") ?: "User"
                _currency.value = snapshot.getString("default_currency") ?: "USD"
                
                val map = snapshot.get("budgets") as? Map<*, *>
                var total = 0.0
                if (map != null) {
                    for (value in map.values) {
                        if (value is Number) {
                            total += value.toDouble()
                        }
                    }
                }
                _totalBudget.value = total
            }
        }
    }
}
