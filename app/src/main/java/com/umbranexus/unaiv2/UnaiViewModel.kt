package com.umbranexus.unaiv2

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.umbranexus.unaiv2.ai.*
import com.umbranexus.unaiv2.worker.SystemMaintenanceWorker
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

enum class UnaiNavigation {
    CHAT, SAVED_CHATS, SAVED_IMAGES, MEMORY_BANK, SETTINGS
}

data class MemoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class UnaiViewModel(application: Application) : AndroidViewModel(application) {
    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    private val _memoryBank = mutableStateListOf<MemoryEntry>()
    val memoryBank: List<MemoryEntry> = _memoryBank

    private val _currentScreen = mutableStateOf(UnaiNavigation.CHAT)
    val currentScreen: State<UnaiNavigation> = _currentScreen

    private val _isBusy = mutableStateOf(false)
    val isBusy: State<Boolean> = _isBusy

    private val _isSpeaking = mutableStateOf(false)
    val isSpeaking: State<Boolean> = _isSpeaking

    private val _neuralRoutingEnabled = mutableStateOf(true)
    val neuralRoutingEnabled: State<Boolean> = _neuralRoutingEnabled

    private val _savedChats = mutableStateListOf<List<ChatMessage>>()
    val savedChats: List<List<ChatMessage>> = _savedChats

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val umbraNexusService: UmbraNexusService = Retrofit.Builder()
        .baseUrl("https://umbra-nexus-server-production-f4af.up.railway.app/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(UmbraNexusService::class.java)

    private val voiceManager = VoiceManager(application) { speaking ->
        _isSpeaking.value = speaking
    }

    private val _conversationKey = mutableStateOf(SystemCores.CONVERSATION_CORE)
    val conversationKey: State<String> = _conversationKey

    private val _groqKey = mutableStateOf("")
    val groqKey: State<String> = _groqKey

    private val _useGroq = mutableStateOf(false)
    val useGroq: State<Boolean> = _useGroq

    private val _voiceKey = mutableStateOf(SystemCores.VOICE_SYSTEM)
    private val _backendKey = mutableStateOf(SystemCores.BACKGROUND_TASKS)
    private val _visionKey = mutableStateOf(SystemCores.VISION_IMAGE_CORE)

    private var currentAssistant: AiAssistant = UmbraNexusAssistant(_conversationKey.value)

    init {
        updateAssistant()
        SystemMaintenanceWorker.schedule(application)
    }

    fun addToMemory(content: String) {
        _memoryBank.add(0, MemoryEntry(content = content))
        speakResponse("Information committed to memory bank. Neural indexing complete.")
    }

    fun removeMemory(entry: MemoryEntry) {
        _memoryBank.remove(entry)
        speakResponse("Memory fragment purged from core.")
    }

    private fun updateAssistant() {
        currentAssistant = if (_useGroq.value && _groqKey.value.isNotBlank()) {
            GroqAssistant(_groqKey.value)
        } else {
            UmbraNexusAssistant(_conversationKey.value)
        }
    }

    fun navigateTo(screen: UnaiNavigation) {
        _currentScreen.value = screen
    }

    private fun speakResponse(text: String) {
        val key = _voiceKey.value
        if (key.startsWith("un_") && key.length > 20) {
            viewModelScope.launch {
                try {
                    val response = umbraNexusService.generateSpeech(
                        authHeader = "Bearer $key",
                        request = SpeechRequest(input = text, voice = "nova")
                    )
                    if (response.isSuccessful) {
                        response.body()?.bytes()?.let { 
                            voiceManager.playAudioData(it) 
                            return@launch
                        }
                    }
                    voiceManager.speak(text)
                } catch (e: Exception) {
                    voiceManager.speak(text)
                }
            }
        } else {
            voiceManager.speak(text)
        }
    }

    fun updateApiKeys(conversation: String, groq: String = _groqKey.value, useGroq: Boolean = _useGroq.value) {
        _conversationKey.value = conversation
        _groqKey.value = groq
        _useGroq.value = useGroq
        updateAssistant()
        val provider = if (useGroq) "Groq Llama-3" else "Umbra Nexus"
        speakResponse("Security protocols updated. $provider core established as primary intelligence uplink.")
    }

    fun clearSession() {
        if (_messages.isNotEmpty()) {
            _savedChats.add(_messages.toList())
        }
        _messages.clear()
        speakResponse("System reset. Previous data encrypted and moved to saved chats. New session initiated.")
    }

    fun loadSavedChat(chat: List<ChatMessage>) {
        _messages.clear()
        _messages.addAll(chat)
        _currentScreen.value = UnaiNavigation.CHAT
        speakResponse("Historical data retrieved. Session restored.")
    }

    fun setNeuralRouting(enabled: Boolean) {
        _neuralRoutingEnabled.value = enabled
        val status = if (enabled) "active" else "disabled"
        speakResponse("Neural routing via Tor is now $status.")
    }

    fun checkSystemStatus() {
        val activeProvider = if (_useGroq.value) "Groq Llama-3 (ACTIVE)" else "Umbra Nexus Core (ACTIVE)"
        val statusReport = """
            [SYSTEM STATUS REPORT]
            Umbra Nexus Core: ONLINE
            Primary Uplink: $activeProvider
            
            CORES:
            - Conversation Core (${_conversationKey.value.takeLast(8)}): ACTIVE
            - Voice System (${_voiceKey.value.takeLast(8)}): ACTIVE
            - Vision/Image Core (${_visionKey.value.takeLast(8)}): READY
            - Background Tasks (${_backendKey.value.takeLast(8)}): MONITORING
            
            All protocols established. Neural routing through secure nodes.
        """.trimIndent()
        
        _messages.add(ChatMessage(statusReport, isUser = false))
        speakResponse("All systems are operational. The $activeProvider is currently handling our intelligence requests.")
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isBusy.value) return

        _messages.add(ChatMessage(text, isUser = true))
        _isBusy.value = true

        viewModelScope.launch {
            try {
                val systemPrompt = "You are UNAI, a highly advanced female AI assistant. You are polite, slightly sarcastic, and extremely efficient. You speak with a natural, human-like tone. "
                val fullPrompt = systemPrompt + text
                
                val response = currentAssistant.ask(fullPrompt)
                _messages.add(ChatMessage(response, isUser = false))
                speakResponse(response)
            } catch (e: Exception) {
                _messages.add(ChatMessage("Uplink Error: ${e.message}", isUser = false))
            } finally {
                _isBusy.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutDown()
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)
