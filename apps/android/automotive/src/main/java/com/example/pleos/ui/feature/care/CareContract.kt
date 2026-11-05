package com.example.pleos.ui.feature.care

import com.example.pleos.domain.model.CareStatus
import com.example.pleos.domain.usecase.CareItemView
import java.time.LocalDate

/** One care item as the screens show it. */
data class CareItemUi(
    val id: String,
    val name: String,
    val status: CareStatus,
    /** 0.0 = just serviced, 1.0 = interval reached; may exceed 1. null when unknown. */
    val ratio: Double?,
    /** Negative once overdue. */
    val remainingKm: Double?,
    val dueDate: LocalDate?,
    val intervalKm: Int?,
    val intervalMonths: Int?,
)

internal fun CareItemView.toUi() = CareItemUi(
    id = def.id,
    name = def.name,
    status = progress.status,
    ratio = progress.ratio,
    remainingKm = progress.remainingKm,
    dueDate = progress.dueDate,
    intervalKm = state?.customIntervalKm ?: def.intervalKm,
    intervalMonths = state?.customIntervalMonths ?: def.intervalMonths,
)

data class CareState(
    val items: List<CareItemUi> = emptyList(),
    val isLoading: Boolean = true,
    /** Item whose service is being saved, to disable its button. */
    val savingItemId: String? = null,
)

sealed interface CareEvent {
    data class ServicedTapped(val itemId: String) : CareEvent
    data object BackTapped : CareEvent

    data class ItemsChanged(val items: List<CareItemUi>) : CareEvent
    data class ServiceSaved(val itemName: String) : CareEvent
    data class ServiceFailed(val reason: String) : CareEvent
}

sealed interface CareEffect {
    data class ShowMessage(val text: String) : CareEffect
    data object NavigateBack : CareEffect
}
