package com.fundtrail.domain.model

/**
 * Domain model representing an expense category.
 * Note: Categories are exclusively for expenses; income uses [IncomeSource].
 *
 * @property id Unique category ID
 * @property name User-facing category name
 * @property defaultSpendKind Default spending nature ([SpendKind.COMMITTED] or [SpendKind.DISCRETIONARY])
 * @property iconKey Vector or icon key string
 * @property isDefault Whether this is a system default seeded category
 * @property isArchived Whether category is archived
 * @property sortOrder Quick-add chip sorting priority
 * @property createdAt Server creation timestamp (milliseconds)
 * @property updatedAt Server update timestamp (milliseconds)
 */
data class Category(
    val id: String,
    val name: String,
    val defaultSpendKind: SpendKind,
    val iconKey: String,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
) {
    val nature: SpendKind get() = defaultSpendKind
    val icon: String get() = iconKey
    val archived: Boolean get() = isArchived

    /**
     * Validates category invariants:
     * - [id], [name], and [iconKey] must not be blank.
     */
    fun validate() {
        require(id.isNotBlank()) { "Category ID must not be blank" }
        require(name.isNotBlank()) { "Category name must not be blank" }
        require(iconKey.isNotBlank()) { "Category iconKey must not be blank" }
    }
}
