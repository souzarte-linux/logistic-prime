package com.fernando.centraldomotorista.data.remote

import com.fernando.centraldomotorista.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

val SUPABASE_URL = BuildConfig.SUPABASE_URL
val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY
val SUPABASE_REST_URL = "$SUPABASE_URL/rest/v1/"

val supabase: SupabaseClient by lazy {
    createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            sessionManager = try {
                io.github.jan.supabase.auth.SettingsSessionManager()
            } catch (e: Throwable) {
                io.github.jan.supabase.auth.MemorySessionManager()
            }
        }
    }
}

