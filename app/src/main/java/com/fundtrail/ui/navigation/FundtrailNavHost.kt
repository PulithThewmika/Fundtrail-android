package com.fundtrail.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController

data class BottomNavTab(
    val route: MainTabRoute,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun FundtrailNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        BottomNavTab(MainTabRoute.Insights, "Insights", Icons.Default.Home),
        BottomNavTab(MainTabRoute.History, "History", Icons.AutoMirrored.Filled.List),
        BottomNavTab(MainTabRoute.Income, "Income", Icons.Default.Add),
        BottomNavTab(MainTabRoute.Goal, "Goal", Icons.Default.Star),
        BottomNavTab(MainTabRoute.More, "More", Icons.Default.Menu),
    )

    val showBottomBar = items.any { tab ->
        currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.MainGraph,
            modifier = Modifier.padding(innerPadding),
        ) {
            navigation<Route.AuthGraph>(startDestination = Route.SignIn) {
                composable<Route.SignIn> {
                    PlaceholderScreen("Sign In Screen Placeholder")
                }
            }

            navigation<Route.MainGraph>(startDestination = MainTabRoute.Insights) {
                composable<MainTabRoute.Insights> {
                    PlaceholderScreen("Insights Screen Placeholder")
                }
                composable<MainTabRoute.History> {
                    PlaceholderScreen("History Screen Placeholder")
                }
                composable<MainTabRoute.Income> {
                    PlaceholderScreen("Income Screen Placeholder")
                }
                composable<MainTabRoute.Goal> {
                    PlaceholderScreen("Goal Screen Placeholder")
                }
                composable<MainTabRoute.More> {
                    PlaceholderScreen("More Screen Placeholder")
                }
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
