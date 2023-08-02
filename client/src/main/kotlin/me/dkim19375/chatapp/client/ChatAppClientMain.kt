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

package me.dkim19375.chatapp.client

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.singleWindowApplication
import kotlinx.coroutines.delay
import me.dkim19375.chatapp.client.navigation.NavController
import me.dkim19375.chatapp.client.navigation.NavigationHost
import me.dkim19375.chatapp.client.navigation.Screen
import me.dkim19375.chatapp.client.navigation.composable
import me.dkim19375.chatapp.client.navigation.rememberNavController
import me.dkim19375.chatapp.client.window.ChatScreen
import me.dkim19375.chatapp.client.window.LoginScreen
import me.dkim19375.chatapp.common.util.logging.SysStreamsLogger
import java.awt.Dimension

fun main() {
    SysStreamsLogger.bindSystemStreams()
    singleWindowApplication {
        LaunchedEffect(key1 = Unit) {
            window.size = Dimension(window.width + 1, window.height + 1)
            delay(1)
            window.size = Dimension(window.width - 1, window.height - 1)
        }
        ChatApp()
    }
}

@Composable
fun ChatApp() {
    val navController by rememberNavController(Screen.LoginScreen)
    CustomNavigationHost(navController)
}

@Composable
fun CustomNavigationHost(
    navController: NavController,
) {
    NavigationHost(navController) {
        composable(Screen.LoginScreen) {
            LoginScreen(navController)
        }

        composable(Screen.ChatScreen) {
            ChatScreen(navController)
        }
    }.build()
}