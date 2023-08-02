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

package me.dkim19375.chatapp.server.packet.`in`

import io.ktor.server.websocket.DefaultWebSocketServerSession
import me.dkim19375.chatapp.server.ServerManager
import me.dkim19375.chatapp.server.packet.Packet
import me.dkim19375.chatapp.server.packet.out.UserConnectPacketOut
import java.util.UUID

class ConnectPacketIn : Packet {
    override suspend fun execute(socket: DefaultWebSocketServerSession, manager: ServerManager, packetData: ByteArray) {
        customExecute(socket, manager, packetData)
    }

    suspend fun customExecute(
        socket: DefaultWebSocketServerSession,
        manager: ServerManager,
        packetData: ByteArray,
    ): Pair<UUID, String> {
        val nameLength = packetData[0].toInt()
        val username = String(packetData.drop(1).take(nameLength).toByteArray())
        println(
            "Name length: $nameLength, username: $username, uuid: ${
                String(
                    packetData.drop(nameLength + 1).toByteArray()
                )
            }"
        )
        val uuid = UUID.fromString(String(packetData.drop(nameLength + 1).toByteArray()))
        manager.userMap[uuid] = username to socket
        println("added $username to userMap")
        val packet = UserConnectPacketOut(username)
        manager.broadcastPacket(packet, setOf(socket))
        return uuid to username
    }
}