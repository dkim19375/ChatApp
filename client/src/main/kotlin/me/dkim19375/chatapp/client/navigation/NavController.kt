package me.dkim19375.chatapp.client.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * NavController Class
 *
 * CREDIT: https://github.com/itheamc/navigation-for-compose-for-desktop/
 */
class NavController(
    private val startDestination: Screen,
    private var backStackScreens: MutableSet<Screen> = mutableSetOf(),
) {
    // Variable to store the state of the current screen
    var currentScreen: MutableState<Screen> = mutableStateOf(startDestination)

    // Function to handle the navigation between the screen
    fun navigate(route: Screen) {
        if (route != currentScreen.value) {
            if (backStackScreens.contains(currentScreen.value) && currentScreen.value != startDestination) {
                backStackScreens.remove(currentScreen.value)
            }

            if (route == startDestination) {
                backStackScreens = mutableSetOf()
            } else {
                backStackScreens.add(currentScreen.value)
            }

            currentScreen.value = route
        }
    }

    // Function to handle the back
    fun navigateBack() {
        if (backStackScreens.isNotEmpty()) {
            currentScreen.value = backStackScreens.last()
            backStackScreens.remove(currentScreen.value)
        }
    }
}


/**
 * Composable to remember the state of the [NavController]
 *
 * CREDIT: https://github.com/itheamc/navigation-for-compose-for-desktop/
 */
@Composable
fun rememberNavController(
    startDestination: Screen,
    backStackScreens: MutableSet<Screen> = mutableSetOf(),
): MutableState<NavController> = rememberSaveable {
    mutableStateOf(NavController(startDestination, backStackScreens))
}