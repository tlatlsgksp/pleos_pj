package com.example.pleos.domain.gateway

import com.example.pleos.domain.model.Transcript
import kotlinx.coroutines.flow.Flow

/** Speech to text. Collecting the flow starts recognition; cancelling it stops. */
interface SpeechInput {
    fun listen(): Flow<Transcript>
}

/** Text to speech. */
interface SpeechOutput {
    suspend fun speak(text: String)

    fun stop()
}
