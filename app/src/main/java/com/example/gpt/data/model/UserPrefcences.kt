package com.example.chatgptapp.data.model

data class UserPreferences(
    val isOnlineMode: Boolean,
    val isConnected: Boolean,
    val currentModel: String,
    val personality: Personality,
    val temperature: Float,
    val maxTokens: Int,
    val customInstructions: String
)
