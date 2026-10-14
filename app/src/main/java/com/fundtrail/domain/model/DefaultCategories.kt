package com.fundtrail.domain.model

/**
 * The expense categories every user starts with (FR-20, EPIC-008 T2 seeding).
 *
 * Ids are fixed so seeding twice, or from two devices, writes the same documents instead of
 * duplicates (schema.md section 5). Never rename an id: transactions point at it.
 * [Category.icon] holds an icon key that the UI maps to a vector (`CategoryIcons`).
 */
object DefaultCategories {

    val committed: List<Category> = listOf(
        seed("cat_rent", "Rent", SpendKind.COMMITTED, "apartment", 1),
        seed("cat_utilities", "Utilities", SpendKind.COMMITTED, "bolt", 2),
        seed("cat_gym", "Gym", SpendKind.COMMITTED, "fitness_center", 3),
        seed("cat_subscriptions", "Subscriptions", SpendKind.COMMITTED, "autorenew", 4),
        seed("cat_groceries", "Groceries", SpendKind.COMMITTED, "shopping_basket", 5),
        seed("cat_phone_internet", "Phone & internet", SpendKind.COMMITTED, "wifi", 6),
    )

    val discretionary: List<Category> = listOf(
        seed("cat_ride_hailing", "Ride-hailing", SpendKind.DISCRETIONARY, "local_taxi", 7),
        seed("cat_food_delivery", "Food delivery", SpendKind.DISCRETIONARY, "takeout_dining", 8),
        seed("cat_coffee_dining", "Coffee & dining", SpendKind.DISCRETIONARY, "local_cafe", 9),
        seed("cat_shopping", "Shopping", SpendKind.DISCRETIONARY, "shopping_bag", 10),
        seed("cat_entertainment", "Entertainment", SpendKind.DISCRETIONARY, "theaters", 11),
        seed("cat_other", "Other", SpendKind.DISCRETIONARY, "more_horiz", 12),
    )

    val all: List<Category> = committed + discretionary

    private fun seed(id: String, name: String, kind: SpendKind, icon: String, sortOrder: Int) =
        Category(id = id, name = name, defaultSpendKind = kind, icon = icon, sortOrder = sortOrder)
}
