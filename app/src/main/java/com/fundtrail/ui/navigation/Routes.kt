package com.fundtrail.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object AuthGraph : Route

    @Serializable
    data object MainGraph : Route

    @Serializable
    data object SignIn : Route

    @Serializable
    data object SignUp : Route

    @Serializable
    data object Onboarding : Route

    @Serializable
    data object Accounts : Route

    /** SCR-53, opened from More. */
    @Serializable
    data object Categories : Route

    /** EPIC-012, opened from the dashboard goal card or the Goal tab empty state. */
    @Serializable
    data object GoalSetup : Route

    /** EPIC-012, opened from the dashboard goal card once a goal exists. */
    @Serializable
    data object GoalDetail : Route

    /** EPIC-015, opened from More. */
    @Serializable
    data object Recurring : Route

    /** EPIC-018, opened from More. */
    @Serializable
    data object Reminders : Route
}

@Serializable
sealed interface MainTabRoute : Route {
    @Serializable
    data object Insights : MainTabRoute

    @Serializable
    data object History : MainTabRoute

    @Serializable
    data object Income : MainTabRoute

    @Serializable
    data object Goal : MainTabRoute

    @Serializable
    data object More : MainTabRoute
}
