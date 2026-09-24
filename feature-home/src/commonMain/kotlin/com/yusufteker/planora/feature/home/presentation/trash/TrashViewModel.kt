package com.yusufteker.planora.feature.home.presentation.trash

import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock

/**
 * ViewModel managing the Recycle Bin (Trash) feature.
 *
 * Observes soft-deleted tasks from the offline database, coordinates
 * task restoration back into active planning, and handles individual
 * or bulk permanent deletion.
 *
 * Free users have a 7-day retention visibility window; Premium users
 * retain full access to all 30 days of deleted items.
 *
 * @property planRepository Repository providing task data access and deletion/restoration logic.
 * @property sessionPreferences User preferences to observe Premium status.
 * @property timeProvider Lambda supplying current epoch millis (configurable for unit tests).
 */
class TrashViewModel(
    private val planRepository: PlanRepository,
    private val sessionPreferences: SessionPreferences,
    private val timeProvider: () -> Long
) : BaseViewModel<TrashState, TrashEvent, TrashEffect>(TrashState()) {

    constructor(
        planRepository: PlanRepository,
        sessionPreferences: SessionPreferences
    ) : this(
        planRepository = planRepository,
        sessionPreferences = sessionPreferences,
        timeProvider = { com.yusufteker.planora.core.utils.getCurrentTimeMs() }
    )

    companion object {
        /**
         * Ücretsiz kullanıcılar için çöp kutusunda görünen maksimum saklama süresi (gün).
         */
        const val TRASH_RETENTION_FREE_DAYS = 7L

        /**
         * Premium kullanıcılar için çöp kutusunda görünen maksimum saklama süresi (gün).
         */
        const val TRASH_RETENTION_PREMIUM_DAYS = 30L
    }

    init {
        // Observe soft-deleted tasks reactively and enforce 7-day free vs 30-day premium retention window
        launch {
            combine(
                planRepository.observeDeletedTasks(),
                sessionPreferences.isPremiumFlow
            ) { allDeletedItems, isPrem ->
                val retentionDays = if (isPrem) TRASH_RETENTION_PREMIUM_DAYS else TRASH_RETENTION_FREE_DAYS
                val retentionMs = retentionDays * 24L * 60L * 60L * 1000L
                val currentMs = timeProvider()

                if (isPrem) {
                    allDeletedItems to 0
                } else {
                    val visibleItems = allDeletedItems.filter { item ->
                        (currentMs - item.deletedAt) <= retentionMs
                    }
                    val hiddenCount = allDeletedItems.count { item ->
                        (currentMs - item.deletedAt) > retentionMs
                    }
                    visibleItems to hiddenCount
                } to isPrem
            }.collect { (itemsAndHidden, isPrem) ->
                val (visibleItems, hiddenCount) = itemsAndHidden
                setState {
                    copy(
                        items = visibleItems,
                        hiddenItemCount = hiddenCount,
                        isPremium = isPrem
                    )
                }
            }
        }
    }

    override fun onEvent(event: TrashEvent) {
        when (event) {
            is TrashEvent.RestoreItem -> restoreItem(event.item.id)
            is TrashEvent.PermanentlyDeleteClicked -> {
                setState { copy(itemToDeletePermanently = event.item) }
            }
            is TrashEvent.PermanentlyDeleteConfirmed -> {
                val item = state.value.itemToDeletePermanently ?: return
                permanentlyDeleteItem(item.id)
            }
            is TrashEvent.PermanentlyDeleteDismissed -> {
                setState { copy(itemToDeletePermanently = null) }
            }
            is TrashEvent.ClearAllClicked -> {
                setState { copy(showClearAllDialog = true) }
            }
            is TrashEvent.ClearAllConfirmed -> {
                clearAllTrash()
            }
            is TrashEvent.ClearAllDismissed -> {
                setState { copy(showClearAllDialog = false) }
            }
            is TrashEvent.BackClicked -> {
                setEffect(TrashEffect.NavigateBack)
            }
            is TrashEvent.UpgradeClicked -> {
                setEffect(TrashEffect.NavigateToPremium)
            }
        }
    }

    private fun restoreItem(taskId: String) {
        launch {
            setState { copy(isLoading = true) }
            planRepository.restoreDeletedTask(taskId)
            setState { copy(isLoading = false) }
        }
    }

    private fun permanentlyDeleteItem(taskId: String) {
        launch {
            setState { copy(isLoading = true, itemToDeletePermanently = null) }
            planRepository.permanentlyDeleteTask(taskId)
            setState { copy(isLoading = false) }
        }
    }

    private fun clearAllTrash() {
        launch {
            setState { copy(isLoading = true, showClearAllDialog = false) }
            planRepository.clearAllDeletedTasks()
            setState { copy(isLoading = false) }
        }
    }
}
