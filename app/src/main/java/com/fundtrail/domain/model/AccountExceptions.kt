package com.fundtrail.domain.model

class LastActiveAccountException : IllegalStateException("Cannot archive the last active account. At least one active account is required.")

class DuplicateAccountNameException : IllegalArgumentException("An account with this name already exists.")

class InvalidCurrencyException : IllegalArgumentException("Unsupported currency.")
