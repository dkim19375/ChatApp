package me.dkim19375.chatapp.client.navigation

import androidx.compose.runtime.Composable

/**
 * NavigationHost class
 *
 * CREDIT: https://github.com/itheamc/navigation-for-compose-for-desktop/
 */
class NavigationHost(
    val navController: NavController,
    val contents: @Composable NavigationGraphBuilder.() -> Unit,
) {

    @Composable
    fun build() {
        NavigationGraphBuilder().renderContents()
    }

    inner class NavigationGraphBuilder(
        val navController: NavController = this@NavigationHost.navController,
    ) {
        @Composable
        fun renderContents() {
            this@NavigationHost.contents(this)
        }
    }
}


/**
 * Composable to build the Navigation Host
 *
 * CREDIT: https://github.com/itheamc/navigation-for-compose-for-desktop/
 */
@Composable
fun NavigationHost.NavigationGraphBuilder.composable(
    route: Screen,
    content: @Composable () -> Unit,
) {
    if (navController.currentScreen.value == route) {
        content()
    }
}