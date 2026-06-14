package com.example.repository

import com.example.model.TransactionMapObject
import com.example.model.TransactionUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransactionRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    fun getTransactionsStream(): Flow<TransactionUiState> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(TransactionUiState.Error("User not authenticated"))
            close()
            return@callbackFlow
        }
        
        val userId = currentUser.uid
        val listenerRegistration = db.collection("users").document(userId).collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(TransactionUiState.Error(error.message ?: "Unknown error"))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(TransactionMapObject::class.java)?.copy(id = doc.id)
                    }
                    
                    val totalBalance = transactions.sumOf { tx -> 
                        if (tx.category == "Income") tx.amount else -tx.amount 
                    }
                    
                    val totalMonthlyOutflow = calculateMonthlyOutflow(transactions)
                    
                    val grouped = transactions.groupBy { tx ->
                        formatTimestampToDateString(tx.timestamp)
                    }
                    
                    trySend(TransactionUiState.Success(
                        totalBalance = totalBalance, 
                        totalMonthlyOutflow = totalMonthlyOutflow,
                        groupedTransactions = grouped
                    ))
                }
            }
            
        awaitClose {
            listenerRegistration.remove()
        }
    }
    
    private fun calculateMonthlyOutflow(transactions: List<TransactionMapObject>): Double {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val thirtyDaysAgo = cal.timeInMillis
        return transactions
            .filter { it.timestamp >= thirtyDaysAgo && it.category != "Income" }
            .sumOf { it.amount }
    }
    
    private fun formatTimestampToDateString(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    fun addTransaction(amount: Double, title: String, category: String, timestamp: Long = System.currentTimeMillis()) {
         val currentUser = auth.currentUser ?: return
         val tx = hashMapOf(
             "amount" to amount,
             "title" to title,
             "category" to category,
             "timestamp" to timestamp
         )
         db.collection("users").document(currentUser.uid).collection("transactions").add(tx)
    }

    fun updateTransaction(id: String, amount: Double, title: String, category: String, timestamp: Long) {
         val currentUser = auth.currentUser ?: return
         val tx = hashMapOf(
             "amount" to amount,
             "title" to title,
             "category" to category,
             "timestamp" to timestamp
         )
         db.collection("users").document(currentUser.uid).collection("transactions").document(id).set(tx)
    }

    fun deleteTransaction(id: String) {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).collection("transactions").document(id).delete()
    }
}
