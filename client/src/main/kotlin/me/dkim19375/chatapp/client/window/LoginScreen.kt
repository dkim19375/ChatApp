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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.dkim19375.chatapp.client.navigation.NavController
import me.dkim19375.chatapp.client.navigation.Screen
import me.dkim19375.chatapp.client.online.ClientManager
import me.dkim19375.chatapp.client.util.Constants
import me.dkim19375.chatapp.client.util.EventActions
import me.dkim19375.chatapp.common.util.logging.logInfo
import me.dkim19375.dkimcore.extension.SCOPE

private val openChatWindow = mutableStateOf(false)
private var username = mutableStateOf(TextFieldValue("Guest", TextRange(5)))
private var usernameStr: String
    get() = username.value.text
    set(value) {
        username.value = TextFieldValue(value, TextRange(value.length))
    }
val connectState = mutableStateOf(ConnectState.NONE)

enum class ConnectState {
    NONE,
    CONNECTING,
    CONNECTED,
    FAILURE
}

@Composable
@Preview
fun LoginScreen(navController: NavController) {
    val connectState = connectState.value
    logInfo("state: ${connectState.name}")
    if (connectState == ConnectState.CONNECTED) {
        navController.navigate(Screen.ChatScreen)
        return
    }
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Constants.BACKGROUND_COLOR),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Chat App",
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(50.dp),
                fontSize = 40.sp,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Username:",
                    modifier = Modifier
                        .padding(16.dp, 0.dp)
                        .height(41.dp),
                    fontSize = 20.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
                TextField(
                    value = username.value,
                    onValueChange = { text: TextFieldValue ->
                        usernameStr = text.text.filter {
                            it.code in 33..126
                        }
                    },
                    modifier = Modifier
                        .padding(16.dp, 0.dp, 16.dp, 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .requiredHeightIn(50.dp, 300.dp)
                        .fillMaxWidth(0.5f)
                        .background(Constants.TEXT_BACKGROUND_COLOR),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        backgroundColor = Constants.TEXT_BACKGROUND_COLOR,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = Color.White,
                        placeholderColor = Color(255, 255, 255, 0xA0),
                    ),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                    ),
                    placeholder = { Text("Guest") }
                )
            }
            when (connectState) {
                ConnectState.NONE -> {}
                ConnectState.CONNECTING -> {
                    Text(
                        text = "Connecting...",
                        fontSize = 30.sp,
                        maxLines = 1,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }

                ConnectState.FAILURE -> {
                    Text(
                        text = "Could not connect!",
                        fontSize = 30.sp,
                        maxLines = 1,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Constants.RED_TEXT
                    )
                    SCOPE.launch {
                        delay(3000L)
                        withContext(Dispatchers.Default) {
                            me.dkim19375.chatapp.client.window.connectState.value = ConnectState.NONE
                        }
                    }
                }

                ConnectState.CONNECTED -> {}
            }
            Button(
                onClick = {
                    if (connectState == ConnectState.NONE) {
                        openChatWindow.value = true
                    }
                },
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .size(200.dp, 50.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Constants.BUTTON_COLOR)
            ) {
                Text(
                    text = "Enter Chat",
                    fontSize = 20.sp,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }
        }
    }
    if (openChatWindow.value) {
        openChatWindow.value = false
        logInfo("called 1")
        me.dkim19375.chatapp.client.window.connectState.value = ConnectState.CONNECTING
        val addMessageStr: (Long, String) -> Unit = { id, str ->
            messageText.add(id to AnnotatedString(str))
        }
        ClientManager.connect(
            username = usernameStr.ifEmpty { "Guest" },
            actions = EventActions(
                onUserConnect = { user ->
                    withContext(Dispatchers.Default) {
                        addMessageStr(Long.MIN_VALUE, "$user has joined the chat!")
                    }
                },
                onMessage = { data ->
                    withContext(Dispatchers.Default) {
                        addMessageStr(data.id, "${data.sender}: ${data.message}")
                    }
                },
                onDisconnect = { user ->
                    withContext(Dispatchers.Default) {
                        addMessageStr(Long.MIN_VALUE, "$user has left the chat!")
                    }
                }
            ),
            success = {
                withContext(Dispatchers.Default) {
                    messageText.clear()
                    me.dkim19375.chatapp.client.window.connectState.value = ConnectState.CONNECTED
                }
            },
            failure = {
                withContext(Dispatchers.Default) {
                    me.dkim19375.chatapp.client.window.connectState.value = ConnectState.FAILURE
                    navController.navigate(Screen.LoginScreen)
                }
                it.printStackTrace()
            },
            finish = {
                withContext(Dispatchers.Default) {
                    me.dkim19375.chatapp.client.window.connectState.value = ConnectState.NONE
                    navController.navigate(Screen.LoginScreen)
                }
            }
        )
        navController.navigate(Screen.ChatScreen)
    }
}