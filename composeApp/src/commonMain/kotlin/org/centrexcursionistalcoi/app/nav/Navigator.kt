package org.centrexcursionistalcoi.app.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import kotlin.reflect.KClass

/**
 * Creates the [Navigator] for this app's single back stack, persisted across config changes and process death.
 */
@Composable
fun rememberNavigator(startDestination: Destination): Navigator {
    val backStack = rememberNavBackStack(destinationSavedStateConfiguration, startDestination)
    return remember(backStack) { Navigator(backStack) }
}

/**
 * Handles navigation events for this app's single back stack.
 *
 * The app only ever has one linear back stack (no bottom-nav-style top-level routes with their own retained
 * stacks), so this is intentionally simpler than a multi-stack navigator.
 */
class Navigator(val backStack: NavBackStack<NavKey>) {
    val current: Destination get() = backStack.last() as Destination

    fun navigate(destination: Destination) {
        backStack.add(destination)
    }

    /** Pushes the whole synthetic back stack for a deep link, see [Destination.backStackFor]. */
    fun navigate(destinations: List<Destination>) {
        backStack.addAll(destinations)
    }

    /** Replaces the entire back stack with just [destination]. */
    fun navigateClearingStack(destination: Destination) {
        backStack.clear()
        backStack.add(destination)
    }

    /** Pops the stack back to (but keeping) the nearest [popUpTo] entry, then pushes [destination]. */
    fun navigatePoppingUpTo(destination: Destination, popUpTo: KClass<out Destination>) {
        val index = backStack.indexOfLast { popUpTo.isInstance(it) }
        if (index >= 0) {
            while (backStack.size > index + 1) backStack.removeAt(backStack.size - 1)
        }
        backStack.add(destination)
    }

    fun goBack(): Boolean = backStack.removeLastOrNull() != null
}
