package com.fernando.centraldomotorista.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

sealed class SignInResult {
    data class Success(val user: FirebaseUser) : SignInResult()
    data class Error(val message: String) : SignInResult()
    object Cancelled : SignInResult()
}

class GoogleAuthClient(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val credentialManager = CredentialManager.create(context)

    companion object {
        /**
         * Web Client ID do Firebase.
         * IMPORTANTE: Copie este valor no Firebase Console:
         * Authentication > Sign-in method > Google > Web SDK configuration > Web client ID
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
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    SignInResult.Success(user)
                } else {
                    SignInResult.Error("Não foi possível obter dados do usuário autenticado.")
                }
            } else {
                SignInResult.Error("Credencial retornada não é compatível com Google ID Token.")
            }
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: Exception) {
            SignInResult.Error(e.localizedMessage ?: "Erro desconhecido ao autenticar com o Google.")
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    fun getSignedInUser(): FirebaseUser? = firebaseAuth.currentUser
}
