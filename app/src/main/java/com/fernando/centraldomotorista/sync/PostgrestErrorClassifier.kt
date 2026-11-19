package com.fernando.centraldomotorista.sync

object PostgrestErrorClassifier {
    const val UNIQUE_VIOLATION = "23505"
    const val FOREIGN_KEY_VIOLATION = "23503"

    fun isIdempotentDuplicate(sqlState: String?): Boolean = sqlState == UNIQUE_VIOLATION
}
