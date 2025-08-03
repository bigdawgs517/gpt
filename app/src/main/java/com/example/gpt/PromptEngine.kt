package com.example.chatgptapp

import android.content.Context
import com.example.chatgptapp.data.model.ChatMessage
import com.example.chatgptapp.data.model.MessageRole
import com.example.chatgptapp.data.model.Personality
import com.example.chatgptapp.data.model.PromptTemplate
import kotlinx.coroutines.runBlocking
import java.io.File

class PromptEngine(private val context: Context, private val preferencesManager: PreferencesManager) {
    private val templatesDir = File(context.getExternalFilesDir(null), "prompts")
    
    init {
        if (!templatesDir.exists()) {
            templatesDir.mkdirs()
        }
        initializeDefaultTemplates()
    }
    
    fun enhancePrompt(userMessage: String, conversationHistory: List<ChatMessage>): String {
        val preferences = runBlocking { preferencesManager.getPreferences() }
        val personalityPrompt = getPersonalityPrompt(preferences.personality)
        val contextPrompt = buildContextPrompt(conversationHistory)
        
        return """
            $personalityPrompt
            $contextPrompt
            User: $userMessage
            Assistant:
        """.trimIndent()
    }
    
    fun applyTemplate(userMessage: String, template: PromptTemplate, conversationHistory: List<ChatMessage>): String {
        val preferences = runBlocking { preferencesManager.getPreferences() }
        val personalityPrompt = getPersonalityPrompt(preferences.personality)
        val contextPrompt = buildContextPrompt(conversationHistory)
        
        return template.template
            .replace("{PERSONALITY}", personalityPrompt)
            .replace("{CONTEXT}", contextPrompt)
            .replace("{USER_MESSAGE}", userMessage)
            .replace("{CURRENT_DATE}", System.currentTimeMillis().toString())
    }
    
    private fun getPersonalityPrompt(personality: Personality): String {
        return when (personality) {
            Personality.FRIENDLY -> "You are a friendly and helpful assistant."
            Personality.PROFESSIONAL -> "You are a professional and concise assistant."
            Personality.CREATIVE -> "You are a creative and imaginative assistant."
            Personality.TECHNICAL -> "You are a technical expert who provides detailed explanations."
            Personality.HUMOROUS -> "You are a witty assistant who uses humor appropriately."
        }
    }
    
    private fun buildContextPrompt(conversationHistory: List<ChatMessage>): String {
        if (conversationHistory.isEmpty()) return ""
        
        val recentMessages = conversationHistory.takeLast(5) // Last 5 messages for context
        val context = recentMessages.joinToString("\n") { message ->
            when (message.role) {
                MessageRole.USER -> "User: ${message.content}"
                MessageRole.ASSISTANT -> "Assistant: ${message.content}"
                MessageRole.SYSTEM -> "System: ${message.content}"
            }
        }
        
        return "Context:\n$context"
    }
    
    fun getAvailableTemplates(): List<PromptTemplate> {
        val defaultTemplates = getDefaultTemplates()
        return defaultTemplates
    }
    
    private fun initializeDefaultTemplates() {
        // In a real app, you would save templates to files
    }
    
    private fun getDefaultTemplates(): List<PromptTemplate> {
        return listOf(
            PromptTemplate(
                id = "summarize",
                name = "Summarize",
                description = "Summarize the given text concisely",
                template = """
                    {PERSONALITY}
                    Please summarize the following text in 2-3 sentences:
                    {USER_MESSAGE}
                    Summary:
                """.trimIndent()
            ),
            PromptTemplate(
                id = "explain",
                name = "Explain Concept",
                description = "Explain a concept in simple terms",
                template = """
                    {PERSONALITY}
                    Explain the following concept in simple terms that a beginner could understand:
                    {USER_MESSAGE}
                    Explanation:
                """.trimIndent()
            ),
            PromptTemplate(
                id = "creative_story",
                name = "Creative Story",
                description = "Generate a creative story",
                template = """
                    {PERSONALITY}
                    Write a creative short story based on the following prompt:
                    {USER_MESSAGE}
                    Story:
                """.trimIndent()
            )
        )
    }
}
