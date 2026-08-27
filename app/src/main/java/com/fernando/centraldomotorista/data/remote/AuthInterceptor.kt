package com.fernando.centraldomotorista.data.remote

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                // Obter o token JWT do Firebase síncronamente com timeout
                val tokenResult = Tasks.await(user.getIdToken(false), 10, TimeUnit.SECONDS)
                val token = tokenResult.token
                if (!token.isNullOrEmpty()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                    Log.d("JWT_DIAGNOSTICO", token)
                }
            } catch (e: Exception) {
                Log.e("AuthInterceptor", "Erro ao obter token JWT do Firebase: ${e.message}", e)
            }
        } else {
            Log.w("AuthInterceptor", "Nenhum usuário autenticado no Firebase no momento da requisição.")
        }

        return chain.proceed(requestBuilder.build())
    }
}
