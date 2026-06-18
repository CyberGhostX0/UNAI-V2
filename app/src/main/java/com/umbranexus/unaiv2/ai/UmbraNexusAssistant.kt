package com.umbranexus.unaiv2.ai

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UmbraNexusAssistant(private val apiKey: String) : AiAssistant {
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val service: UmbraNexusService = Retrofit.Builder()
        .baseUrl("https://umbra-nexus-server-production-f4af.up.railway.app/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(UmbraNexusService::class.java)

    override suspend fun ask(prompt: String): String {
        if (!apiKey.startsWith("un_")) {
            return "Uplink Restricted: No valid Umbra Nexus API key detected. Please configure your primary key in System Settings to establish a connection with the intelligence core."
        }
        return try {
            executeRequest(apiKey, prompt)
        } catch (e: Exception) {
            val message = e.message ?: ""
            if (message.contains("401")) {
                "Authentication Error (401): The established uplink key was rejected by the server. Please verify your Umbra Nexus API key in Settings."
            } else if (message.contains("429")) {
                "System Overload: Umbra Nexus rate limit reached (HTTP 429). Please check your account quota."
            } else {
                "Error: $message"
            }
        }
    }

    private suspend fun executeRequest(key: String, prompt: String): String {
        val response = service.getChatCompletion(
            authHeader = "Bearer $key",
            request = ChatRequest(
                messages = listOf(Message(role = "user", content = prompt))
            )
        )
        return response.choices.firstOrNull()?.message?.content ?: "No response from Umbra Nexus Core."
    }
}
