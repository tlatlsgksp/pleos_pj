package com.example.pleos.ui.feature.riding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pleos.domain.model.Member
import com.example.pleos.domain.model.RideMembership
import com.example.pleos.domain.model.Riding
import com.example.pleos.domain.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RidingMode { LIST, CREATE, ROOM }

data class RidingState(
    val mode: RidingMode = RidingMode.LIST,
    val me: Member? = null,
    val ridings: List<Riding> = emptyList(),
    val neighbors: List<Member> = emptyList(),
    /** The room on screen in [RidingMode.ROOM]. */
    val roomId: String? = null,
    val talking: Boolean = false,
    val soundOff: Boolean = false,
    /** Who is speaking now (demo: other members take turns). */
    val speakerId: String? = null,
    val draftName: String = "",
    val draftInvitees: Set<String> = emptySet(),
) {
    val room: Riding? get() = ridings.firstOrNull { it.id == roomId }
}

@HiltViewModel
class RidingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val community: CommunityRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RidingState())
    val state: StateFlow<RidingState> = _state.asStateFlow()
    private var turns: Job? = null

    init {
        val id: String? = savedState[ARG_ID]
        _state.update { it.copy(mode = if (id.isNullOrEmpty()) RidingMode.LIST else RidingMode.ROOM, roomId = id?.takeIf { s -> s.isNotEmpty() }) }
        viewModelScope.launch {
            combine(community.me, community.ridings, community.neighbors) { me, ridings, neighbors -> Triple(me, ridings, neighbors) }
                .collect { (me, ridings, neighbors) -> _state.update { it.copy(me = me, ridings = ridings, neighbors = neighbors) } }
        }
        startTurns()
    }

    fun openRoom(id: String) = _state.update { it.copy(mode = RidingMode.ROOM, roomId = id) }
    fun openList() = _state.update { it.copy(mode = RidingMode.LIST) }
    fun openCreate() = _state.update { s -> s.copy(mode = RidingMode.CREATE, draftName = "${s.me?.nickname.orEmpty()}의 라이딩", draftInvitees = emptySet()) }
    fun toggleInvitee(id: String) = _state.update { s -> s.copy(draftInvitees = if (id in s.draftInvitees) s.draftInvitees - id else s.draftInvitees + id) }

    fun create() = viewModelScope.launch {
        val s = _state.value
        val id = community.createRiding(s.draftName, null, s.draftInvitees.toList())
        openRoom(id)
    }

    fun join(id: String) = viewModelScope.launch {
        community.join(id)
        openRoom(id)
    }

    fun leave(id: String) = viewModelScope.launch { community.leave(id) }
    fun decline(id: String) = viewModelScope.launch { community.decline(id) }
    fun setTalking(talking: Boolean) = _state.update { it.copy(talking = talking, speakerId = if (talking) it.me?.id else it.speakerId) }
    fun toggleSound() = _state.update { it.copy(soundOff = !it.soundOff) }

    /** Demo only: the other members in the room take turns speaking every few seconds. */
    private fun startTurns() {
        turns = viewModelScope.launch {
            var i = 0
            while (isActive) {
                delay(3_000)
                val s = _state.value
                if (s.talking) continue
                val others = s.room?.joined?.filter { it.id != s.me?.id }.orEmpty()
                val speaker = if (others.isEmpty() || i % 3 == 2) null else others[i % others.size].id
                _state.update { it.copy(speakerId = speaker) }
                i++
            }
        }
    }

    companion object {
        const val ARG_ID = "ridingId"
    }
}

internal fun Riding.isMine(me: Member?) = host.id == me?.id
internal fun Riding.canJoin() = membership == RideMembership.INVITED || membership == RideMembership.LEFT
