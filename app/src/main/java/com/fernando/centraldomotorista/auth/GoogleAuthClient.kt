package com.fernando.centraldomotorista.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.fernando.centraldomotorista.data.remote.supabase
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo

sealed class SignInResult {
    data class Success(val supabaseUser: UserInfo) : SignInResult()
    data class Error(val message: String) : SignInResult()
    object Cancelled : SignInResult()
}

class GoogleAuthClient(
    private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    companion object {
        /**
         * Web Client ID do Google Cloud Console.
         */
        const val WEB_CLIENT_ID = "578056018121-1rumesf467p899cr29du1d1sblhhe04n.apps.googleusercontent.com"
    }

    suspend fun signIn(): SignInResult {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                // Autenticação no Supabase usando o ID Token obtido do Credential Manager
                supabase.auth.signInWith(IDToken) {
                    idToken = googleIdTokenCredential.idToken
                    provider = Google
                }

                val supabaseUser = supabase.auth.currentUserOrNull()
                Log.d("SupabaseAuth", "==================================================")
                Log.d("SupabaseAuth", "LOGIN COM SUCESSO NO SUPABASE!")
                Log.d("SupabaseAuth", "User UUID: ${supabaseUser?.id}")
                Log.d("SupabaseAuth", "Email: ${supabaseUser?.email}")
                Log.d("SupabaseAuth", "==================================================")

                if (supabaseUser != null) {
                    SignInResult.Success(supabaseUser = supabaseUser)
                } else {
                    SignInResult.Error("Não foi possível obter dados do usuário autenticado no Supabase.")
                }
            } else {
                SignInResult.Error("Credencial retornada não é compatível com Google ID Token.")
            }
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Erro ao autenticar com o Google via Supabase: ${e.message}", e)
            SignInResult.Error(e.localizedMessage ?: "Erro desconhecido ao autenticar com o Google.")
        }
    }

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Erro ao deslogar do Supabase: ${e.message}")
        }
    }

    fun getSignedInUser(): UserInfo? = supabase.auth.currentUserOrNull()
}
