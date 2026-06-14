package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.repository.TransactionRepository
import com.example.viewmodel.SettingsViewModel

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Delete
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onNavigateBack: () -> Unit,
    transactionId: String? = null,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    var amount by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    
    val userData by settingsViewModel.userData.collectAsState()
    val categories = (userData["categories"] as? List<*>)?.mapNotNull { it as? String } ?: listOf("Food", "Rent", "Shopping", "Transport", "Bills", "Income")
    var category by remember { mutableStateOf(categories.firstOrNull() ?: "Food") }
    var expanded by remember { mutableStateOf(false) }
    
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    val context = LocalContext.current
    
    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                FirebaseFirestore.getInstance().collection("users").document(user.uid)
                    .collection("transactions").document(transactionId).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val data = doc.data
                            amount = data?.get("amount")?.toString() ?: ""
                            title = data?.get("title")?.toString() ?: ""
                            category = data?.get("category")?.toString() ?: "Food"
                            val ts = data?.get("timestamp") as? Long
                            if (ts != null) {
                                calendar = Calendar.getInstance().apply { timeInMillis = ts }
                            }
                        }
                    }
            }
        }
    }

    val dateString = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
    val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val repository = remember { TransactionRepository() }
    
    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(if (transactionId != null) "Edit Expense" else "Add Expense") },
                actions = {
                    if (transactionId != null) {
                        IconButton(onClick = {
                            repository.deleteTransaction(transactionId)
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            ) 
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            androidx.compose.animation.AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; errorMessage = null },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = dateString,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            modifier = Modifier.clickable {
                                DatePickerDialog(context, { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance().apply { timeInMillis = calendar.timeInMillis }
                                    newCal.set(year, month, dayOfMonth)
                                    calendar = newCal
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                            }
                        )
                    }
                )
                OutlinedTextField(
                    value = timeString,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Time") },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Info, // Placeholder for Time
                            contentDescription = "Select Time",
                            modifier = Modifier.clickable {
                                TimePickerDialog(context, { _, hourOfDay, minute ->
                                    val newCal = Calendar.getInstance().apply { timeInMillis = calendar.timeInMillis }
                                    newCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    newCal.set(Calendar.MINUTE, minute)
                                    calendar = newCal
                                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
                            }
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt == null) {
                        errorMessage = "Please enter a valid amount"
                        return@Button
                    }
                    if (title.isBlank()) {
                        errorMessage = "Please enter a title"
                        return@Button
                    }
                    if (transactionId != null) {
                        repository.updateTransaction(transactionId, amt, title, category, calendar.timeInMillis)
                    } else {
                        repository.addTransaction(amt, title, category, calendar.timeInMillis)
                    }
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

