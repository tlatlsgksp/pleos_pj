package com.example.pleos.ui.feature.care

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pleos.domain.usecase.ObserveCareItemsUseCase
import com.example.pleos.domain.usecase.RecordMaintenanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
class CareViewModel @Inject constructor(
    observeCareItems: ObserveCareItemsUseCase,
    private val recordMaintenance: RecordMaintenanceUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CareState())
    val state: StateFlow<CareState> = _state.asStateFlow()

    private val _effects = Channel<CareEffect>(Channel.BUFFERED)
    val effects: Flow<CareEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeCareItems().collect { items -> onEvent(CareEvent.ItemsChanged(items.map { it.toUi() })) }
        }
    }

    fun onEvent(event: CareEvent) {
        when (event) {
            is CareEvent.ItemsChanged -> _state.update { it.copy(items = event.items, isLoading = false) }
            is CareEvent.ServicedTapped -> {
                if (_state.value.savingItemId != null) return
                val name = _state.value.items.firstOrNull { it.id == event.itemId }?.name ?: return
                _state.update { it.copy(savingItemId = event.itemId) }
                viewModelScope.launch {
                    val result = runCatching { recordMaintenance(listOf(event.itemId)) }
                    onEvent(
                        result.fold(
                            onSuccess = { CareEvent.ServiceSaved(name) },
                            onFailure = { CareEvent.ServiceFailed(it.message ?: "알 수 없는 오류") },
                        ),
                    )
                }
            }
            is CareEvent.ServiceSaved -> {
                _state.update { it.copy(savingItemId = null) }
                _effects.trySend(CareEffect.ShowMessage("${event.itemName} 교체를 기록했어요"))
            }
            is CareEvent.ServiceFailed -> {
                _state.update { it.copy(savingItemId = null) }
                _effects.trySend(CareEffect.ShowMessage("기록하지 못했어요: ${event.reason}"))
            }
            CareEvent.BackTapped -> _effects.trySend(CareEffect.NavigateBack)
        }
    }
}
