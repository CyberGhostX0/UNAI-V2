package com.umbranexus.unaiv2

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.io.FileOutputStream
import java.util.*

class VoiceManager(private val context: Context, private val onSpeakingStatusChanged: (Boolean) -> Unit = {}) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false
    private var mediaPlayer: MediaPlayer? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language to UK English as base
            val result = tts.setLanguage(Locale.UK)
            
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                // Non-Google workaround: Search for female indicators across any installed engine (Samsung, etc.)
                val voices = tts.voices
                if (voices != null) {
                    val femaleVoice = voices
                        .filter { it.locale.language == "en" }
                        .find { voice ->
                            val name = voice.name.lowercase()
                            name.contains("female") || 
                            name.contains("woman") || 
                            name.contains("f-") || 
                            name.contains("_f_") ||
                            name.contains("nova") ||
                            name.contains("shimmer")
                        } ?: voices.find { it.name.lowercase().contains("network") }

                    femaleVoice?.let { 
                        tts.voice = it
                    }
                }
            }

            tts.setPitch(1.05f) 
            tts.setSpeechRate(0.95f) 
            isReady = true
            
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeakingStatusChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    onSpeakingStatusChanged(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeakingStatusChanged(false)
                }
            })
        }
    }

    fun speak(text: String) {
        if (isReady) {
            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "unai_speech")
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "unai_speech")
        }
    }

    /**
     * Play high-quality human voice from byte array (e.g., Umbra Nexus TTS)
     */
    fun playAudioData(data: ByteArray) {
        try {
            stopSpeaking()
            val tempFile = File.createTempFile("unai_voice", "mp3", context.cacheDir)
            tempFile.deleteOnExit()
            val fos = FileOutputStream(tempFile)
            fos.write(data)
            fos.close()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    onSpeakingStatusChanged(false)
                    tempFile.delete()
                }
                start()
                onSpeakingStatusChanged(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopSpeaking() {
        if (tts.isSpeaking) tts.stop()
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        onSpeakingStatusChanged(false)
    }

    fun shutDown() {
        stopSpeaking()
        tts.shutdown()
    }
}
