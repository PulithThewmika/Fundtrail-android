package com.fundtrail.domain.usecase.category

import com.fundtrail.domain.model.DuplicateCategoryNameException
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.fakes.FakeCategoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveCategoryUseCaseTest {
    private val repo = FakeCategoryRepository()
    private val save = SaveCategoryUseCase(repo)

    @Test
    fun createsACategoryAtTheEndOfTheListWithAGeneratedId() = runTest {
        val before = repo.observeAll().first()

        assertTrue(save(null, "  Health  ", SpendKind.COMMITTED, "payments") is Result.Success)

        val created = repo.observeAll().first().single { it.name == "Health" }
        assertTrue(created.id.startsWith("cat_"))
        assertEquals(SpendKind.COMMITTED, created.defaultSpendKind)
        assertEquals(before.maxOf { it.sortOrder } + 1, created.sortOrder)
    }

    @Test
    fun changingTheTypeKeepsTheIdAndOtherFields() = runTest {
        val rent = repo.observeAll().first().single { it.id == "cat_rent" }
        assertEquals(SpendKind.COMMITTED, rent.defaultSpendKind)

        assertTrue(save("cat_rent", "Rent", SpendKind.DISCRETIONARY, rent.icon) is Result.Success)

        val updated = repo.observeAll().first().single { it.id == "cat_rent" }
        assertEquals(SpendKind.DISCRETIONARY, updated.defaultSpendKind)
        assertEquals(rent.sortOrder, updated.sortOrder)
        assertEquals(rent.icon, updated.icon)
    }

    @Test
    fun aBlankNameIsRejected() = runTest {
        assertTrue(save(null, "   ", SpendKind.COMMITTED, "payments") is Result.Error)
    }

    @Test
    fun aDuplicateNameIsRejectedIgnoringCaseAndSpaces() = runTest {
        val before = repo.observeAll().first().size

        val result = save(null, "  rENT ", SpendKind.COMMITTED, "payments")

        assertTrue(result is Result.Error && result.exception is DuplicateCategoryNameException)
        assertEquals(before, repo.observeAll().first().size)
    }

    @Test
    fun renamingToAnotherCategorysNameIsRejected() = runTest {
        val result = save("cat_rent", "Groceries", SpendKind.COMMITTED, "payments")

        assertTrue(result is Result.Error && result.exception is DuplicateCategoryNameException)
        assertEquals("Rent", repo.observeAll().first().single { it.id == "cat_rent" }.name)
    }

    @Test
    fun aCategoryMayKeepItsOwnName() = runTest {
        assertTrue(save("cat_rent", "rent", SpendKind.DISCRETIONARY, "payments") is Result.Success)
    }

    @Test
    fun anArchivedCategoryDoesNotBlockItsName() = runTest {
        repo.archive("cat_rent")

        assertTrue(save(null, "Rent", SpendKind.COMMITTED, "payments") is Result.Success)
    }

    @Test
    fun editingAnUnknownCategoryFails() = runTest {
        assertTrue(save("cat_missing", "X", SpendKind.COMMITTED, "payments") is Result.Error)
        assertNotNull(repo.observeAll().first().firstOrNull { it.id == "cat_rent" })
    }
}
