package com.fernando.centraldomotorista.data.local

import javax.inject.Inject

interface LocalDatabaseProvider {
    fun current(): CostCenterDatabase
}

class GuardedDatabaseProvider @Inject constructor(
    private val guard: UserDatabaseGuard
) : LocalDatabaseProvider {
    override fun current(): CostCenterDatabase = guard.getOrCreateDatabase()
}
