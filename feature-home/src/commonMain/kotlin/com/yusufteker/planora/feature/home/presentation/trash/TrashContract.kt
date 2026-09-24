package com.yusufteker.planora.feature.home.presentation.trash

import com.yusufteker.planora.core.base.UiEffect
import com.yusufteker.planora.core.base.UiEvent
import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.feature.home.domain.model.DeletedTaskItem

/**
 * UI State for the Recycle Bin (Trash) screen.
 *
 * @property items List of soft-deleted tasks currently stored in the recycle bin.
 * @property isLoading Indicates whether a background operation (restore, delete) is in progress.
 * @property isPremium Whether the user is currently subscribed to Premium.
 * @property showClearAllDialog Controls visibility of the "Empty Trash" confirmation modal.
 * @property itemToDeletePermanently If non-null, prompts the user to confirm permanent deletion for this specific item.
 */
data class TrashState(
    val items: List<DeletedTaskItem> = emptyList(),
    val isLoading: Boolean = false,
    val isPremium: Boolean = false,
    val hiddenItemCount: Int = 0,
    val showClearAllDialog: Boolean = false,
    val itemToDeletePermanently: DeletedTaskItem? = null
) : UiState

/**
 * User actions and events on the Recycle Bin screen.
 */
sealed interface TrashEvent : UiEvent {
    /** Restores the specified item back to active tasks. */
    data class RestoreItem(val item: DeletedTaskItem) : TrashEvent

    /** Prompts confirmation to permanently delete a specific item. */
    data class PermanentlyDeleteClicked(val item: DeletedTaskItem) : TrashEvent

    /** Confirms permanent deletion of the selected item. */
    data object PermanentlyDeleteConfirmed : TrashEvent

    /** Dismisses the single-item permanent deletion dialog. */
    data object PermanentlyDeleteDismissed : TrashEvent

    /** Prompts confirmation to empty the entire recycle bin. */
    data object ClearAllClicked : TrashEvent

    /** Confirms emptying the entire recycle bin. */
    data object ClearAllConfirmed : TrashEvent

    /** Dismisses the empty-trash dialog. */
    data object ClearAllDismissed : TrashEvent

    /** User tapped the back button. */
    data object BackClicked : TrashEvent

    /** User tapped the upgrade to Premium banner. */
    data object UpgradeClicked : TrashEvent
}

/**
 * Side effects triggered by the Recycle Bin ViewModel.
 */
sealed interface TrashEffect : UiEffect {
    data object NavigateBack : TrashEffect
    data object NavigateToPremium : TrashEffect
    data class ShowToast(val message: String) : TrashEffect
}
