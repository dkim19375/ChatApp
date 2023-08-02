/*
 * MIT License
 *
 * Copyright (c) 2023 dkim19375
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.dkim19375.chatapp.client.online

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.dkim19375.chatapp.client.online.packet.Packet
import me.dkim19375.chatapp.client.online.packet.`in`.DisconnectPacketIn
import me.dkim19375.chatapp.client.online.packet.`in`.MessageSendPacketIn
import me.dkim19375.chatapp.client.online.packet.`in`.UserConnectPacketIn
import me.dkim19375.chatapp.client.online.packet.out.ConnectPacketOut
import me.dkim19375.chatapp.client.util.EventActions
import me.dkim19375.chatapp.common.util.SharedConstants
import me.dkim19375.chatapp.common.util.logging.logInfo
import me.dkim19375.dkimcore.extension.IO_SCOPE
import java.net.ConnectException
import java.util.UUID
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

@Suppress("unused")
private const val LOCALHOST = "127.0.0.1"

@Suppress("unused")
private const val PUBLIC_HOST = "chat.dkim19375.me"
private const val HOST = LOCALHOST
private const val PORT = SharedConstants.PORT

object ClientManager {
    private val packetTypes: Map<Byte, KFunction<Packet>> = mapOf(
        0x00 to UserConnectPacketIn::class,
        0x01 to MessageSendPacketIn::class,
        0x02 to DisconnectPacketIn::class
    ).mapNotNull {
        it.value.primaryConstructor?.let { constructor -> it.key.toByte() to constructor }
    }.toMap()

    val client = HttpClient {
        install(WebSockets)
    }
    var session: ClientWebSocketSession? = null
    private var uuid: UUID? = null

    fun connect(
        username: String,
        actions: EventActions = EventActions(),
        success: suspend () -> Unit = {},
        failure: suspend (ConnectException) -> Unit = {},
        finish: suspend () -> Unit = {},
    ) {
        IO_SCOPE.launch {
            try {
                session?.cancel()
                val startSession: suspend ClientWebSocketSession.() -> Unit = {
                    if (!isActive) {
                        throw ConnectException("Session inactive")
                    }
                    logInfo("success on port $PORT")
                    session = this
                    val uuid = UUID.randomUUID()
                    this@ClientManager.uuid = uuid
                    success()
                    runSession(username, actions, this, uuid, finish)
                }
                client.webSocket(method = HttpMethod.Get, host = HOST, port = PORT, path = "/chat") {
                    startSession()
                }
            } catch (ex: ConnectException) {
                failure(ex)
            }
        }
    }

    private suspend fun packetListener(
        actions: EventActions,
        session: ClientWebSocketSession,
    ) = coroutineScope baseScope@{
        logInfo("listening")
        runCatching {
            for (frame in session.incoming) {
                if (!this@baseScope.isActive || !session.isActive) {
                    logInfo("broke")
                    break
                }
                logInfo("got a frame")
                frame as? Frame.Binary ?: continue
                val bytes = frame.readBytes()
                val type = bytes[0]
                val packetData = bytes.drop(1).toByteArray()
                logInfo("type: $type, data (string): ${String(packetData)}, raw data: ${packetData.joinToString()}")
                when (type.toInt()) {
                    0x00, 0x01, 0x02 -> handlePacketIn(session, type, packetData, actions)
                    else -> handlePacketIn(session, type, packetData)
                }
                logInfo("Available")
            }
        }.exceptionOrNull()?.printStackTrace()
        logInfo("uh oh")
    }

    private suspend fun runSession(
        username: String,
        actions: EventActions,
        session: ClientWebSocketSession,
        uuid: UUID,
        finish: suspend () -> Unit = {},
    ) = coroutineScope baseScope@{
        handlePacketOut(session, ConnectPacketOut(username, uuid))
        logInfo("sent connect packet")
        val packetListenerJob = launch { packetListener(actions, session) }
        packetListenerJob.join()
        /*while (true) {
            if (!isActive) {
                logInfo("coroutine inactive")
                break
            }
            if (!session.isActive) {
                logInfo("session inactive 1")
                break
            }
            if (!packetListenerJob.isActive) {
                logInfo("packet listener inactive")
                break
            }
            Thread.sleep(100L)
            // delay(200L)
        }*/
        logInfo("cancelling")
        //packetListenerJob.cancelAndJoin()
        logInfo("finished")
        finish()
    }

    private suspend fun handlePacketIn(
        session: ClientWebSocketSession,
        type: Byte,
        data: ByteArray,
        extra: Any? = null,
    ) {
        val packet = packetTypes[type]?.call() ?: run {
            logInfo("invalid packet type: $type")
            return
        }
        sendDebugPacketMsg(packet)
        runCatching {
            packet.execute(
                socket = session,
                manager = this,
                data = data,
                extra = extra
            )
        }.exceptionOrNull()?.printStackTrace()
    }

    suspend fun handlePacketOut(session: ClientWebSocketSession, packet: Packet) {
        if (!session.isActive) {
            logInfo("session inactive 2")
            return
        }
        sendDebugPacketMsg(packet)
        runCatching {
            packet.execute(
                socket = session,
                manager = this,
                data = byteArrayOf()
            )
        }.exceptionOrNull()?.printStackTrace()
    }

    private fun sendDebugPacketMsg(packet: Packet) {
        val className = packet::class.simpleName ?: "NULL"
        logInfo("Packet $className, ${if (className.endsWith("Out")) "Outbound" else "Inbound"}")
    }
}

suspend fun ClientWebSocketSession.sendPacket(type: Byte, packetData: ByteArray) {
    if (!isActive) {
        logInfo("session inactive 3")
        return
    }
    send(Frame.Binary(true, byteArrayOf(type) + packetData))
}