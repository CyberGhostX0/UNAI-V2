package com.umbranexus.unaiv2.ai

interface AiAssistant {
    suspend fun ask(prompt: String): String
}
