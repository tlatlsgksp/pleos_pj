package com.example.pleos.ui.feature.riding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import com.example.pleos.ui.design.TWO_COLUMN_MIN_WIDTH_DP
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.pleos.R
import com.example.pleos.domain.model.Member
import com.example.pleos.domain.model.RideMembership
import com.example.pleos.domain.model.Riding
import com.example.pleos.ui.design.BarDivider
import com.example.pleos.ui.design.ButtonStyle
import com.example.pleos.ui.design.FloatingPanel
import com.example.pleos.ui.design.HomeCard
import com.example.pleos.ui.design.MemberAvatar
import com.example.pleos.ui.design.PageNavigation
import com.example.pleos.ui.design.PleosButton
import com.example.pleos.ui.design.DEMO_ROUTE_GANGNEUNG
import com.example.pleos.ui.design.MapControls
import com.example.pleos.ui.design.PleosMap
import com.naver.maps.map.compose.CameraPositionState
import com.naver.maps.map.compose.rememberCameraPositionState
import com.example.pleos.ui.design.PleosIconButton
import com.example.pleos.ui.design.PleosText
import com.example.pleos.ui.design.RoadSign
import com.example.pleos.ui.design.destinationArrow
import com.example.pleos.ui.design.SignKind
import com.example.pleos.ui.design.Space
import com.example.pleos.ui.design.TalkButton
import com.example.pleos.ui.theme.Pleos

/**
 * Group riding (docs/visual/wireframes.html E2-E8), like a voice channel: the ride list with
 * invitations, creating a ride, and the room with a floating bar at the bottom. Press-to-talk sits
 * at the far left, nearest the driver.
 */
@Composable
fun RidingScreen(
    state: RidingState,
    onBack: () -> Unit,
    vm: RidingActions,
) {
    Box(Modifier.fillMaxSize().background(Pleos.colors.background)) {
        when (state.mode) {
            RidingMode.LIST -> RideList(state, onBack, vm)
            RidingMode.CREATE -> CreateRide(state, vm)
            RidingMode.ROOM -> state.room?.let { Room(it, state, onBack, vm) } ?: RideList(state, onBack, vm)
        }
    }
}

/** What the screen can ask of the ViewModel; kept small so previews can pass a no-op. */
interface RidingActions {
    fun openRoom(id: String)
    fun openList()
    fun openCreate()
    fun toggleInvitee(id: String)
    fun create()
    fun join(id: String)
    fun leave(id: String)
    fun decline(id: String)
    fun setTalking(talking: Boolean)
    fun toggleSound()
}

@Composable
private fun RideList(state: RidingState, onBack: () -> Unit, vm: RidingActions) {
    val c = Pleos.colors
    Column(Modifier.fillMaxSize().padding(horizontal = Space.l).verticalScroll(rememberScrollState())) {
        PageNavigation(stringResource(R.string.ride_title), onBack = onBack) {
            PleosButton(stringResource(R.string.ride_create), vm::openCreate, style = ButtonStyle.Filled)
        }
        val mine = state.ridings.filter { it.membership == RideMembership.IN || it.membership == RideMembership.LEFT }
        val invites = state.ridings.filter { it.membership == RideMembership.INVITED }
        if (state.ridings.isEmpty()) {
            Text(stringResource(R.string.ride_list_empty), style = PleosText.body, color = c.textSecondary, modifier = Modifier.padding(vertical = Space.l))
        }
        if (mine.isNotEmpty()) SectionLabel(stringResource(R.string.ride_list_mine))
        mine.forEach { r -> RideRow(r, state.me) {
            if (r.membership == RideMembership.IN) PleosButton(stringResource(R.string.ride_view), { vm.openRoom(r.id) }, style = ButtonStyle.Filled)
            else PleosButton(stringResource(R.string.ride_rejoin), { vm.join(r.id) }, style = ButtonStyle.Filled)
        } }
        if (invites.isNotEmpty()) SectionLabel(stringResource(R.string.ride_list_invites))
        invites.forEach { r -> RideRow(r, state.me) {
            PleosButton(stringResource(R.string.ride_join), { vm.join(r.id) }, style = ButtonStyle.Filled)
            PleosButton(stringResource(R.string.ride_decline), { vm.decline(r.id) })
        } }
        Spacer(Modifier.padding(bottom = Space.xl))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = PleosText.label, color = Pleos.colors.textSecondary, modifier = Modifier.padding(top = Space.m, bottom = Space.xs))
}

