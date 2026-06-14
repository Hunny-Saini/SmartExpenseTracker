package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.AddExpenseScreen
import com.example.ui.AuthScreen
import com.example.ui.BudgetPlanningScreen
import com.example.ui.DashboardScreen
import com.example.ui.InsightsScreen
import com.example.ui.SettingsScreen
import com.example.ui.SplashScreen
import com.example.ui.theme.MyApplicationTheme

import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Local state for app theme switch based on user requirements.
            var isDarkTheme by remember { mutableStateOf(false) }
            
            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        Scaffold(
                            bottomBar = {
                                if (currentRoute in listOf("dashboard", "insights", "budget")) {
                                    NavigationBar {
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                            label = { Text("Home") },
                                            selected = currentRoute == "dashboard",
                                            onClick = {
                                                navController.navigate("dashboard") {
                                                    popUpTo("dashboard") { inclusive = true }
                                                }
                                            }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.Info, contentDescription = "Insights") },
                                            label = { Text("Insights") },
                                            selected = currentRoute == "insights",
                                            onClick = {
                                                navController.navigate("insights") {
                                                    popUpTo("dashboard") { inclusive = false }
                                                }
                                            }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Budgets") },
                                            label = { Text("Budgets") },
                                            selected = currentRoute == "budget",
                                            onClick = {
                                                navController.navigate("budget") {
                                                    popUpTo("dashboard") { inclusive = false }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController, 
                                startDestination = "splash", 
                                modifier = Modifier.padding(innerPadding),
                                enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideInHorizontally(initialOffsetX = { 1000 }) },
                                exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -1000 }) },
                                popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideInHorizontally(initialOffsetX = { -1000 }) },
                                popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideOutHorizontally(targetOffsetX = { 1000 }) }
                            ) {
                        composable("splash") {
                            SplashScreen(
                                onNavigateToDashboard = {
                                    navController.navigate("dashboard") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                },
                                onNavigateToAuth = {
                                    navController.navigate("auth") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("auth") {
                            AuthScreen(
                                onAuthSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("dashboard") {
                            DashboardScreen(
                                onNavigateToAddExpense = { id -> 
                                    if (id != null) {
                                        navController.navigate("add_expense?transactionId=$id")
                                    } else {
                                        navController.navigate("add_expense")
                                    }
                                },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToInsights = { navController.navigate("insights") },
                                onNavigateToBudget = { navController.navigate("budget") }
                            )
                        }
                        composable("add_expense?transactionId={transactionId}") { backStackEntry ->
                            val transactionId = backStackEntry.arguments?.getString("transactionId")
                            AddExpenseScreen(
                                onNavigateBack = { navController.navigateUp() },
                                transactionId = transactionId
                            )
                        }
                        composable("insights") {
                            InsightsScreen(
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                        composable("budget") {
                            BudgetPlanningScreen(
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onNavigateToAuth = {
                                    navController.navigate("auth") {
                                        popUpTo(0) { inclusive = true } // Clear backstack entirely
                                    }
                                },
                                onNavigateBack = { navController.navigateUp() },
                                isDarkTheme = isDarkTheme,
                                onThemeToggle = { isDarkTheme = it }
                            )
                        }
                        }
                    }
                }
            }
        }
    }
}

