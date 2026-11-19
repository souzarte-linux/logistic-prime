package com.fernando.centraldomotorista.sync

import java.time.Instant

sealed class PushResult {
    data object Success : PushResult()
    data object AlreadyApplied : PushResult()
    data class Failure(val cause: Throwable) : PushResult()
}

sealed class PullResult {
    data class Success(val newCursor: Instant?, val recordsCount: Int = 0) : PullResult()
    data class Failure(val cause: Throwable) : PullResult()
}
