package com.umbranexus.unaiv2.ai

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GroqAssistant(private val apiKey: String) : AiAssistant {
    private val service: UmbraNexusService = Retrofit.Builder()
        .baseUrl("https://api.groq.com/openai/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(UmbraNexusService::class.java)

    override suspend fun ask(prompt: String): String {
        if (!apiKey.startsWith("gsk_")) {
            return "Uplink Restricted: No valid Groq API key detected. Please configure your gsk_ key in System Settings."
        }
        return try {
            val response = service.getChatCompletion(
                authHeader = "Bearer $apiKey",
                request = ChatRequest(
                    model = "llama-3.1-70b-versatile",
                    messages = listOf(Message(role = "user", content = prompt))
                )
            )
            response.choices.firstOrNull()?.message?.content ?: "No response from Groq."
        } catch (e: Exception) {
            val message = e.message ?: ""
            if (message.contains("401")) {
                "Authentication Error (401): Groq key rejected. Verify your gsk_ key."
            } else if (message.contains("429")) {
                "System Overload: Groq rate limit reached. Please wait."
            } else {
                "Error: $message"
            }
        }
    }
}
