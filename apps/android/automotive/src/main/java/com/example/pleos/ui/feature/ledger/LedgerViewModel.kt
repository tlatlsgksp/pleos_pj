package com.example.pleos.ui.feature.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pleos.domain.model.CareCatalog
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.ExpenseCategory
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordOrigin
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.usecase.ConfirmDraftUseCase
import com.example.pleos.domain.usecase.DismissDraftUseCase
import com.example.pleos.domain.usecase.ObserveLedgerUseCase
import com.example.pleos.domain.usecase.monthLedgers
import com.example.pleos.ui.common.Format
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LedgerViewModel @Inject constructor(
    observeLedger: ObserveLedgerUseCase,
    private val confirmDraft: ConfirmDraftUseCase,
    private val dismissDraft: DismissDraftUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(LedgerState())
    val state: StateFlow<LedgerState> = _state.asStateFlow()

    private val _effects = Channel<LedgerEffect>(Channel.BUFFERED)
    val effects: Flow<LedgerEffect> = _effects.receiveAsFlow()

    /** Every row, newest first; [LedgerState.items] is this list after the filter. */
    private var all: List<LedgerItemUi> = emptyList()

    init {
        viewModelScope.launch {
            observeLedger().collect { entries ->
                // Maintenance is not part of the app any more; only fuel, charge and other spending.
                val confirmed = entries.confirmed.filter { it.type != RecordType.MAINTENANCE }
                val records = (entries.drafts + confirmed).sortedByDescending { it.occurredAt }
                val isEv = records.firstNotNullOfOrNull { (it.detail as? RecordDetail.Energy)?.kind }?.let { it == EnergyType.EV } ?: true
                onEvent(
                    LedgerEvent.EntriesChanged(
                        items = records.map { it.toItem(clock.zone) },
                        months = monthLedgers(confirmed, clock.instant(), clock.zone),
                        isEv = isEv,
                    ),
                )
            }
        }
    }

    fun onEvent(event: LedgerEvent) {
        when (event) {
            is LedgerEvent.EntriesChanged -> {
                all = event.items
                _state.update { it.copy(items = all.filtered(it.filter), months = event.months, isEv = event.isEv, isLoading = false) }
            }
            is LedgerEvent.TabSelected -> _state.update { it.copy(tab = event.tab, selection = null) }
            is LedgerEvent.FilterSelected -> _state.update { it.copy(filter = event.filter, items = all.filtered(event.filter), selection = null) }
            is LedgerEvent.RecordTapped -> {
                val item = all.firstOrNull { it.id == event.id } ?: return
                _state.update { it.copy(selection = LedgerSelection.Record(item.id), amountInput = item.amountWon?.toString().orEmpty()) }
            }
            LedgerEvent.BackTapped -> _state.update { it.copy(selection = null) }
            is LedgerEvent.MonthStepped -> _state.update {
                it.copy(monthIndex = (it.monthIndex - event.delta).coerceIn(0, (it.months.size - 1).coerceAtLeast(0)))
            }
            is LedgerEvent.AmountChanged -> _state.update { it.copy(amountInput = event.digits) }
            is LedgerEvent.SaveDraftTapped -> handleDraft(event.id, "기록을 저장했어요") { confirmDraft(it, _state.value.amountInput.toLongOrNull()) }
            is LedgerEvent.DismissDraftTapped -> handleDraft(event.id, "기록하지 않았어요") { dismissDraft(it); true }
            is LedgerEvent.DraftHandled -> {
                _state.update { it.copy(isSaving = false, selection = null) }
                _effects.trySend(LedgerEffect.ShowMessage(event.message))
            }
        }
    }

    private fun handleDraft(id: String, done: String, action: suspend (String) -> Boolean) {
        if (_state.value.isSaving || all.none { it.id == id && it.isDraft }) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val ok = runCatching { action(id) }.getOrDefault(false)
            onEvent(LedgerEvent.DraftHandled(if (ok) done else "처리하지 못했어요. 다시 시도해 주세요."))
        }
    }

    /** The filtered list with the distance driven between each record and the next older one. */
    private fun List<LedgerItemUi>.filtered(filter: RecordFilter): List<LedgerItemUi> {
        val shown = filter.types?.let { types -> filter { it.type in types } } ?: this
        return shown.mapIndexed { i, item ->
            val older = shown.getOrNull(i + 1)?.odometerKm
            item.copy(gapKm = if (older != null && item.odometerKm != null) (item.odometerKm - older).takeIf { it > 0 } else null)
        }
    }
}

internal fun Record.toItem(zone: ZoneId): LedgerItemUi {
    val day = occurredAt.atZone(zone)
    val energy = detail as? RecordDetail.Energy
    val energyText = energy?.amount?.let { Format.energy(it, energy.kind == EnergyType.EV) }
    // The title says what happened in a few words; the second line is when.
    val title = when (val d = detail) {
        is RecordDetail.Energy -> listOfNotNull(if (d.kind == EnergyType.EV) "충전" else "주유", energyText).joinToString(" ")
        is RecordDetail.Maintenance -> d.itemIds.map { id -> CareCatalog.items.firstOrNull { it.id == id }?.name ?: id }
            .joinToString(", ").ifEmpty { "정비" }.let { "$it 교체" }
        is RecordDetail.Expense -> d.customLabel ?: d.category.label()
        is RecordDetail.Accident -> "사고"
    }
    val facts = buildList {
        energyText?.let { add((if (energy.kind == EnergyType.EV) "충전량" else "주유량") to it) }
        add("주행거리" to Format.km(stamp.odometerKm))
        when (val d = detail) {
            is RecordDetail.Energy -> d.placeName?.let { add("장소" to it) }
            is RecordDetail.Maintenance -> d.shopName?.let { add("정비소" to it) }
            is RecordDetail.Accident -> d.speedKmh?.let { add("속도" to "시속 %.0f km".format(it)) }
            is RecordDetail.Expense -> Unit
        }
        add("기록 방식" to if (origin == RecordOrigin.MANUAL) "직접 입력" else "차량이 감지")
    }
    return LedgerItemUi(
        id = id,
        date = day.toLocalDate(),
        type = type,
        title = title,
        detail = "%d월 %d일 %02d:%02d".format(day.monthValue, day.dayOfMonth, day.hour, day.minute),
        amountWon = amountWon,
        isDraft = status == RecordStatus.DRAFT,
        facts = facts,
        odometerKm = stamp.odometerKm,
        levelBefore = energy?.levelBefore,
        levelAfter = energy?.levelAfter,
        energyText = energyText,
        unitPriceText = energy?.unitPriceWon?.let { "%s당 %.1f원".format(if (energy.kind == EnergyType.EV) "kWh" else "L", it) },
    )
}

internal fun ExpenseCategory.label(): String = when (this) {
    ExpenseCategory.CAR_WASH -> "세차"
    ExpenseCategory.PARKING -> "주차"
    ExpenseCategory.TOLL -> "통행료"
    ExpenseCategory.INSURANCE -> "보험"
    ExpenseCategory.TAX -> "세금"
    ExpenseCategory.OTHER, ExpenseCategory.CUSTOM -> "기타"
}
