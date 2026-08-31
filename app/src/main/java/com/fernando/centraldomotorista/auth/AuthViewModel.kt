package com.fernando.centraldomotorista.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.Profile
import com.fernando.centraldomotorista.data.preferences.AuthPreferences
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.ProfileRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

sealed class AuthState {
    object Loading : AuthState()
    object LoggedOut : AuthState()
    data class LoggedIn(val profile: Profile? = null, val supabaseUser: UserInfo? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    application: Application,
    private val profileRepository: ProfileRepository = ProfileRepository()
) : AndroidViewModel(application) {

    private val authPreferences = AuthPreferences(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val rememberedEmail: StateFlow<String> = authPreferences.rememberEmailFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val rememberMe: StateFlow<Boolean> = authPreferences.rememberMeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isBiometricEnabled: StateFlow<Boolean> = authPreferences.biometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isBiometricPrompted: StateFlow<Boolean> = authPreferences.biometricPromptedFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        checkCurrentUser()
    }

    fun checkCurrentUser() {
        val supabaseUser = supabase.auth.currentUserOrNull()

        if (supabaseUser != null) {
            Log.d("SupabaseAuth", "Sessão ativa encontrada no Supabase: UUID=${supabaseUser.id}")
            loadProfile(supabaseUser)
        } else {
            _authState.value = AuthState.LoggedOut
        }
    }

    fun signInWithEmail(
        email: String,
        pass: String,
        remember: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isBlank()) {
            val err = "Informe o seu e-mail."
            _authState.value = AuthState.Error(err)
            onError(err)
            return
        }
        if (cleanPass.isBlank()) {
            val err = "Informe a sua senha."
            _authState.value = AuthState.Error(err)
            onError(err)
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                authPreferences.saveRememberedEmail(cleanEmail, remember)
                supabase.auth.signInWith(Email) {
                    this.email = cleanEmail
                    this.password = cleanPass
                }
                val user = supabase.auth.currentUserOrNull()
                if (user != null) {
                    loadProfile(user)
                    onSuccess()
                } else {
                    val err = "Falha ao obter dados do usuário após login."
                    _authState.value = AuthState.Error(err)
                    onError(err)
                }
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "Erro no login com e-mail: ${e.message}", e)
                val friendlyError = mapAuthError(e)
                _authState.value = AuthState.Error(friendlyError)
                onError(friendlyError)
            }
        }
    }

    fun signUpWithEmail(
        email: String,
        pass: String,
        fullName: String,
        remember: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        val cleanName = fullName.trim()

        if (cleanEmail.isBlank()) {
            val err = "Informe o seu e-mail."
            onError(err)
            return
        }
        if (cleanPass.length < 6) {
            val err = "A senha deve ter pelo menos 6 caracteres."
            onError(err)
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                authPreferences.saveRememberedEmail(cleanEmail, remember)
                supabase.auth.signUpWith(Email) {
                    this.email = cleanEmail
                    this.password = cleanPass
                    this.data = buildJsonObject {
                        put("full_name", cleanName.ifBlank { "Motorista" })
                    }
                }
                val user = supabase.auth.currentUserOrNull()
                if (user != null) {
                    loadProfile(user)
                    onSuccess()
                } else {
                    _authState.value = AuthState.LoggedOut
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "Erro no cadastro com e-mail: ${e.message}", e)
                val friendlyError = mapAuthError(e)
                _authState.value = AuthState.Error(friendlyError)
                onError(friendlyError)
            }
        }
    }

    fun sendPasswordResetEmail(
        email: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            onResult(false, "Informe um e-mail válido.")
            return
        }

        viewModelScope.launch {
            try {
                supabase.auth.resetPasswordForEmail(cleanEmail)
                onResult(true, "Se esse e-mail estiver cadastrado, enviamos um link de recuperação para ele.")
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "Erro na recuperação de senha: ${e.message}", e)
                onResult(true, "Se esse e-mail estiver cadastrado, enviamos um link de recuperação para ele.")
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            authPreferences.setBiometricEnabled(enabled)
        }
    }

    fun handleSignInSuccess(supabaseUser: UserInfo?) {
        val sUser = supabaseUser ?: supabase.auth.currentUserOrNull()
        Log.d("SupabaseAuth", "handleSignInSuccess: UUID=${sUser?.id} | Email=${sUser?.email}")
        if (sUser != null) {
            loadProfile(sUser)
        }
    }

    fun handleSignInError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.LoggedOut
        }
    }

    private fun loadProfile(user: UserInfo) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val fullName = user.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull
                    ?: user.userMetadata?.get("name")?.jsonPrimitive?.contentOrNull
                    ?: user.email
                val avatarUrl = user.userMetadata?.get("avatar_url")?.jsonPrimitive?.contentOrNull
                    ?: user.userMetadata?.get("picture")?.jsonPrimitive?.contentOrNull

                val profile = profileRepository.createOrFetchProfile(
                    userId = user.id,
                    email = user.email,
                    fullName = fullName,
                    avatarUrl = avatarUrl
                )
                _authState.value = AuthState.LoggedIn(profile = profile, supabaseUser = user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Erro ao recuperar perfil: ${e.localizedMessage}")
            }
        }
    }

    fun signOut(googleAuthClient: GoogleAuthClient? = null) {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "Erro ao deslogar do Supabase: ${e.message}")
            }
            googleAuthClient?.signOut()
            _authState.value = AuthState.LoggedOut
        }
    }

    private fun mapAuthError(e: Exception): String {
        val msg = e.message.orEmpty().lowercase()
        return when {
            msg.contains("invalid login credentials") || msg.contains("invalid_grant") || msg.contains("invalid credentials") ->
                "E-mail ou senha inválidos."
            msg.contains("user already registered") || msg.contains("already exists") ->
                "Este e-mail já está cadastrado."
            msg.contains("password should be at least") ->
                "A senha deve ter no mínimo 6 caracteres."
            msg.contains("rate limit") || msg.contains("too many requests") ->
                "Muitas tentativas. Aguarde um momento e tente novamente."
            msg.contains("email not confirmed") ->
                "E-mail não confirmado. Verifique sua caixa de entrada."
            msg.contains("network") || msg.contains("timeout") || msg.contains("host") || msg.contains("connect") ->
                "Falha de conexão. Verifique sua internet."
            else -> e.localizedMessage ?: "Ocorreu um erro ao processar sua solicitação."
        }
    }
}
