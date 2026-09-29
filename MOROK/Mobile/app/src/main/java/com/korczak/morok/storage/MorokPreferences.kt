package com.korczak.morok.storage
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
private val Context.morokDataStore by preferencesDataStore("morok_preferences")
class MorokPreferences(private val context: Context) {
    private val onboardingKey = booleanPreferencesKey("onboarding_complete")
    val onboardingComplete: Flow<Boolean> = context.morokDataStore.data.map { it[onboardingKey] ?: false }
    suspend fun setOnboardingComplete(value: Boolean) { context.morokDataStore.edit { it[onboardingKey] = value } }
}
