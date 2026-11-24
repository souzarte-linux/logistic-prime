package com.fernando.centraldomotorista.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface UserSession {
    val userIdFlow: Flow<String?>
    fun currentUserId(): String?
}

@Singleton
class SupabaseUserSession @Inject constructor(
    private val supabase: SupabaseClient
) : UserSession {

    override val userIdFlow: Flow<String?> = supabase.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.user?.id ?: supabase.auth.currentUserOrNull()?.id
            is SessionStatus.NotAuthenticated -> null
            else -> supabase.auth.currentUserOrNull()?.id
        }
    }.distinctUntilChanged()

    override fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id
}
