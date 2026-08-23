package com.fernando.centraldomotorista.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.Profile
import com.fernando.centraldomotorista.data.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object LoggedOut : AuthState()
    data class LoggedIn(val user: FirebaseUser, val profile: Profile) : AuthState()
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
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            loadProfile(currentUser)
        } else {
            _authState.value = AuthState.LoggedOut
        }
    }

    fun handleSignInSuccess(user: FirebaseUser) {
        loadProfile(user)
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
        googleAuthClient?.signOut() ?: firebaseAuth.signOut()
        _authState.value = AuthState.LoggedOut
    }
}
