package com.example.pleos.ui.feature.ledger

import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.usecase.MonthLedger
import java.time.LocalDate

/** Line tabs of the fuel and charge tab (docs/visual/ledger-design-v5.html): statistics, records. */
enum class LedgerTab { STATS, RECORDS }

/** Filter chips inside the records tab. */
enum class RecordFilter(val types: Set<RecordType>?) {
    ALL(null),
    ENERGY(setOf(RecordType.ENERGY)),
    EXPENSE(setOf(RecordType.EXPENSE, RecordType.ACCIDENT)),
}

/** One record as the spine and the detail show it. Text is built in the ViewModel. */
data class LedgerItemUi(
    val id: String,
    val date: LocalDate,
    val type: RecordType,
    val title: String,
    val detail: String?,
    val amountWon: Long?,
    /** Detected and waiting for the user. */
    val isDraft: Boolean,
    /** Label–value lines for the detail column. */
    val facts: List<Pair<String, String>> = emptyList(),
    /** The odometer the car stamped; the spine leads with it in the two-thirds window. */
    val odometerKm: Double? = null,
    /** Battery or fuel % before and after an energy record, for the level meter. */
    val levelBefore: Double? = null,
    val levelAfter: Double? = null,
    /** "40.0 kWh" for an energy record, shown in the middle of the level meter. */
    val energyText: String? = null,
    /** "kWh당 324.4원" when the car knew the unit price, under the amount field. */
    val unitPriceText: String? = null,
    /** Driven between this record and the next older one in the list. */
    val gapKm: Double? = null,
)

/** What the detail column shows. */
sealed interface LedgerSelection {
    data class Record(val id: String) : LedgerSelection
}

data class LedgerState(
    val tab: LedgerTab = LedgerTab.RECORDS,
    val filter: RecordFilter = RecordFilter.ALL,
    val items: List<LedgerItemUi> = emptyList(),
    /** Newest month first; [monthIndex] picks the one the statistics tab describes. */
    val months: List<MonthLedger> = emptyList(),
    val monthIndex: Int = 0,
    val isEv: Boolean = true,
    val selection: LedgerSelection? = null,
    /** Digits typed into the amount field of the selected draft. */
    val amountInput: String = "",
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
) {
    val selectedRecord: LedgerItemUi?
        get() = (selection as? LedgerSelection.Record)?.let { s -> items.firstOrNull { it.id == s.id } }
}

sealed interface LedgerEvent {
    data class TabSelected(val tab: LedgerTab) : LedgerEvent
    data class FilterSelected(val filter: RecordFilter) : LedgerEvent
    data class RecordTapped(val id: String) : LedgerEvent
    /** Back from a detail shown in place of the list (one-third window). */
    data object BackTapped : LedgerEvent
    /** -1 for the month before, +1 for the month after. */
    data class MonthStepped(val delta: Int) : LedgerEvent
    data class AmountChanged(val digits: String) : LedgerEvent
    data class SaveDraftTapped(val id: String) : LedgerEvent
    data class DismissDraftTapped(val id: String) : LedgerEvent

    data class EntriesChanged(val items: List<LedgerItemUi>, val months: List<MonthLedger>, val isEv: Boolean) : LedgerEvent
    data class DraftHandled(val message: String) : LedgerEvent
}

sealed interface LedgerEffect {
    data class ShowMessage(val text: String) : LedgerEffect
}
