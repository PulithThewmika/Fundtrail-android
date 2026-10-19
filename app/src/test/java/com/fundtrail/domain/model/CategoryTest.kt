package com.fundtrail.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CategoryTest {

    @Test
    fun `valid category succeeds validation`() {
        val category = Category(
            id = "cat_01",
            name = "Coffee & Dining",
            defaultSpendKind = SpendKind.DISCRETIONARY,
            icon = "coffee"
        )
        category.validate()
        assertEquals(SpendKind.DISCRETIONARY, category.defaultSpendKind)
        assertEquals("coffee", category.icon)
        assertFalse(category.archived)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank id fails validation`() {
        val category = Category(
            id = "",
            name = "Rent",
            defaultSpendKind = SpendKind.COMMITTED,
            icon = "home"
        )
        category.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank name fails validation`() {
        val category = Category(
            id = "cat_02",
            name = "  ",
            defaultSpendKind = SpendKind.COMMITTED,
            icon = "home"
        )
        category.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank icon fails validation`() {
        val category = Category(
            id = "cat_03",
            name = "Groceries",
            defaultSpendKind = SpendKind.DISCRETIONARY,
            icon = ""
        )
        category.validate()
    }
}
