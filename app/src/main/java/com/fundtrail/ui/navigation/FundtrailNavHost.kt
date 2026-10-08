package com.fundtrail.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun FundtrailNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.MainGraph,
    ) {
        composable<Route.AuthGraph> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Auth Graph (Login / Onboarding)")
            }
        }

        composable<Route.MainGraph> {
            MainWithBottomNavShell()
        }
    }
}

data class BottomNavTab(
    val route: MainTabRoute,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun MainWithBottomNavShell() {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        BottomNavTab(MainTabRoute.Dashboard, "Dashboard", Icons.Default.Home),
        BottomNavTab(MainTabRoute.Transactions, "Transactions", Icons.AutoMirrored.Filled.List),
        BottomNavTab(MainTabRoute.Goals, "Goals", Icons.Default.Person),
        BottomNavTab(MainTabRoute.Settings, "Settings", Icons.Default.Settings),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { tab ->
                    val selected = currentRoute?.contains(tab.route::class.simpleName.orEmpty()) == true
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = selected,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = MainTabRoute.Dashboard,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<MainTabRoute.Dashboard> {
                PlaceholderScreen("Dashboard Tab Placeholder")
            }
            composable<MainTabRoute.Transactions> {
                PlaceholderScreen("Transactions Tab Placeholder")
            }
            composable<MainTabRoute.Goals> {
                PlaceholderScreen("Goals Tab Placeholder")
            }
            composable<MainTabRoute.Settings> {
                PlaceholderScreen("Settings Tab Placeholder")
            }
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text)
    }
}
