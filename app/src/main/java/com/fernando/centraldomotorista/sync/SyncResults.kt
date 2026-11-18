package com.fernando.centraldomotorista.sync

import java.time.Instant

sealed class PushResult {
    data object Success : PushResult()
    data class Conflict409(val message: String = "409 Conflict") : PushResult()
    data class Failure(val cause: Throwable) : PushResult()
}

sealed class PullResult {
    data class Success(val newCursor: Instant?, val recordsCount: Int = 0) : PullResult()
    data class Failure(val cause: Throwable) : PullResult()
}
