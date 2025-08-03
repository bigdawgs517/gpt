package com.example.chatgptapp.data.model

data class ChatMessage(
    val id: Long,
    val content: String,
    val role: MessageRole,
    val timestamp: Long
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}
