package com.example.pleos.ui.feature.charge

import java.time.LocalDateTime

data class ChargeConfirmState(
    val isLoading: Boolean = true,
    /** The draft is gone or already handled. */
    val isMissing: Boolean = false,
    val amountKwh: Double? = null,
    val levelBefore: Double? = null,
    val levelAfter: Double? = null,
    val odometerKm: Double? = null,
    val occurredAt: LocalDateTime? = null,
    val unitPriceWon: Double? = null,
    /** What the user typed; starts as the estimate. Digits only. */
    val amountInput: String = "",
    val isSaving: Boolean = false,
) {
    val amountWon: Long? get() = amountInput.toLongOrNull()
}

sealed interface ChargeConfirmEvent {
    data class AmountChanged(val text: String) : ChargeConfirmEvent
    data object ConfirmTapped : ChargeConfirmEvent
    data object DismissTapped : ChargeConfirmEvent
    data object BackTapped : ChargeConfirmEvent

    data class DraftLoaded(val state: ChargeConfirmState) : ChargeConfirmEvent
    data class Finished(val message: String) : ChargeConfirmEvent
}

sealed interface ChargeConfirmEffect {
    data class ShowMessage(val text: String) : ChargeConfirmEffect
    data object Close : ChargeConfirmEffect
}
