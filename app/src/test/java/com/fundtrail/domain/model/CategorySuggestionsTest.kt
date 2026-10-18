package com.fundtrail.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorySuggestionsTest {

    private fun cat(id: String, sortOrder: Int, archived: Boolean = false, deletedAt: Long? = null) = Category(
        id = id, name = id, defaultSpendKind = SpendKind.DISCRETIONARY, icon = "payments",
        archived = archived, sortOrder = sortOrder, deletedAt = deletedAt,
    )

    private val rent = cat("rent", 1)
    private val coffee = cat("coffee", 2)
    private val taxi = cat("taxi", 3)
    private val shop = cat("shop", 4)
    private val all = listOf(shop, taxi, rent, coffee) // deliberately unsorted

    private fun ids(list: List<Category>) = list.map { it.id }

    // Level 1: most recent category used with the account.

    @Test
    fun level1_recentCategoryOfTheAccountWinsEvenWhenAnotherIsUsedMoreOverall() {
        val ranked = CategorySuggestions.rank(all, recentForAccountId = "shop", usesByCategory = mapOf("coffee" to 50, "shop" to 1))

        assertEquals("shop", ranked.first().id)
    }

    @Test
    fun level1_restOfTheListIsMostUsedFirstWithoutDuplicatingTheSuggestion() {
        val ranked = CategorySuggestions.rank(all, "shop", mapOf("taxi" to 9, "coffee" to 5, "shop" to 1))

        assertEquals(listOf("shop", "taxi", "coffee", "rent"), ids(ranked))
    }

    // Level 2: no recent category for this account, so the most-used overall.

    @Test
    fun level2_mostUsedOverallWhenTheAccountHasNoHistory() {
        val ranked = CategorySuggestions.rank(all, recentForAccountId = null, usesByCategory = mapOf("taxi" to 7, "coffee" to 3))

        assertEquals(listOf("taxi", "coffee", "rent", "shop"), ids(ranked))
    }

    @Test
    fun level2_equalUsageBreaksTiesBySortOrder() {
        val ranked = CategorySuggestions.rank(all, null, mapOf("shop" to 4, "coffee" to 4))

        assertEquals(listOf("coffee", "shop", "rent", "taxi"), ids(ranked))
    }

    @Test
    fun level2_aRecentIdThatIsNotAnActiveCategoryIsIgnored() {
        val ranked = CategorySuggestions.rank(all, recentForAccountId = "deleted-long-ago", usesByCategory = mapOf("taxi" to 2))

        assertEquals("taxi", ranked.first().id)
    }

    // Level 3: nothing used at all, so the first by sortOrder.

    @Test
    fun level3_firstBySortOrderWhenNothingHasBeenUsed() {
        val ranked = CategorySuggestions.rank(all, recentForAccountId = null, usesByCategory = emptyMap())

        assertEquals(listOf("rent", "coffee", "taxi", "shop"), ids(ranked))
    }

    @Test
    fun level3_zeroCountsBehaveLikeNoUsage() {
        val ranked = CategorySuggestions.rank(all, null, mapOf("shop" to 0, "taxi" to 0))

        assertEquals("rent", ranked.first().id)
    }

    // Archived and deleted categories are never suggested (FR-21).

    @Test
    fun archivedCategoriesAreNeverSuggestedEvenIfTheyAreTheRecentOrMostUsed() {
        val withArchived = all + cat("old", 0, archived = true)

        val ranked = CategorySuggestions.rank(withArchived, recentForAccountId = "old", usesByCategory = mapOf("old" to 99))

        assertTrue("old" !in ids(ranked))
        assertEquals("rent", ranked.first().id)
    }

    @Test
    fun deletedCategoriesAreNeverSuggested() {
        val ranked = CategorySuggestions.rank(all + cat("gone", 0, deletedAt = 1L), null, emptyMap())

        assertTrue("gone" !in ids(ranked))
    }

    @Test
    fun noActiveCategoriesGivesAnEmptyList() {
        assertEquals(emptyList<Category>(), CategorySuggestions.rank(listOf(cat("a", 1, archived = true)), "a", mapOf("a" to 3)))
        assertEquals(emptyList<Category>(), CategorySuggestions.rank(emptyList(), null, emptyMap()))
    }
}
