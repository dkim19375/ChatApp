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

package me.dkim19375.chatapp.server

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.origin
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.send
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import me.dkim19375.chatapp.common.util.SharedConstants
import me.dkim19375.chatapp.common.util.logging.SysStreamsLogger
import me.dkim19375.chatapp.common.util.logging.logInfo
import me.dkim19375.chatapp.server.packet.Packet
import me.dkim19375.chatapp.server.packet.`in`.ConnectPacketIn
import me.dkim19375.chatapp.server.packet.`in`.MessageSendPacketIn
import me.dkim19375.chatapp.server.packet.out.DisconnectPacketOut
import me.dkim19375.chatapp.server.util.MessageData
import java.time.Duration
import java.util.UUID
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

fun main() {
    SysStreamsLogger.bindSystemStreams()
    embeddedServer(Netty, port = SharedConstants.PORT) {
        runServerApplication()
    }.start(wait = true)
}

private fun Application.runServerApplication() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(5)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/chat") {
            ServerManager.runSession(this)
        }
    }
}

object ServerManager {
    private val packetTypes: Map<Byte, KFunction<Packet>> = mapOf(
        0x00 to ConnectPacketIn::class,
        0x01 to MessageSendPacketIn::class
    ).mapNotNull {
        it.value.primaryConstructor?.let { constructor -> it.key.toByte() to constructor }
    }.toMap()
    val userMap = mutableMapOf<UUID, Pair<String, DefaultWebSocketServerSession>>()
    var id = 0L
    val messages = mutableListOf<MessageData>()

    suspend fun runSession(session: DefaultWebSocketServerSession) = coroutineScope baseScope@{
        val ip = session.call.request.origin.remoteAddress
        println("got request from $ip, isActive: ${session.isActive}")
        val (uuid, username) = let {
            runCatching {
                logInfo("Listening for first packet")
                val bytes = (session.incoming.receive() as Frame.Binary).readBytes()
                println("got a frame")
                if (bytes[0] != 0x00.toByte()) {
                    throw IllegalStateException("first packet must be ConnectPacketIn")
                }
                val packet = ConnectPacketIn()
                sendDebugPacketMsg(packet)
                packet.customExecute(
                    session,
                    manager = this@ServerManager,
                    packetData = bytes.drop(1).toByteArray()
                )
            }.getOrElse {
                logInfo("Nope")
                it.printStackTrace()
                session.close()
                return@baseScope
            }
        }
        for (frame in session.incoming) {
            if (uuid !in userMap) {
                logInfo("UUID not in userMap")
                break
            }
            println("got a frame: ${frame is Frame.Binary}")
            frame as? Frame.Binary ?: continue
            val bytes = frame.readBytes()
            println("bytes: ${bytes.joinToString()}")
            val type = bytes[0]
            println("type: $type")
            val packetData = bytes.drop(1).toByteArray()
            println("type: $type, data (string): ${String(packetData)}, raw data: ${packetData.joinToString()}")
            handlePacketIn(session, type, packetData)
        }
        println("disconnected from $ip")
        broadcastPacket(DisconnectPacketOut(username))
        userMap.remove(uuid)
        if (session.isActive) {
            session.close()
        }
    }

    private suspend fun handlePacketIn(session: DefaultWebSocketServerSession, type: Byte, packetData: ByteArray) {
        if (!session.isActive) {
            println("session closed")
            return
        }
        val packet = packetTypes[type]?.call() ?: run {
            println("invalid packet type: $type")
            return
        }
        sendDebugPacketMsg(packet, userMap.toList().firstOrNull { (_, pair) ->
            pair.second == session
        }?.first)
        runCatching {
            packet.execute(
                socket = session,
                manager = this,
                packetData = packetData
            )
        }.exceptionOrNull()?.printStackTrace()
    }

    private suspend fun handlePacketOut(session: DefaultWebSocketServerSession, packet: Packet) {
        if (!session.isActive) {
            println("session closed")
            return
        }
        sendDebugPacketMsg(packet, userMap.toList().firstOrNull { (_, pair) ->
            pair.second == session
        }?.first)
        runCatching {
            packet.execute(
                socket = session,
                manager = this,
                packetData = byteArrayOf()
            )
        }.exceptionOrNull()?.printStackTrace()
    }

    suspend fun broadcastPacket(
        packet: Packet,
        add: Set<DefaultWebSocketServerSession> = emptySet(),
        exclude: Set<UUID> = emptySet(),
    ) {
        println("broadcasted to ${userMap.map { it.key to it.value.first }.joinToString()}")
        val allSessions = userMap.toMap()
        val sessions = add.toMutableSet()
        for ((uuid, pair) in allSessions) {
            val session = pair.second
            if (uuid in exclude) {
                sessions.remove(session)
                continue
            }
            sessions.add(session)
        }
        sessions.forEach { session ->
            handlePacketOut(session, packet)
            println("Sent to ${userMap.toList().find { it.second.second === session }?.second?.first}")
        }
    }

    private fun sendDebugPacketMsg(packet: Packet, uuid: UUID? = null) {
        val className = packet::class.simpleName ?: "NULL"
        println("Packet $className, ${if (className.endsWith("Out")) "Outbound" else "Inbound"} to $uuid")
    }
}

suspend fun DefaultWebSocketServerSession.sendPacket(type: Byte, packetData: ByteArray) {
    if (!isActive) {
        println("session closed")
        return
    }
    //val compressed = Compressor.compress(data)
    send(byteArrayOf(type) + /*Ints.toByteArray(packetData.size) + */packetData)
}