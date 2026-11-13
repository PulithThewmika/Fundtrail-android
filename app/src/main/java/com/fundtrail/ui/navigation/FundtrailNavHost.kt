package com.fundtrail.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.fundtrail.ui.common.Motion
import com.fundtrail.ui.common.rememberHaptics
import com.fundtrail.ui.common.systemAnimationsEnabled
import com.fundtrail.ui.common.MaxContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.fundtrail.ui.common.card
import com.fundtrail.ui.theme.Spacing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import com.fundtrail.ui.accounts.AccountsRoute
import com.fundtrail.ui.auth.ConfirmResetPasswordRoute
import com.fundtrail.ui.auth.ForgotPasswordRoute
import com.fundtrail.ui.auth.SignInRoute
import com.fundtrail.ui.auth.SignUpRoute
import com.fundtrail.ui.categories.CategoriesRoute
import com.fundtrail.ui.dashboard.DashboardRoute
import com.fundtrail.ui.history.HistoryRoute
import com.fundtrail.ui.income.IncomeRoute
import com.fundtrail.ui.goal.GoalDetailRoute
import com.fundtrail.ui.goal.GoalSetupRoute
import com.fundtrail.ui.more.MoreRoute
import com.fundtrail.ui.onboarding.OnboardingRoute
import com.fundtrail.ui.insights.InsightsRoute
import com.fundtrail.ui.quickadd.QuickAddRoute
import com.fundtrail.ui.recurring.RecurringRoute
import com.fundtrail.ui.reminders.ReminderRoute
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BottomNavTab(
    val route: MainTabRoute,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

@Composable
fun FundtrailNavHost(
    startDestination: Route,
    isOnline: Boolean,
    deepLinkInvoiceId: String? = null,
    quickAddRequested: Boolean = false,
    onQuickAddHandled: () -> Unit = {},
    /** Clears the request at the activity, so a recreation does not reopen a handled invoice (#256). */
    onDeepLinkHandled: (handledInvoiceId: String) -> Unit = {},
    /** The `oobCode` from a password-reset email link (FR-04), or null outside that flow. */
    resetPasswordOobCode: String? = null,
    /** Clears the request at the activity once the reset screen has consumed it. */
    onResetPasswordHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    var pendingDeepLinkInvoiceId by rememberSaveable { mutableStateOf(deepLinkInvoiceId) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(deepLinkInvoiceId) {
        pendingDeepLinkInvoiceId = deepLinkInvoiceId
    }

    // The reset-password email link can arrive signed in or out, so it clears the whole back stack (FR-04).
    LaunchedEffect(resetPasswordOobCode) {
        if (resetPasswordOobCode != null) {
            navController.navigate(Route.ConfirmResetPassword) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
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

    // The widget's "+ Add" (#228): bring the user to the dashboard, where the sheet opens. Signed out, the request
    // is dropped so it cannot pop open after the next login.
    LaunchedEffect(quickAddRequested, currentDestination) {
        if (!quickAddRequested || currentDestination == null) return@LaunchedEffect
        val inMainGraph = currentDestination.hierarchy.any { it.hasRoute(Route.MainGraph::class) }
        when {
            !inMainGraph -> onQuickAddHandled()
            !currentDestination.hasRoute(MainTabRoute.Insights::class) ->
                navController.navigate(MainTabRoute.Insights) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
        }
    }

    val reducedMotion = !systemAnimationsEnabled()
    val haptics = rememberHaptics()
    val items = listOf(
        BottomNavTab(MainTabRoute.Insights, "Insights", Icons.Outlined.Home, Icons.Filled.Home),
        BottomNavTab(MainTabRoute.History, "History", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
        BottomNavTab(MainTabRoute.Income, "Income", Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet),
        BottomNavTab(MainTabRoute.Goal, "Goal", Icons.Outlined.Flag, Icons.Filled.Flag),
        BottomNavTab(MainTabRoute.More, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
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
                // Centred, so on a wide screen the bar stays a phone-width pill in the middle.
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.card,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .widthIn(max = MaxContentWidth)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    ) {
                        Row(Modifier.padding(6.dp).selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
                            items.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true ||
                                    (onCategories && tab.route == MainTabRoute.More)
                                val onClick = {
                                    if (!selected) haptics.tick()
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                // The selected tab is wider; the weight eases between the two, so the pill glides.
                                val weight by animateFloatAsState(if (selected) 2.3f else 1f, tween(Motion.STANDARD, easing = Motion.Decelerate), label = "tabWeight")
                                val pill by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, tween(Motion.STANDARD), label = "tabPill")
                                val tint by animateColorAsState(if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, tween(Motion.STANDARD), label = "tabTint")
                                Row(
                                    Modifier
                                        .weight(weight)
                                        .clip(CircleShape)
                                        .background(pill)
                                        .selectable(selected = selected, role = Role.Tab, onClick = onClick)
                                        .heightIn(min = 48.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        if (selected) tab.selectedIcon else tab.icon,
                                        // The visible label names the selected tab; the others have only the icon.
                                        contentDescription = if (selected) null else tab.label,
                                        tint = tint,
                                    )
                                    AnimatedVisibility(
                                        visible = selected,
                                        enter = fadeIn(tween(Motion.STANDARD)) + expandHorizontally(tween(Motion.STANDARD, easing = Motion.Decelerate)),
                                        exit = fadeOut(tween(100)) + shrinkHorizontally(tween(Motion.STANDARD, easing = Motion.Decelerate)),
                                    ) {
                                        Text(
                                            tab.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(start = Spacing.sm),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        // Consumed as well as applied: screens host their own Scaffold, which would otherwise add the status
        // bar inset a second time and leave a dead gap above every title.
        Column(Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)) {
            // One app-wide banner, neutral wording and colour (FR-110, NFR-14); screens keep showing Room data.
            if (showBottomBar && !isOnline) OfflineBanner()
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.weight(1f),
                // Peer screens (the five tabs, and the step into the app) fade through; deeper pages slide a little
                // sideways, so going in and coming back read as a direction. Reduced-motion users get plain fades.
                enterTransition = {
                    if (reducedMotion) fadeIn(tween(Motion.FAST))
                    else if (targetState.isTab()) fadeIn(tween(Motion.STANDARD, 90, Motion.Decelerate)) + scaleIn(tween(Motion.STANDARD, 90, Motion.Decelerate), 0.96f)
                    else fadeIn(tween(Motion.STANDARD, 90, Motion.Decelerate)) + slideInHorizontally(tween(Motion.SLOW, easing = Motion.Decelerate)) { it / 10 }
                },
                exitTransition = {
                    if (reducedMotion || targetState.isTab()) fadeOut(tween(90))
                    else fadeOut(tween(90)) + slideOutHorizontally(tween(Motion.SLOW, easing = Motion.Decelerate)) { -it / 10 }
                },
                popEnterTransition = {
                    if (reducedMotion) fadeIn(tween(Motion.FAST))
                    else fadeIn(tween(Motion.STANDARD, 90, Motion.Decelerate)) + slideInHorizontally(tween(Motion.SLOW, easing = Motion.Decelerate)) { -it / 10 }
                },
                popExitTransition = {
                    if (reducedMotion) fadeOut(tween(90))
                    else fadeOut(tween(90)) + slideOutHorizontally(tween(Motion.SLOW, easing = Motion.Decelerate)) { it / 10 }
                },
            ) {
                navigation<Route.AuthGraph>(startDestination = Route.SignIn) {
                    composable<Route.SignIn> {
                        SignInRoute(
                            onSignedIn = { navController.enterMainGraph() },
                            onNavigateToSignUp = { navController.navigate(Route.SignUp) },
                            onNavigateToForgotPassword = { navController.navigate(Route.ForgotPassword) },
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
                    composable<Route.ForgotPassword> {
                        ForgotPasswordRoute(onNavigateToSignIn = { navController.popBackStack() })
                    }
                    composable<Route.ConfirmResetPassword> {
                        ConfirmResetPasswordRoute(
                            oobCode = resetPasswordOobCode ?: "",
                            onDone = {
                                onResetPasswordHandled()
                                navController.navigate(Route.SignIn) { popUpTo(navController.graph.id) { inclusive = true } }
                            },
                        )
                    }
                    composable<Route.Onboarding> {
                        OnboardingRoute(onContinue = { navController.enterMainGraph() })
                    }
                }

                navigation<Route.MainGraph>(
                    // Always Insights: the deep-link effect above does the one-time navigation to Income, so the
                    // start destination never depends on it and cannot replay on recomposition (#256).
                    startDestination = MainTabRoute.Insights
                ) {
                    composable<MainTabRoute.Insights> {
                        var showQuickAdd by rememberSaveable { mutableStateOf(false) }
                        var salaryPrefillAmount by rememberSaveable { mutableStateOf<Long?>(null) }
                        var salaryPrefillDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }

                        LaunchedEffect(quickAddRequested) {
                            if (quickAddRequested) {
                                salaryPrefillAmount = null
                                salaryPrefillDate = null
                                showQuickAdd = true
                                onQuickAddHandled()
                            }
                        }

                        DashboardRoute(
                            onOpenSettings = {
                                navController.navigate(MainTabRoute.More) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
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
                            onOpenRecurring = { navController.navigate(Route.Recurring) },
                            onOpenInsights = { navController.navigate(Route.InsightsDetail) },
                        )
                        // A sheet over the dashboard, not a destination, so the totals stay visible behind it.
                        if (showQuickAdd) {
                            QuickAddRoute(
                                onDismiss = { showQuickAdd = false },
                                onAddAccount = { showQuickAdd = false; navController.navigate(Route.Accounts) },
                                onAddCategory = { showQuickAdd = false; navController.navigate(Route.Categories) },
                                initialSalaryAmount = salaryPrefillAmount,
                                initialSalaryDate = salaryPrefillDate,
                                onSaved = { undo ->
                                    haptics.success()
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
                                onAddAccount = { editTransactionId = null; showAdd = false; navController.navigate(Route.Accounts) },
                                onAddCategory = { editTransactionId = null; showAdd = false; navController.navigate(Route.Categories) },
                                editTransactionId = editTransactionId,
                                onSaved = { undo ->
                                    haptics.success()
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
                        // Captured now: the callback runs from an effect keyed on this id, and a newer link may have
                        // replaced the pending one by then, which this callback must not clear (#256).
                        val handling = pendingDeepLinkInvoiceId
                        IncomeRoute(
                            initialInvoiceId = handling,
                            onInitialInvoiceHandled = {
                                if (pendingDeepLinkInvoiceId == handling) pendingDeepLinkInvoiceId = null
                                handling?.let(onDeepLinkHandled)
                            },
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
                            onOpenRecurring = { navController.navigate(Route.Recurring) },
                            onOpenReminders = { navController.navigate(Route.Reminders) },
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
                        RecurringRoute(onBack = { navController.popBackStack() })
                    }
                    composable<Route.InsightsDetail> {
                        InsightsRoute(onBack = { navController.popBackStack() })
                    }
                    composable<Route.Reminders> {
                        ReminderRoute(onBack = { navController.popBackStack() })
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

private val tabRoutes = listOf(MainTabRoute.Insights, MainTabRoute.History, MainTabRoute.Income, MainTabRoute.Goal, MainTabRoute.More)

/** True for the five tab screens, which fade through; everything else slides. */
private fun androidx.navigation.NavBackStackEntry.isTab(): Boolean =
    destination.hierarchy.any { d -> tabRoutes.any { d.hasRoute(it::class) } }
