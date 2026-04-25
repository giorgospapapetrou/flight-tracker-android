package com.giorgospapapetrou.flightfinder.data.api

import com.giorgospapapetrou.flightfinder.BuildConfig
import com.giorgospapapetrou.flightfinder.data.api.dto.AircraftDto
import com.giorgospapapetrou.flightfinder.data.api.dto.AircraftRemovedDto
import com.giorgospapapetrou.flightfinder.data.api.dto.StreamEventDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the live WebSocket. Exposes a Flow of raw JSON events.
 * Auto-reconnects with exponential backoff on disconnect.
 */
@Singleton
class LiveStreamClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    /**
     * Returns an infinite flow of events. Emits Connected/Disconnected
     * along with each parsed event from the server.
     *
     * Cold flow: starts the WebSocket only when collected.
     * Cancelling the collection closes the WebSocket.
     */
    fun events(): Flow<StreamEventEnvelope> = flow {
        var backoffMs = 1_000L
        while (true) {
            try {
                connectOnce().collect { envelope ->
                    if (envelope is StreamEventEnvelope.Connected) {
                        backoffMs = 1_000L  // reset on successful connect
                    }
                    emit(envelope)
                }
            } catch (t: Throwable) {
                Timber.w(t, "WebSocket session ended")
                emit(StreamEventEnvelope.Disconnected)
            }
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
        }
    }.flowOn(Dispatchers.IO)

    private fun connectOnce(): Flow<StreamEventEnvelope> = callbackFlow {
        val wsUrl = BuildConfig.API_BASE_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/') + "/api/v1/stream"

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer ${BuildConfig.API_KEY}")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.i("WebSocket opened")
                trySend(StreamEventEnvelope.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val event = json.decodeFromString(StreamEventDto.serializer(), text)
                    when (event.type) {
                        "position_update" -> {
                            val data = event.data ?: return
                            val dto = json.decodeFromJsonElement(
                                AircraftDto.serializer(), data
                            )
                            trySend(StreamEventEnvelope.AircraftUpdate(dto))
                        }
                        "aircraft_removed" -> {
                            val data = event.data ?: return
                            val dto = json.decodeFromJsonElement(
                                AircraftRemovedDto.serializer(), data
                            )
                            trySend(StreamEventEnvelope.AircraftRemoved(dto.icao))
                        }
                        "ping" -> { /* keepalive, ignore */ }
                        else -> Timber.d("Unknown event type: %s", event.type)
                    }
                } catch (t: Throwable) {
                    Timber.w(t, "Failed to parse WS message")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.i("WebSocket closing: %d %s", code, reason)
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.i("WebSocket closed: %d %s", code, reason)
                close()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.w(t, "WebSocket failure")
                close(t)
            }
        }

        val ws = okHttpClient.newWebSocket(request, listener)

        awaitClose {
            ws.close(1000, "Flow cancelled")
        }
    }
}

sealed interface StreamEventEnvelope {
    data object Connected : StreamEventEnvelope
    data object Disconnected : StreamEventEnvelope
    data class AircraftUpdate(val dto: AircraftDto) : StreamEventEnvelope
    data class AircraftRemoved(val icao: String) : StreamEventEnvelope
}