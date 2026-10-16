package com.fundtrail.ui

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AccountRepository
import com.fundtrail.domain.usecase.account.SeedDefaultAccountsUseCase
import com.fundtrail.domain.usecase.auth.ObserveCurrentUserIdUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import com.fundtrail.fakes.FakeAccountRepository
import com.fundtrail.fakes.FakeAuthRepository
import com.fundtrail.fakes.FakeSyncStatusRepository
import com.fundtrail.fakes.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private var seeds = 0
    private val accounts = object : AccountRepository by FakeAccountRepository() {
        override suspend fun seedDefaultsIfEmpty(): Result<Unit> = Result.Success(Unit).also { seeds++ }
    }

    private fun createVm() = MainViewModel(
        ObserveCurrentUserIdUseCase(auth),
        ObserveSyncStatusUseCase(FakeSyncStatusRepository()),
        SeedDefaultAccountsUseCase(accounts),
    )

    @Test
    fun seedsOnAppStartWhenAlreadySignedIn() {
        auth.user.value = "u1"
        createVm()
        assertEquals(1, seeds)
    }

    @Test
    fun seedsOncePerSignInIncludingTheSameUserSigningInAgain() {
        createVm()
        assertEquals(0, seeds)

        auth.user.value = "u1"
        auth.user.value = null
        auth.user.value = "u1"

        assertEquals(2, seeds)
    }
}
