package com.fundtrail.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object AuthGraph : Route

    @Serializable
    data object MainGraph : Route

    @Serializable
    data object SignIn : Route
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
