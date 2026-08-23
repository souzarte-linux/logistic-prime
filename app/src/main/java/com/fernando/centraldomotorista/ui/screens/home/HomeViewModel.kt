package com.fernando.centraldomotorista.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.Profile
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.ProfileApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class NeonTestState {
    object Idle : NeonTestState()
    object Loading : NeonTestState()
    data class Success(val profile: Profile?, val rawJsonCount: Int) : NeonTestState()
    data class Error(val message: String) : NeonTestState()
}

class HomeViewModel(
    private val profileApi: ProfileApi = RetrofitClient.profileApi,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _testState = MutableStateFlow<NeonTestState>(NeonTestState.Idle)
    val testState: StateFlow<NeonTestState> = _testState.asStateFlow()

    init {
        testFetchProfile()
    }

    fun testFetchProfile() {
        val user = firebaseAuth.currentUser
        if (user == null) {
            Log.w("NeonDataApi", "Nenhum usuário logado no Firebase para testar o Neon Data API.")
            _testState.value = NeonTestState.Error("Usuário não autenticado no Firebase.")
            return
        }

        _testState.value = NeonTestState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("NeonDataApi", "=== INICIANDO TESTE DE SANIDADE NO NEON DATA API ===")
                val dtoList = profileApi.getProfile("eq.${user.uid}")
                var profile = dtoList.firstOrNull()?.toDomain()
                if (profile != null) {
                    Log.d("NeonDataApi", ">>> DADOS DO PERFIL ENCONTRADO: $profile")
                } else {
                    Log.d("NeonDataApi", ">>> Perfil não encontrado. Criando perfil inicial no Neon Data API...")
                    val initialDto = com.fernando.centraldomotorista.data.remote.dto.ProfileDto(
                        id = user.uid,
                        email = user.email,
                        fullName = user.displayName,
                        avatarUrl = user.photoUrl?.toString(),
                        dailyGoal = java.math.BigDecimal("200"),
                        weeklyGoal = java.math.BigDecimal("1000"),
                        monthlyGoal = java.math.BigDecimal("3450"),
                        vehicle = "moto",
                        hasBag = false
                    )
                    val createdList = profileApi.createProfile(initialDto)
                    profile = createdList.firstOrNull()?.toDomain() ?: initialDto.toDomain()
                    Log.d("NeonDataApi", ">>> PERFIL CRIADO COM SUCESSO NO BANCO NEON: $profile")
                }
                Log.d("NeonDataApi", "=== TESTE DE SANIDADE CONCLUÍDO COM SUCESSO ===")

                withContext(Dispatchers.Main) {
                    _testState.value = NeonTestState.Success(profile, if (dtoList.isEmpty()) 1 else dtoList.size)
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                Log.e("NeonDataApi", "!!! ERRO HTTP ${e.code()}: $errorBody", e)
                withContext(Dispatchers.Main) {
                    _testState.value = NeonTestState.Error("HTTP ${e.code()}: ${errorBody ?: e.message()}")
                }
            } catch (e: Exception) {
                Log.e("NeonDataApi", "!!! ERRO AO CONSULTAR NEON DATA API: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _testState.value = NeonTestState.Error("Erro: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }
}
