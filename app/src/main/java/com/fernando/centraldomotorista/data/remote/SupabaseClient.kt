package com.fernando.centraldomotorista.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

const val SUPABASE_URL = "https://koocvhlprwtdympjwbco.supabase.co"
const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imtvb2N2aGxwcnd0ZHltcGp3YmNvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc3MDA1OTYsImV4cCI6MjEwMzI3NjU5Nn0.celeh6fUU4jgEC7C1FDm4z2nqnsgagblg9VwcStaQE0"
const val SUPABASE_REST_URL = "$SUPABASE_URL/rest/v1/"

val supabase: SupabaseClient = createSupabaseClient(
    supabaseUrl = SUPABASE_URL,
    supabaseKey = SUPABASE_ANON_KEY
) {
    install(Auth)
}

