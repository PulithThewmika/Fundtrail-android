package com.fundtrail.domain.model

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun testRunCatchingResultSuccess() {
        val result = runCatchingResult { "Hello" }
        assertTrue(result is Result.Success)
        assertEquals("Hello", result.getOrNull())
    }

    @Test
    fun testRunCatchingResultFailure() {
        val result = runCatchingResult { throw IllegalStateException("Boom") }
        assertTrue(result is Result.Error)
        assertEquals("Boom", (result as Result.Error).exception.message)
    }

    @Test(expected = CancellationException::class)
    fun testRunCatchingResultRethrowsCancellationException() {
        runCatchingResult { throw CancellationException("Coroutine cancelled") }
    }
}
