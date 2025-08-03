package com.example.chatgptapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.chatgptapp.data.model.Personality
import com.example.chatgptapp.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chat_preferences")

class PreferencesManager(private val context: Context) {
    private val IS_ONLINE_MODE = booleanPreferencesKey("is_online_mode")
    private val IS_CONNECTED = booleanPreferencesKey("is_connected")
    private val CURRENT_MODEL = stringPreferencesKey("current_model")
    private val PERSONALITY = stringPreferencesKey("personality")
    private val TEMPERATURE = floatPreferencesKey("temperature")
    private val MAX_TOKENS = intPreferencesKey("max_tokens")
    private val CUSTOM_INSTRUCTIONS = stringPreferencesKey("custom_instructions")
    
    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                isOnlineMode = preferences[IS_ONLINE_MODE] ?: true,
                isConnected = preferences[IS_CONNECTED] ?: true,
                currentModel = preferences[CURRENT_MODEL] ?: "default",
                personality = Personality.valueOf(preferences[PERSONALITY] ?: "FRIENDLY"),
                temperature = preferences[TEMPERATURE] ?: 0.7f,
                maxTokens = preferences[MAX_TOKENS] ?: 150,
                customInstructions = preferences[CUSTOM_INSTRUCTIONS] ?: ""
            )
        }
    
    suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        context.dataStore.edit { preferences ->
            val current = UserPreferences(
                isOnlineMode = preferences[IS_ONLINE_MODE] ?: true,
                isConnected = preferences[IS_CONNECTED] ?: true,
                currentModel = preferences[CURRENT_MODEL] ?: "default",
                personality = Personality.valueOf(preferences[PERSONALITY] ?: "FRIENDLY"),
                temperature = preferences[TEMPERATURE] ?: 0.7f,
                maxTokens = preferences[MAX_TOKENS] ?: 150,
                customInstructions = preferences[CUSTOM_INSTRUCTIONS] ?: ""
            )
            val updated = transform(current)
            
            preferences[IS_ONLINE_MODE] = updated.isOnlineMode
            preferences[IS_CONNECTED] = updated.isConnected
            preferences[CURRENT_MODEL] = updated.currentModel
            preferences[PERSONALITY] = updated.personality.name
            preferences[TEMPERATURE] = updated.temperature
            preferences[MAX_TOKENS] = updated.maxTokens
            preferences[CUSTOM_INSTRUCTIONS] = updated.customInstructions
        }
    }
    
    suspend fun getPreferences(): UserPreferences {
        val prefs = context.dataStore.data.first()
        return UserPreferences(
            isOnlineMode = prefs[IS_ONLINE_MODE] ?: true,
            isConnected = prefs[IS_CONNECTED] ?: true,
            currentModel = prefs[CURRENT_MODEL] ?: "default",
            personality = Personality.valueOf(prefs[PERSONALITY] ?: "FRIENDLY"),
            temperature = prefs[TEMPERATURE] ?: 0.7f,
            maxTokens = prefs[MAX_TOKENS] ?: 150,
            customInstructions = prefs[CUSTOM_INSTRUCTIONS] ?: ""
        )
    }
}
