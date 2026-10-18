package com.fundtrail.ui.categories

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.TakeoutDining
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.fundtrail.R

/**
 * Maps the icon key stored on a category to a vector. The key (not the vector) is what Firestore and
 * Room hold, so a new icon only needs a line here. Unknown keys fall back to a neutral icon.
 */
object CategoryIcons {
    private val icons: Map<String, ImageVector> = linkedMapOf(
        "apartment" to Icons.Outlined.Apartment,
        "bolt" to Icons.Outlined.Bolt,
        "fitness_center" to Icons.Outlined.FitnessCenter,
        "autorenew" to Icons.Outlined.Autorenew,
        "shopping_basket" to Icons.Outlined.ShoppingBasket,
        "wifi" to Icons.Outlined.Wifi,
        "local_taxi" to Icons.Outlined.LocalTaxi,
        "takeout_dining" to Icons.Outlined.TakeoutDining,
        "local_cafe" to Icons.Outlined.LocalCafe,
        "shopping_bag" to Icons.Outlined.ShoppingBag,
        "theaters" to Icons.Outlined.Theaters,
        "more_horiz" to Icons.Outlined.MoreHoriz,
        "payments" to Icons.Outlined.Payments,
        "home" to Icons.Outlined.Home,
    )

    /** Keys offered in the icon picker, in display order. */
    val keys: List<String> = icons.keys.toList()

    fun vectorFor(key: String): ImageVector = icons[key] ?: Icons.Outlined.Category
}

/** Short description for a seeded category; user-made categories have none. */
@StringRes
fun categoryDescriptionRes(categoryId: String): Int? = when (categoryId) {
    "cat_rent" -> R.string.category_desc_cat_rent
    "cat_utilities" -> R.string.category_desc_cat_utilities
    "cat_gym" -> R.string.category_desc_cat_gym
    "cat_subscriptions" -> R.string.category_desc_cat_subscriptions
    "cat_groceries" -> R.string.category_desc_cat_groceries
    "cat_phone_internet" -> R.string.category_desc_cat_phone_internet
    "cat_ride_hailing" -> R.string.category_desc_cat_ride_hailing
    "cat_food_delivery" -> R.string.category_desc_cat_food_delivery
    "cat_coffee_dining" -> R.string.category_desc_cat_coffee_dining
    "cat_shopping" -> R.string.category_desc_cat_shopping
    "cat_entertainment" -> R.string.category_desc_cat_entertainment
    "cat_other" -> R.string.category_desc_cat_other
    else -> null
}
