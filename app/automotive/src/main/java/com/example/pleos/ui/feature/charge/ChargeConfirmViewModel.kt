package com.example.pleos.ui.feature.charge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.repository.RecordRepository
import com.example.pleos.domain.usecase.ConfirmDraftUseCase
import com.example.pleos.domain.usecase.DismissDraftUseCase
import com.example.pleos.ui.common.Format
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Shows one detected charge draft; the user checks the amount and confirms or dismisses it. */
@HiltViewModel
class ChargeConfirmViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val records: RecordRepository,
    private val confirmDraft: ConfirmDraftUseCase,
    private val dismissDraft: DismissDraftUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val recordId: String = checkNotNull(savedStateHandle[ARG_RECORD_ID]) { "Missing $ARG_RECORD_ID" }

    private val _state = MutableStateFlow(ChargeConfirmState())
    val state: StateFlow<ChargeConfirmState> = _state.asStateFlow()

    private val _effects = Channel<ChargeConfirmEffect>(Channel.BUFFERED)
    val effects: Flow<ChargeConfirmEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            val record = records.observe(recordId).first()
            val detail = record?.detail as? RecordDetail.Energy
            val loaded = if (record == null || detail == null || record.status != RecordStatus.DRAFT) {
                ChargeConfirmState(isLoading = false, isMissing = true)
            } else {
                ChargeConfirmState(
                    isLoading = false,
                    amountKwh = detail.amount,
                    levelBefore = detail.levelBefore,
                    levelAfter = detail.levelAfter,
                    odometerKm = record.stamp.odometerKm,
                    occurredAt = LocalDateTime.ofInstant(record.occurredAt, clock.zone),
                    unitPriceWon = detail.unitPriceWon,
                    amountInput = record.amountWon?.toString().orEmpty(),
                )
            }
            onEvent(ChargeConfirmEvent.DraftLoaded(loaded))
        }
    }

    fun onEvent(event: ChargeConfirmEvent) {
        when (event) {
            is ChargeConfirmEvent.DraftLoaded -> _state.value = event.state
            is ChargeConfirmEvent.AmountChanged ->
                _state.update { it.copy(amountInput = event.text.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS)) }
            ChargeConfirmEvent.ConfirmTapped -> save {
                val amount = _state.value.amountWon
                if (confirmDraft(recordId, amount)) {
                    if (amount != null) "충전 ${Format.won(amount)}을 기록했어요" else "충전을 기록했어요"
                } else {
                    "이미 처리된 기록이에요"
                }
            }
            ChargeConfirmEvent.DismissTapped -> save {
                dismissDraft(recordId)
                "이 충전은 기록하지 않아요"
            }
            is ChargeConfirmEvent.Finished -> {
                _effects.trySend(ChargeConfirmEffect.ShowMessage(event.message))
                _effects.trySend(ChargeConfirmEffect.Close)
            }
            ChargeConfirmEvent.BackTapped -> _effects.trySend(ChargeConfirmEffect.Close)
        }
    }

    private fun save(action: suspend () -> String) {
        if (_state.value.isSaving || _state.value.isMissing) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch { onEvent(ChargeConfirmEvent.Finished(action())) }
    }

    companion object {
        const val ARG_RECORD_ID = "recordId"
        private const val MAX_AMOUNT_DIGITS = 9
    }
}
