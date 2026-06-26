package com.yusufteker.pulse.core.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * A generic navigator that manages a back stack for Navigation 3.
 *
 * @param T The type of the destination screen.
 * @property backStack The mutable state list holding the current navigation stack.
 */
class Navigator<T : Any>(
    val backStack: SnapshotStateList<T>
) {
    /**
     * Gets the current (top-most) screen in the back stack.
     */
    val currentDestination: T?
        get() = backStack.lastOrNull()

    /**
     * Navigates to the specified screen by adding it to the top of the back stack.
     */
    fun navigate(screen: T) {
        backStack.add(screen)
    }

    /**
     * Pops the top-most screen from the back stack.
     * Does nothing if there is only one screen left (the root).
     */
    fun pop() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    /**
     * Replaces the current top-most screen with a new one.
     */
    fun replace(screen: T) {
        if (backStack.isNotEmpty()) {
            backStack.removeLastOrNull()
        }
        backStack.add(screen)
    }

    /**
     * Clears the entire back stack and sets the given screen as the new root.
     */
    fun setRoot(screen: T) {
        backStack.clear()
        backStack.add(screen)
    }

    /**
     * Pops all screens up to the specified screen.
     * 
     * @param screen The destination screen to pop up to.
     * @param inclusive If true, also removes the specified screen from the back stack.
     */
    fun popUpTo(screen: T, inclusive: Boolean = false) {
        val index = backStack.indexOfLast { it == screen }
        if (index != -1) {
            val targetSize = if (inclusive) index else index + 1
            while (backStack.size > targetSize) {
                backStack.removeLastOrNull()
            }
        }
    }
}

/**
 * CompositionLocal providing access to the generic [Navigator].
 * Usually instantiated for [Screen] or other sealed destination types.
 */
val LocalNavigator = compositionLocalOf<Navigator<Screen>> {
    error("No Navigator provided. Make sure to wrap your UI with CompositionLocalProvider(LocalNavigator provides navigator) {...}")
}

/**
 * CompositionLocal providing access to the nested [Navigator] for Bottom Navigation.
 */
val LocalMainNavigator = compositionLocalOf<Navigator<Screen.MainDestination>> {
    error("No MainNavigator provided. Make sure to wrap your UI with CompositionLocalProvider(LocalMainNavigator provides navigator) {...}")
}
