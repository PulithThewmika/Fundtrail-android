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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
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
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.ui.accounts.AccountsRoute
import com.fundtrail.ui.auth.SignInRoute
import com.fundtrail.ui.auth.SignUpRoute
import com.fundtrail.ui.categories.CategoriesRoute
import com.fundtrail.ui.dashboard.DashboardRoute
import com.fundtrail.ui.history.HistoryRoute
import com.fundtrail.ui.income.IncomeRoute
import com.fundtrail.ui.goal.GoalDetailRoute
import com.fundtrail.ui.goal.GoalSetupRoute
import com.fundtrail.ui.more.MoreRoute
import com.fundtrail.ui.onboarding.OnboardingScreen
import com.fundtrail.ui.quickadd.QuickAddRoute
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BottomNavTab(
    val route: MainTabRoute,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun FundtrailNavHost(startDestination: Route, isOnline: Boolean, deepLinkInvoiceId: String? = null) {
    val navController = rememberNavController()
    var pendingDeepLinkInvoiceId by rememberSaveable { mutableStateOf(deepLinkInvoiceId) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(deepLinkInvoiceId) {
        pendingDeepLinkInvoiceId = deepLinkInvoiceId
    }

    LaunchedEffect(pendingDeepLinkInvoiceId, currentDestination) {
        if (pendingDeepLinkInvoiceId != null &&
            currentDestination != null &&
            currentDestination.hierarchy.any { it.hasRoute(Route.MainGraph::class) } &&
            !currentDestination.hasRoute(MainTabRoute.Income::class)
        ) {
            navController.navigate(MainTabRoute.Income) {
                launchSingleTop = true
            }
        }
    }

    val items = listOf(
        BottomNavTab(MainTabRoute.Insights, "Insights", Icons.Default.Home),
        BottomNavTab(MainTabRoute.History, "History", Icons.AutoMirrored.Filled.List),
        BottomNavTab(MainTabRoute.Income, "Income", Icons.Default.Add),
        BottomNavTab(MainTabRoute.Goal, "Goal", Icons.Default.Star),
        BottomNavTab(MainTabRoute.More, "More", Icons.Default.Menu),
    )

    // Categories is a child screen of More: the bar stays and More stays highlighted.
    val onCategories = currentDestination?.hierarchy?.any { it.hasRoute(Route.Categories::class) } == true
    val showBottomBar = onCategories || items.any { tab ->
        currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val savedMessage = stringResource(R.string.quick_add_saved)
    val undoLabel = stringResource(R.string.quick_add_undo)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true ||
                            (onCategories && tab.route == MainTabRoute.More)
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

                navigation<Route.MainGraph>(
                    startDestination = if (deepLinkInvoiceId == null) MainTabRoute.Insights else MainTabRoute.Income
                ) {
                    composable<MainTabRoute.Insights> {
                        var showQuickAdd by rememberSaveable { mutableStateOf(false) }
                        var salaryPrefillAmount by rememberSaveable { mutableStateOf<Long?>(null) }
                        var salaryPrefillDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }

                        DashboardRoute(
                            onAddExpense = {
                                salaryPrefillAmount = null
                                salaryPrefillDate = null
                                showQuickAdd = true
                            },
                            onConfirmSalary = { expectedAmount, date ->
                                salaryPrefillAmount = expectedAmount
                                salaryPrefillDate = date
                                showQuickAdd = true
                            },
                            onSetGoal = { navController.navigate(Route.GoalSetup) },
                            onOpenGoal = { navController.navigate(Route.GoalDetail) },
                        )
                        // A sheet over the dashboard, not a destination, so the totals stay visible behind it.
                        if (showQuickAdd) {
                            QuickAddRoute(
                                onDismiss = { showQuickAdd = false },
                                initialSalaryAmount = salaryPrefillAmount,
                                initialSalaryDate = salaryPrefillDate,
                                onSaved = { undo ->
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val result = snackbarHostState.showSnackbar(
                                            message = savedMessage,
                                            actionLabel = undoLabel,
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            // The delete runs in the ViewModel; this scope only shows the snackbar (§3.3).
                                            undo { messageRes ->
                                                scope.launch { snackbarHostState.showSnackbar(resources.getString(messageRes)) }
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                    composable<MainTabRoute.History> {
                        var editTransactionId by rememberSaveable { mutableStateOf<String?>(null) }
                        var showAdd by rememberSaveable { mutableStateOf(false) }
                        HistoryRoute(
                            onEditTransaction = { id -> editTransactionId = id },
                            onAddEntry = { showAdd = true },
                        )
                        if (editTransactionId != null || showAdd) {
                            QuickAddRoute(
                                onDismiss = { editTransactionId = null; showAdd = false },
                                editTransactionId = editTransactionId,
                                onSaved = { undo ->
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val result = snackbarHostState.showSnackbar(
                                            message = savedMessage,
                                            actionLabel = undoLabel,
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            undo { messageRes -> scope.launch { snackbarHostState.showSnackbar(resources.getString(messageRes)) } }
                                        }
                                    }
                                },
                            )
                        }
                    }
                    composable<MainTabRoute.Income> {
                        IncomeRoute(
                            initialInvoiceId = pendingDeepLinkInvoiceId,
                            onInitialInvoiceHandled = { pendingDeepLinkInvoiceId = null },
                        )
                    }
                    composable<MainTabRoute.Goal> {
                        GoalDetailRoute(
                            onBack = null,
                            onSetGoal = { navController.navigate(Route.GoalSetup) },
                        )
                    }
                    composable<MainTabRoute.More> {
                        MoreRoute(
                            onOpenCategories = { navController.navigate(Route.Categories) },
                            onSignedOut = {
                                // An Undo left over from quick-add would act on the old session after its ViewModel is gone.
                                snackbarHostState.currentSnackbarData?.dismiss()
                                // Clears the whole back stack so nothing from the old session can be reached with Back.
                                navController.navigate(Route.AuthGraph) { popUpTo(navController.graph.id) { inclusive = true } }
                            },
                            onNavigateToAccounts = {
                                navController.navigate(Route.Accounts)
                            },
                        )
                    }
                    composable<Route.Accounts> {
                        AccountsRoute(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable<Route.Categories> {
                        CategoriesRoute(onBack = { navController.popBackStack() })
                    }
                    composable<Route.GoalSetup> {
                        GoalSetupRoute(onBack = { navController.popBackStack() })
                    }
                    composable<Route.GoalDetail> {
                        GoalDetailRoute(
                            onBack = { navController.popBackStack() },
                            onSetGoal = { navController.navigate(Route.GoalSetup) },
                        )
                    }
                    composable<Route.Recurring> {
                        PlaceholderScreen("Recurring Placeholder")
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