@Composable
private fun RideRow(r: Riding, me: Member?, actions: @Composable () -> Unit) {
    val c = Pleos.colors
    HomeCard(Modifier.fillMaxWidth().padding(bottom = Space.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(r.name, style = PleosText.cardTitle, color = c.textPrimary)
                Text(stringResource(R.string.ride_joined_count, r.joined.size), style = PleosText.body, color = c.textSecondary)
                r.destinationName?.let { RoadSign(it, SignKind.DESTINATION, arrow = r.destinationArrow(me)) }
            }
            // Actions next to the name, nearest the driver; members on the right.
            actions()
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Space.xs, Alignment.End)) { r.joined.take(4).forEach { MemberAvatar(it.nickname, it.handle) } }
        }
    }
}

@Composable
private fun CreateRide(state: RidingState, vm: RidingActions) {
    val c = Pleos.colors
    Column(Modifier.fillMaxSize().padding(horizontal = Space.l).verticalScroll(rememberScrollState())) {
        PageNavigation(stringResource(R.string.ride_create), onBack = vm::openList)
        HomeCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ride_name), style = PleosText.label, color = c.textSecondary)
            Text(state.draftName, style = PleosText.cardTitle, color = c.textPrimary)
        }
        SectionLabel(stringResource(R.string.ride_invite_neighbors))
        state.neighbors.forEach { n ->
            val on = n.id in state.draftInvitees
            HomeCard(Modifier.fillMaxWidth().padding(bottom = Space.xs), onClick = { vm.toggleInvitee(n.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    MemberAvatar(n.nickname, n.handle)
                    Column(Modifier.weight(1f)) {
                        Text(n.nickname, style = PleosText.itemTitle, color = c.textPrimary)
                        Text("@${n.handle}, ${n.carModel}", style = PleosText.label, color = c.textSecondary)
                    }
                    Text(stringResource(if (on) R.string.ride_selected else R.string.ride_select), style = PleosText.status, color = if (on) c.textPrimary else c.textTertiary)
                }
            }
        }
        Row(Modifier.padding(vertical = Space.l), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            PleosButton(stringResource(R.string.common_cancel), vm::openList)
            PleosButton(stringResource(R.string.ride_create_and_join), vm::create, style = ButtonStyle.Filled)
        }
    }
}

@Composable
private fun Room(room: Riding, state: RidingState, onBack: () -> Unit, vm: RidingActions) {
    val c = Pleos.colors
    val inRoom = room.membership == RideMembership.IN
    val camera = rememberCameraPositionState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        RideMap(room, state.speakerId, camera)
        MapControls(camera, state.me, Modifier.align(Alignment.CenterStart).padding(Space.l))
        Row(Modifier.align(Alignment.TopStart).padding(Space.l), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            PleosIconButton(R.drawable.ic_arrow_back, stringResource(R.string.ride_back), onBack)
            Text(room.name, style = PleosText.sectionTitle, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            room.destinationName?.let { RoadSign(it, SignKind.DESTINATION, arrow = room.destinationArrow(state.me)) }
        }
        val controls: @Composable RowScope.() -> Unit = {
            TalkButton(
                label = stringResource(if (state.talking) R.string.ride_talking else R.string.ride_talk),
                talking = state.talking,
                enabled = inRoom,
                onTalkingChange = vm::setTalking,
            )
            // Controls next to the talk button, nearest the driver; members to the right.
            if (inRoom) {
                PleosButton(stringResource(if (state.soundOff) R.string.ride_sound_on else R.string.ride_sound_off), vm::toggleSound)
                PleosButton(stringResource(R.string.ride_leave), { vm.leave(room.id) })
            } else {
                PleosButton(stringResource(R.string.ride_rejoin), { vm.join(room.id) }, style = ButtonStyle.Filled)
            }
        }
        val members: @Composable RowScope.() -> Unit = {
            room.joined.forEach { m -> MemberAvatar(m.nickname, m.handle, speaking = m.id == state.speakerId, large = true) }
            if (room.away.isNotEmpty()) BarDivider()
            room.away.forEach { m -> MemberAvatar(m.nickname, m.handle, dim = true) }
        }
        val panel = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(Space.l)
        if (maxWidth.value >= TWO_COLUMN_MIN_WIDTH_DP) {
            FloatingPanel(panel) {
                controls()
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs, Alignment.End), content = members)
            }
        } else {
            // The one-third window: members above, the controls on their own row so nothing is cut off.
            FloatingPanel(panel) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs), content = members)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s), content = controls)
                }
            }
        }
    }
}

/** Where the members are: the ride's route and every member who shares a position, the speaker in green. */
@Composable
private fun RideMap(room: Riding, speakerId: String?, camera: CameraPositionState) {
    PleosMap(
        modifier = Modifier.fillMaxSize(),
        route = DEMO_ROUTE_GANGNEUNG,
        members = room.joined,
        speakerId = speakerId,
        camera = camera,
    )
}
