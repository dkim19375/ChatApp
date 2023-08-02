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

package me.dkim19375.chatapp.client.window

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.runBlocking
import me.dkim19375.chatapp.client.navigation.NavController
import me.dkim19375.chatapp.client.online.ClientManager
import me.dkim19375.chatapp.client.online.packet.out.MessageSendPacketOut
import me.dkim19375.chatapp.client.util.Constants
import me.dkim19375.chatapp.common.util.logging.logInfo
import java.awt.event.MouseEvent

val messageText = mutableStateListOf<Pair<Long, AnnotatedString>>()
private val rightClick = mutableStateOf<Pair<Long, Int>?>(null)

@Composable
@Preview
fun ChatScreen(navController: NavController) {
    if (connectState.value != ConnectState.CONNECTED) {
        return
    }
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Constants.BACKGROUND_COLOR),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .background(Constants.TEXT_BACKGROUND_COLOR)
                    .weight(1F)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(ScrollState(Int.MAX_VALUE))
                ) {
                    for ((i, pair) in messageText.mapIndexed { i, str -> i to str }) {
                        val id = pair.first
                        val message = pair.second
                        Text(
                            text = message,
                            modifier = Modifier
                                .padding(
                                    start = 16.dp,
                                    top = if (i == 0) 16.dp else 0.dp,
                                    end = 16.dp,
                                    bottom = if (i + 1 == messageText.size) 16.dp else 0.dp
                                )
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    while (true) {
                                        val event = awaitPointerEventScope { awaitPointerEvent() }
                                        val awtEvent = event.awtEventOrNull ?: continue
                                        if ((event.type == PointerEventType.Release)
                                            && awtEvent.button == MouseEvent.BUTTON3
                                        ) {
                                            logInfo("loc: ${awtEvent.point}")
                                            rightClick.value = id to (awtEvent.x - 32).coerceAtLeast(0)
                                        }
                                    }
                                },
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                lineHeight = 30.sp
                            )
                        )
                        val rcValue = rightClick.value
                        if (rcValue?.first == id) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = {
                                    rightClick.value = null
                                },
                                modifier = Modifier
                                    .padding(rcValue.second.dp, 0.dp, 0.dp, 0.dp)
                                    .background(Constants.CONTEXT_MENU_COLOR, shape = RoundedCornerShape(10.dp))
                            ) {
                                Text(
                                    text = "Testing",
                                    modifier = Modifier
                                        .background(Color.Transparent),
                                    style = TextStyle(
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        lineHeight = 30.sp
                                    )
                                )
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterVertically),
                    adapter = ScrollbarAdapter(ScrollState(Int.MAX_VALUE))
                )
            }
            val textState = remember { mutableStateOf(TextFieldValue()) }
            TextField(
                value = textState.value,
                onValueChange = { str ->
                    textState.value = str
                },
                modifier = Modifier
                    .padding(16.dp, 0.dp, 16.dp, 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .requiredHeightIn(50.dp, 300.dp)
                    .fillMaxWidth()
                    .background(Constants.TEXT_BACKGROUND_COLOR)
                    .onPreviewKeyEvent event@{ event ->
                        if (event.type == KeyEventType.KeyUp) {
                            return@event false
                        }
                        when {
                            (event.key == Constants.ENTER_KEY) -> {
                                val text = textState.value
                                if (text.text.isBlank()) {
                                    return@event false
                                }
                                if (event.isShiftPressed) {
                                    val newStr = "${text.text}\n"
                                    val value = TextFieldValue(newStr, TextRange(newStr.length))
                                    textState.value = value
                                    return@event true
                                }
                                // addMessageStr("$username: ${text.text.trimEnd()}")
                                textState.value = TextFieldValue("")
                                val session = ClientManager.session ?: return@event true
                                runBlocking {
                                    ClientManager.handlePacketOut(
                                        session,
                                        MessageSendPacketOut(text.text.trim())
                                    )
                                }
                                true
                            }

                            else -> false
                        }
                    },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    backgroundColor = Constants.TEXT_BACKGROUND_COLOR,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Color.White,
                    placeholderColor = Color(255, 255, 255, 0xA0),
                ),
                singleLine = false,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                ),
                placeholder = { Text("Message") }
            )
        }
    }
}