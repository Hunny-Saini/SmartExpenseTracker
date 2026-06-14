package com.example.model

data class TransactionMapObject(
    val id: String = "",
    val amount: Double = 0.0,
    val title: String = "",
    val category: String = "",
    val timestamp: Long = 0L
)
