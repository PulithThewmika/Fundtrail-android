package com.fundtrail.ui.common

import com.fundtrail.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeoutException

class ErrorMessageMapperTest {

    private class TestFirebaseNetworkException : FirebaseNetworkException("Network error")
    private class TestFirebaseAuthException : FirebaseAuthException("auth/user-not-found", "User not found")
    private class TestFirebaseFirestoreException(
        private val firestoreCode: Code
    ) : FirebaseFirestoreException("Firestore error", firestoreCode) {
        override fun getCode(): Code = firestoreCode
    }

    @Test
    fun testNetworkExceptionsMapToNetworkError() {
        assertEquals(R.string.error_network, mapErrorToMessage(IOException("Network failed")))
        assertEquals(R.string.error_network, mapErrorToMessage(TimeoutException("Timed out")))
        assertEquals(R.string.error_network, mapErrorToMessage(TestFirebaseNetworkException()))
    }

    @Test
    fun testSecurityAndAuthExceptionsMapToPermissionError() {
        assertEquals(R.string.error_permission, mapErrorToMessage(SecurityException("Access denied")))
        assertEquals(R.string.error_permission, mapErrorToMessage(TestFirebaseAuthException()))
    }

    @Test
    fun testFirestorePermissionDenied() {
        val exception = TestFirebaseFirestoreException(Code.PERMISSION_DENIED)
        assertEquals(R.string.error_permission, mapErrorToMessage(exception))
    }

    @Test
    fun testFirestoreUnavailable() {
        val exception = TestFirebaseFirestoreException(Code.UNAVAILABLE)
        assertEquals(R.string.error_network, mapErrorToMessage(exception))
    }

    @Test
    fun testGenericExceptionFallback() {
        val exception = RuntimeException("Unknown error")
        assertEquals(R.string.error_generic, mapErrorToMessage(exception))
    }
}
