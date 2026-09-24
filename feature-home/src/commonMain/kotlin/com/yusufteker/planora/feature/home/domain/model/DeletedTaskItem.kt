package com.yusufteker.planora.feature.home.domain.model

/**
 * Represents a soft-deleted task stored in the Recycle Bin for 30-day recovery.
 *
 * Premium users' deleted tasks are temporarily stored in this local recycle bin
 * rather than being deleted immediately, allowing one-tap restoration or permanent deletion.
 *
 * @property id Unique identifier of the deleted task.
 * @property title Title of the deleted task.
 * @property deletedAt Timestamp (epoch milliseconds) when the task was moved to the recycle bin.
 * @property daysRemaining Calculated number of days remaining before automatic permanent deletion (0 to 30).
 */
data class DeletedTaskItem(
    val id: String,
    val title: String,
    val deletedAt: Long,
    val daysRemaining: Int
)
