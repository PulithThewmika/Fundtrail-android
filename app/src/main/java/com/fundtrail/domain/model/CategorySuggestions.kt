package com.fundtrail.domain.model

/**
 * Ranks the categories a new expense could use, best suggestion first (FR-23, FR-31, FR-33).
 *
 * The first element is the suggestion the quick-add sheet pre-selects; the rest are the other chips.
 * The suggestion follows three fallback levels:
 * 1. the category most recently used with the chosen account,
 * 2. otherwise the category used most often overall,
 * 3. otherwise the first category by [Category.sortOrder].
 *
 * The remaining chips are ordered most-used first, then by [Category.sortOrder]. Archived and deleted
 * categories never appear, so a suggestion can never point at a category the user has hidden (FR-21).
 * Pure function: the repository supplies the usage numbers from Room.
 */
object CategorySuggestions {

    /**
     * @param recentForAccountId id of the category of the account's newest expense, or null if the account has none
     * @param usesByCategory how many expenses each category id has
     */
    fun rank(
        categories: List<Category>,
        recentForAccountId: String?,
        usesByCategory: Map<String, Int>,
    ): List<Category> {
        val active = categories
            .filter { !it.archived && it.deletedAt == null }
            .sortedWith(compareBy<Category> { it.sortOrder }.thenBy { it.id })
        // Stable sort: equal usage keeps the sortOrder order from above, which is fallback level 3.
        val byUsage = active.sortedByDescending { usesByCategory[it.id] ?: 0 }
        val recent = recentForAccountId?.let { id -> active.firstOrNull { it.id == id } }
        return if (recent == null) byUsage else listOf(recent) + byUsage.filter { it.id != recent.id }
    }
}
