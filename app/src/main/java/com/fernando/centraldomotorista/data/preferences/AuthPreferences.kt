package com.fernando.centraldomotorista.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

class AuthPreferences(private val context: Context) {
    companion object {
        val REMEMBER_EMAIL_KEY = stringPreferencesKey("remember_email")
        val REMEMBER_ME_KEY = booleanPreferencesKey("remember_me")
        val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
        val BIOMETRIC_PROMPTED_KEY = booleanPreferencesKey("biometric_prompted")
    }

    val rememberEmailFlow: Flow<String> = context.authDataStore.data.map { preferences ->
        preferences[REMEMBER_EMAIL_KEY] ?: ""
    }

    val rememberMeFlow: Flow<Boolean> = context.authDataStore.data.map { preferences ->
        preferences[REMEMBER_ME_KEY] ?: false
    }

    val biometricEnabledFlow: Flow<Boolean> = context.authDataStore.data.map { preferences ->
        preferences[BIOMETRIC_ENABLED_KEY] ?: false
    }

    val biometricPromptedFlow: Flow<Boolean> = context.authDataStore.data.map { preferences ->
        preferences[BIOMETRIC_PROMPTED_KEY] ?: false
    }

    suspend fun saveRememberedEmail(email: String, remember: Boolean) {
        context.authDataStore.edit { preferences ->
            preferences[REMEMBER_ME_KEY] = remember
            if (remember) {
                preferences[REMEMBER_EMAIL_KEY] = email
            } else {
                preferences.remove(REMEMBER_EMAIL_KEY)
            }
        }
    }

    suspend fun clearRememberedEmail() {
        context.authDataStore.edit { preferences ->
            preferences.remove(REMEMBER_EMAIL_KEY)
            preferences[REMEMBER_ME_KEY] = false
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.authDataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] = enabled
            preferences[BIOMETRIC_PROMPTED_KEY] = true
        }
    }
}
