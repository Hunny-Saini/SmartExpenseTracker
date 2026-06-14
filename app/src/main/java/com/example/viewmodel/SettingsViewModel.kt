package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser = auth.currentUser

    private val _userData = MutableStateFlow<Map<String, Any>>(emptyMap())
    val userData: StateFlow<Map<String, Any>> = _userData
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        fetchUser()
    }

    private fun fetchUser() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).addSnapshotListener { snap, _ ->
            if (snap != null && snap.exists()) {
                _userData.value = snap.data ?: emptyMap()
            }
        }
    }

    fun updateCurrency(currency: String) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).update("default_currency", currency)
    }

    fun changePassword(newPass: String) {
        viewModelScope.launch {
            try {
                auth.currentUser?.updatePassword(newPass)?.await()
                _message.value = "Password updated successfully!"
            } catch (e: Exception) {
                _message.value = "Failed to update password: ${e.message}"
            }
        }
    }
    
    fun clearMessage() {
        _message.value = null
    }

    fun updateName(newName: String) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).update("name", newName)
    }

    fun addCategory(categoryName: String) {
        val user = auth.currentUser ?: return
        val currentCategories = (_userData.value["categories"] as? List<*>)?.mapNotNull { it as? String } ?: listOf("Food", "Rent", "Shopping", "Transport", "Bills", "Income")
        if (!currentCategories.contains(categoryName)) {
            val updated = currentCategories + categoryName
            db.collection("users").document(user.uid).update("categories", updated)
        }
    }

    fun deleteCategory(categoryName: String) {
        val user = auth.currentUser ?: return
        val currentCategories = (_userData.value["categories"] as? List<*>)?.mapNotNull { it as? String } ?: listOf("Food", "Rent", "Shopping", "Transport", "Bills", "Income")
        if (currentCategories.contains(categoryName)) {
            val updated = currentCategories - categoryName
            db.collection("users").document(user.uid).update("categories", updated)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
