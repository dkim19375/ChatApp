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

package me.dkim19375.chatapp.client.online.packet.`in`

import com.google.common.primitives.Longs
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import me.dkim19375.chatapp.client.online.ClientManager
import me.dkim19375.chatapp.client.online.packet.Packet
import me.dkim19375.chatapp.client.util.EventActions
import me.dkim19375.chatapp.client.util.MessageData

class MessageSendPacketIn : Packet {
    override suspend fun execute(socket: ClientWebSocketSession, manager: ClientManager, data: ByteArray, extra: Any?) {
        val onMessage = (extra as EventActions).onMessage
        val nameLength = data[0].toInt()
        val name = data.drop(1).dropLast(data.size - 1 - nameLength)
        val message = data.drop(nameLength + 1).dropLast(Long.SIZE_BYTES)
        val id = Longs.fromByteArray(data.drop(data.size - Long.SIZE_BYTES).toByteArray())
        onMessage(MessageData(String(name.toByteArray()), String(message.toByteArray()), id))
    }
}