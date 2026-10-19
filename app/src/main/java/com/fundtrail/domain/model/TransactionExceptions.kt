package com.fundtrail.domain.model

/** A transfer must move money between two different accounts (schema.md section 6.1). */
class SameAccountTransferException : IllegalArgumentException("A transfer needs two different accounts.")
