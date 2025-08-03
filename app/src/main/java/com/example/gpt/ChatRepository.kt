package com.example.chatgptapp

import com.example.chatgptapp.data.ChatDatabase
import com.example.chatgptapp.data.model.ChatMessage
import com.example.chatgptapp.data.model.MessageRole
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val database: ChatDatabase,
    private val apiService: ChatApiService,
    private val modelManager: ModelManager
) {
    
    fun getChatHistory(): Flow<List<ChatMessage>> {
        return database.chatDao().getAllMessages()
    }
    
    suspend fun sendApiMessage(message: String): ChatMessage {
        // In a real app, you would make an API call here
        // For this example, we'll simulate a response
        return ChatMessage(
            id = System.currentTimeMillis(),
            content = "This is a simulated API response to: $message",
            role = MessageRole.ASSISTANT,
            timestamp = System.currentTimeMillis()
        )
    }
    
    suspend fun sendOfflineMessage(message: String): ChatMessage {
        val model = modelManager.loadModel("default") ?: throw Exception("Model not loaded")
        val response = model.infer(message)
        
        return ChatMessage(
            id = System.currentTimeMillis(),
            content = response,
            role = MessageRole.ASSISTANT,
            timestamp = System.currentTimeMillis()
        )
    }
    
    suspend fun clearChatHistory() {
        database.chatDao().clearAllMessages()
    }
}
