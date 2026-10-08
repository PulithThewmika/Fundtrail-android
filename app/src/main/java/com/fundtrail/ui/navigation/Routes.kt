package com.fundtrail.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object AuthGraph : Route

    @Serializable
    data object MainGraph : Route
}

@Serializable
sealed interface MainTabRoute {
    @Serializable
    data object Dashboard : MainTabRoute

    @Serializable
    data object Transactions : MainTabRoute

    @Serializable
    data object Goals : MainTabRoute

    @Serializable
    data object Settings : MainTabRoute
}
