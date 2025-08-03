package com.example.chatgptapp

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatgptapp.data.ChatDatabase
import com.example.chatgptapp.data.model.ChatMessage
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var promptTemplatesContainer: LinearLayout
    private lateinit var chatAdapter: ChatAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        promptTemplatesContainer = findViewById(R.id.promptTemplatesContainer)
        
        // Setup RecyclerView
        chatAdapter = ChatAdapter()
        chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = chatAdapter
        }
        
        // Setup send button
        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                viewModel.sendMessage(message)
                messageInput.setText("")
            }
        }
        
        // Observe messages
        lifecycleScope.launch {
            viewModel.messages.collect { messages ->
                chatAdapter.submitList(messages)
                chatRecyclerView.scrollToPosition(messages.size - 1)
            }
        }
        
        // Observe sending state
        lifecycleScope.launch {
            viewModel.isSending.collect { isSending ->
                sendButton.isEnabled = !isSending
                if (isSending) {
                    sendButton.alpha = 0.5f
                } else {
                    sendButton.alpha = 1.0f
                }
            }
        }
        
        // Setup prompt templates
        setupPromptTemplates()
    }
    
    private fun setupPromptTemplates() {
        lifecycleScope.launch {
            viewModel.availablePrompts.collect { templates ->
                promptTemplatesContainer.removeAllViews()
                templates.take(5).forEach { template ->
                    val chip = layoutInflater.inflate(R.layout.item_prompt_chip, promptTemplatesContainer, false) as Chip
                    chip.text = template.name
                    chip.setOnClickListener {
                        val message = messageInput.text.toString().trim()
                        if (message.isNotEmpty()) {
                            viewModel.sendMessage(message, template)
                            messageInput.setText("")
                        }
                    }
                    promptTemplatesContainer.addView(chip)
                }
            }
        }
    }
}

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
    
    private var messages: List<ChatMessage> = emptyList()
    
    fun submitList(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }
    
    override fun getItemCount(): Int = messages.size
    
    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val roleText: TextView = itemView.findViewById(R.id.roleText)
        private val contentText: TextView = itemView.findViewById(R.id.contentText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        
        fun bind(message: ChatMessage) {
            roleText.text = when (message.role) {
                com.example.chatgptapp.data.model.MessageRole.USER -> "You"
                com.example.chatgptapp.data.model.MessageRole.ASSISTANT -> "Assistant"
                com.example.chatgptapp.data.model.MessageRole.SYSTEM -> "System"
            }
            
            contentText.text = message.content
            
            // Format timestamp
            val date = java.util.Date(message.timestamp)
            val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            timestampText.text = format.format(date)
        }
    }
}
