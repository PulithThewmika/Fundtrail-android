package com.fundtrail.domain.model

/**
 * Domain model representing an expense category.
 * Note: Categories are exclusively for expenses; income uses [IncomeSource].
 *
 * @property id Unique category ID
 * @property name User-facing category name
 * @property defaultSpendKind Default spending nature ([SpendKind.COMMITTED] or [SpendKind.DISCRETIONARY])
 * @property icon Vector or icon key string
 * @property archived Whether category is archived
 * @property sortOrder Quick-add chip sorting priority
 * @property createdAt Server creation timestamp (milliseconds)
 * @property updatedAt Server update timestamp (milliseconds)
 */
data class Category(
    val id: String,
    val name: String,
    val defaultSpendKind: SpendKind,
    val icon: String,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null
) {
    /**
     * Validates category invariants:
     * - [id], [name], and [icon] must not be blank.
     */
    fun validate() {
        require(id.isNotBlank()) { "Category ID must not be blank" }
        require(name.isNotBlank()) { "Category name must not be blank" }
        require(icon.isNotBlank()) { "Category icon must not be blank" }
    }
}
