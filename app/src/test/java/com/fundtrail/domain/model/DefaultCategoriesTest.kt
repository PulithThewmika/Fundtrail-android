package com.fundtrail.domain.model

import com.fundtrail.ui.categories.CategoryIcons
import com.fundtrail.ui.categories.categoryDescriptionRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCategoriesTest {

    @Test
    fun committedCategoriesMatchTheSpec() {
        assertEquals(
            listOf("Rent", "Utilities", "Gym", "Subscriptions", "Groceries", "Phone & internet"),
            DefaultCategories.committed.map { it.name },
        )
        assertTrue(DefaultCategories.committed.all { it.defaultSpendKind == SpendKind.COMMITTED })
    }

    @Test
    fun discretionaryCategoriesMatchTheSpec() {
        assertEquals(
            listOf("Ride-hailing", "Food delivery", "Coffee & dining", "Shopping", "Entertainment", "Other"),
            DefaultCategories.discretionary.map { it.name },
        )
        assertTrue(DefaultCategories.discretionary.all { it.defaultSpendKind == SpendKind.DISCRETIONARY })
    }

    @Test
    fun idsAreFixedUniqueAndSortOrderFollowsTheList() {
        val all = DefaultCategories.all
        assertEquals(12, all.size)
        assertEquals(12, all.map { it.id }.toSet().size)
        assertEquals((1..12).toList(), all.map { it.sortOrder })
        assertEquals(
            listOf(
                "cat_rent", "cat_utilities", "cat_gym", "cat_subscriptions", "cat_groceries", "cat_phone_internet",
                "cat_ride_hailing", "cat_food_delivery", "cat_coffee_dining", "cat_shopping", "cat_entertainment", "cat_other",
            ),
            all.map { it.id },
        )
    }

    @Test
    fun everyDefaultIsValidAndStartsActive() {
        DefaultCategories.all.forEach {
            it.validate()
            assertTrue("${it.id} must start active", !it.archived)
        }
    }

    @Test
    fun everyDefaultHasAnIconAndADescriptionInTheUi() {
        DefaultCategories.all.forEach {
            assertTrue("${it.id} icon ${it.icon} is not in CategoryIcons", it.icon in CategoryIcons.keys)
            assertNotNull("${it.id} has no description", categoryDescriptionRes(it.id))
        }
    }
}
