package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.model.UserProfile
import com.fundtrail.fakes.FakeAuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveUserProfileUseCaseTest {
    private val auth = FakeAuthRepository()
    private val profile = UserProfile(name = "Kavindu", email = "kavindu@example.com", photoUrl = "https://example.com/p.jpg")

    @Test
    fun `signed out has no profile even if one is cached`() = runTest {
        auth.currentUserProfile = profile
        assertNull(ObserveUserProfileUseCase(auth)().first())
    }

    @Test
    fun `signed in exposes the profile`() = runTest {
        auth.currentUserProfile = profile
        auth.user.value = "uid-1"
        assertEquals(profile, ObserveUserProfileUseCase(auth)().first())
    }
}
