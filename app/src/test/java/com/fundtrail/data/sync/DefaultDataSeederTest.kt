package com.fundtrail.data.sync

import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.CategoryRepository
import com.fundtrail.fakes.FakeAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDataSeederTest {

    private class CountingCategories : CategoryRepository {
        var seedCalls = 0
        var result: Result<Unit> = Result.Success(Unit)
        override suspend fun seedDefaultsIfEmpty(): Result<Unit> {
            seedCalls++
            return result
        }
        override fun observeActive(): Flow<List<Category>> = error("unused")
        override fun observeAll(): Flow<List<Category>> = error("unused")
        override suspend fun add(category: Category): Result<Unit> = error("unused")
        override suspend fun update(category: Category): Result<Unit> = error("unused")
        override suspend fun archive(id: String): Result<Unit> = error("unused")
        override fun suggestedFor(accountId: String): Flow<List<Category>> = error("unused")
    }

    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(Job() + dispatcher)
    private val auth = FakeAuthRepository()
    private val categories = CountingCategories()

    private fun start() = DefaultDataSeeder(auth, categories, scope, dispatcher).start()

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun doesNothingWhileSignedOut() {
        start()
        assertEquals(0, categories.seedCalls)
    }

    @Test
    fun seedsWhenAUserSignsInAndAgainForTheNextUser() {
        start()

        auth.user.value = "uid-1"
        assertEquals(1, categories.seedCalls)

        auth.user.value = null
        assertEquals(1, categories.seedCalls)

        auth.user.value = "uid-2"
        assertEquals(2, categories.seedCalls)
    }

    @Test
    fun seedsForAnAlreadySignedInUserAtAppStart() {
        auth.user.value = "uid-1"
        start()
        assertEquals(1, categories.seedCalls)
    }

    @Test
    fun aFailedSeedDoesNotStopLaterSignIns() {
        categories.result = Result.Error(IllegalStateException("offline"))
        start()

        auth.user.value = "uid-1"
        auth.user.value = null
        auth.user.value = "uid-2"

        assertEquals(2, categories.seedCalls)
    }
}
