package com.fernando.centraldomotorista.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.Profile
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object LoggedOut : AuthState()
    data class LoggedIn(val user: FirebaseUser? = null, val profile: Profile? = null, val supabaseUser: UserInfo? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    fun checkCurrentUser() {
        val supabaseUser = supabase.auth.currentUserOrNull()
        val currentUser = firebaseAuth.currentUser

        if (supabaseUser != null) {
            Log.d("SupabaseAuth", "Sessão ativa encontrada no Supabase: UUID=${supabaseUser.id}")
            _authState.value = AuthState.LoggedIn(supabaseUser = supabaseUser)
        } else if (currentUser != null) {
            loadProfile(currentUser)
        } else {
            _authState.value = AuthState.LoggedOut
        }
    }

    fun handleSignInSuccess(supabaseUser: UserInfo?, user: FirebaseUser? = null) {
        val sUser = supabaseUser ?: supabase.auth.currentUserOrNull()
        Log.d("SupabaseAuth", "handleSignInSuccess: UUID=${sUser?.id} | Email=${sUser?.email}")
        if (sUser != null) {
            _authState.value = AuthState.LoggedIn(user = user, profile = null, supabaseUser = sUser)
        } else if (user != null) {
            loadProfile(user)
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

    private fun loadProfile(user: FirebaseUser) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val profile = profileRepository.createOrFetchProfile(
                    userId = user.uid,
                    email = user.email,
                    fullName = user.displayName,
                    avatarUrl = user.photoUrl?.toString()
                )
                _authState.value = AuthState.LoggedIn(user, profile)
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
        }
        googleAuthClient?.let {
            viewModelScope.launch { it.signOut() }
        } ?: firebaseAuth.signOut()
        _authState.value = AuthState.LoggedOut
    }
}
