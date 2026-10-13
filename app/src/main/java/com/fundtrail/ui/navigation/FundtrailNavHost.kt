package com.fundtrail.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.fundtrail.R
import com.fundtrail.ui.auth.SignInRoute
import com.fundtrail.ui.auth.SignUpRoute
import com.fundtrail.ui.dashboard.DashboardRoute
import com.fundtrail.ui.more.MoreRoute
import com.fundtrail.ui.onboarding.OnboardingScreen

data class BottomNavTab(
    val route: MainTabRoute,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun FundtrailNavHost(startDestination: Route, isOnline: Boolean) {
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
        Column(Modifier.padding(innerPadding)) {
            // One app-wide banner, neutral wording and colour (FR-110, NFR-14); screens keep showing Room data.
            if (showBottomBar && !isOnline) OfflineBanner()
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.weight(1f),
            ) {
                navigation<Route.AuthGraph>(startDestination = Route.SignIn) {
                    composable<Route.SignIn> {
                        SignInRoute(
                            onSignedIn = { navController.enterMainGraph() },
                            onNavigateToSignUp = { navController.navigate(Route.SignUp) },
                        )
                    }
                    composable<Route.SignUp> {
                        SignUpRoute(
                            onRegistered = {
                                navController.navigate(Route.Onboarding) { popUpTo(Route.SignIn) { inclusive = true } }
                            },
                            onNavigateToSignIn = { navController.popBackStack() },
                        )
                    }
                    composable<Route.Onboarding> {
                        OnboardingScreen(onContinue = { navController.enterMainGraph() })
                    }
                }

                navigation<Route.MainGraph>(startDestination = MainTabRoute.Insights) {
                    composable<MainTabRoute.Insights> {
                        DashboardRoute(onAddExpense = { navController.navigate(Route.QuickAdd) })
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
                    // TODO(M2): #98 replaces this with the quick-add bottom sheet.
                    composable<Route.QuickAdd> {
                        PlaceholderScreen(stringResource(R.string.quick_add_placeholder))
                    }
                    composable<MainTabRoute.More> {
                        MoreRoute(
                            onSignedOut = {
                                // Clears the whole back stack so nothing from the old session can be reached with Back.
                                navController.navigate(Route.AuthGraph) { popUpTo(navController.graph.id) { inclusive = true } }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Text(
        text = stringResource(R.string.offline_banner),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text)
    }
}

private fun NavController.enterMainGraph() {
    navigate(Route.MainGraph) { popUpTo(graph.id) { inclusive = true } }
}
