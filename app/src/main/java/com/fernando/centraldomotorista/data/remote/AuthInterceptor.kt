package com.fernando.centraldomotorista.data.remote

import android.util.Log
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("apikey", SUPABASE_ANON_KEY)

        try {
            val session = runBlocking {
                try {
                    val current = supabase.auth.currentSessionOrNull()
                    if (current != null && current.expiresAt <= kotlinx.datetime.Clock.System.now()) {
                        supabase.auth.refreshCurrentSession()
                    }
                    supabase.auth.currentSessionOrNull()
                } catch (e: Exception) {
                    Log.w("AuthInterceptor", "Erro ao verificar/renovar sessão Supabase: ${e.message}")
                    supabase.auth.currentSessionOrNull()
                }
            }

            val token = session?.accessToken
            if (!token.isNullOrEmpty()) {
                requestBuilder.header("Authorization", "Bearer $token")
                Log.d("AuthInterceptor", "Token JWT Supabase adicionado à requisição para usuário: ${session.user?.id}")
            } else {
                Log.w("AuthInterceptor", "Nenhuma sessão ativa do Supabase encontrada no momento da requisição.")
            }
        } catch (e: Exception) {
            Log.e("AuthInterceptor", "Erro no AuthInterceptor: ${e.message}", e)
        }

        return chain.proceed(requestBuilder.build())
    }
}
