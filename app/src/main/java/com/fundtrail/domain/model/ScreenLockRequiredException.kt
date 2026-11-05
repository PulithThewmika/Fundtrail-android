package com.fundtrail.domain.model

/** The app lock can't be turned on: the phone has no screen lock to fall back on (#222). */
class ScreenLockRequiredException : IllegalStateException("A screen lock is required for the app lock")
