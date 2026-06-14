package com.example.model

sealed interface TransactionUiState {
    object Loading : TransactionUiState
    data class Success(
        val totalBalance: Double,
        val totalMonthlyOutflow: Double,
        val groupedTransactions: Map<String, List<TransactionMapObject>>
    ) : TransactionUiState
    data class Error(val exceptionMessage: String) : TransactionUiState
}
