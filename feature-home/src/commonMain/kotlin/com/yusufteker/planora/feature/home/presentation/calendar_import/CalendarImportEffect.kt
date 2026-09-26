package com.yusufteker.planora.feature.home.presentation.calendar_import

import com.yusufteker.planora.core.base.UiEffect
import org.jetbrains.compose.resources.StringResource

/**
 * Single-shot side effects for the Calendar Import screen.
 */
sealed interface CalendarImportEffect : UiEffect {
    /** Navigate back to the previous screen */
    data object NavigateBack : CalendarImportEffect

    /** Show a snackbar message with an optional formatted count */
    data class ShowSnackbar(val messageRes: StringResource, val count: Int? = null) : CalendarImportEffect
}
