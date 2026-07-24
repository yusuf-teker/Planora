package com.yusufteker.planora.core.ui.text

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A sealed class to represent text that can be either a hardcoded string
 * or a string resource (with optional arguments) from Compose Multiplatform resources.
 * This is useful for ViewModels to pass localized text to the UI.
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    class StringResourceId(val resId: StringResource, vararg val args: Any) : UiText()

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResourceId -> stringResource(resId, *args)
        }
    }
}
