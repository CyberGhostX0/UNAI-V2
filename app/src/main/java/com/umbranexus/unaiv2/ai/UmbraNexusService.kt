package com.umbranexus.unaiv2.ai

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface UmbraNexusService {
    @POST("api/chat")
    suspend fun getChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: ChatRequest
    ): ChatResponse

    @Streaming
    @POST("api/voice")
    suspend fun generateSpeech(
        @Header("Authorization") authHeader: String,
        @Body request: SpeechRequest
    ): Response<ResponseBody>
}

data class SpeechRequest(
    val model: String = "tts-1",
    val input: String,
    val voice: String = "nova"
)

data class ChatRequest(
    val model: String = "umbra-nexus-core-v2",
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
