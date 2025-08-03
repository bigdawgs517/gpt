package com.example.chatgptapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatgptapp.data.model.ChatMessage
import com.example.chatgptapp.data.model.MessageRole
import com.example.chatgptapp.data.model.Personality
import com.example.chatgptapp.data.model.PromptTemplate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
    private val preferencesManager: PreferencesManager,
    private val promptEngine: PromptEngine
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    private val _availablePrompts = MutableStateFlow<List<PromptTemplate>>(emptyList())
    val availablePrompts: StateFlow<List<PromptTemplate>> = _availablePrompts.asStateFlow()
    
    init {
        loadChatHistory()
        observePreferences()
        loadPromptTemplates()
    }
    
    private fun observePreferences() {
        viewModelScope.launch {
            preferencesManager.preferencesFlow.collect { prefs ->
                _uiState.update { it.copy(
                    isOnlineMode = prefs.isOnlineMode,
                    isConnected = prefs.isConnected,
                    currentModel = prefs.currentModel,
                    personality = prefs.personality,
                    temperature = prefs.temperature,
                    maxTokens = prefs.maxTokens
                ) }
            }
        }
    }
    
    private fun loadChatHistory() {
        viewModelScope.launch {
            repository.getChatHistory().collect { history ->
                _messages.value = history
            }
        }
    }
    
    private fun loadPromptTemplates() {
        viewModelScope.launch {
            _availablePrompts.value = promptEngine.getAvailableTemplates()
        }
    }
    
    fun sendMessage(message: String, promptTemplate: PromptTemplate? = null) {
        if (message.isBlank()) return
        
        viewModelScope.launch {
            _isSending.value = true
            
            // Add user message to UI immediately
            val userMessage = ChatMessage(
                id = System.currentTimeMillis(),
                content = message,
                role = MessageRole.USER,
                timestamp = System.currentTimeMillis()
            )
            _messages.value = _messages.value + userMessage
            
            try {
                // Apply prompt engineering
                val enhancedPrompt = if (promptTemplate != null) {
                    promptEngine.applyTemplate(message, promptTemplate, _messages.value)
                } else {
                    promptEngine.enhancePrompt(message, _messages.value)
                }
                
                val response = if (shouldUseApi()) {
                    repository.sendApiMessage(enhancedPrompt)
                } else {
                    repository.sendOfflineMessage(enhancedPrompt)
                }
                
                // Add response to UI
                _messages.value = _messages.value + response
                
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isSending.value = false
            }
        }
    }
    
    private fun shouldUseApi(): Boolean {
        val state = _uiState.value
        return state.isOnlineMode && state.isConnected
    }
    
    private fun handleError(exception: Exception) {
        val errorMessage = ChatMessage(
            id = System.currentTimeMillis(),
            content = "Error: ${exception.message ?: "Unknown error"}",
            role = MessageRole.SYSTEM,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + errorMessage
    }
    
    fun toggleMode() {
        viewModelScope.launch {
            val currentMode = preferencesManager.getPreferences().isOnlineMode
            preferencesManager.updatePreferences { it.copy(isOnlineMode = !currentMode) }
        }
    }
    
    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
            _messages.value = emptyList()
        }
    }
    
    fun updatePersonality(personality: Personality) {
        viewModelScope.launch {
            preferencesManager.updatePreferences { it.copy(personality = personality) }
        }
    }
    
    fun updateTemperature(temperature: Float) {
        viewModelScope.launch {
            preferencesManager.updatePreferences { it.copy(temperature = temperature) }
        }
    }
    
    fun updateMaxTokens(maxTokens: Int) {
        viewModelScope.launch {
            preferencesManager.updatePreferences { it.copy(maxTokens = maxTokens) }
        }
    }
}

data class ChatUiState(
    val isOnlineMode: Boolean = true,
    val isConnected: Boolean = true,
    val currentModel: String = "default",
    val isModelLoaded: Boolean = false,
    val isLoadingModel: Boolean = false,
    val personality: Personality = Personality.FRIENDLY,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 150
)
