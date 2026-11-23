package com.parking.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.parking.app.ItemKind
import com.parking.app.ParkingSpot
import com.parking.app.ParkingViewModel
import com.parking.app.SpotStatus
import com.parking.app.floorName
import com.parking.app.ui.theme.ParkingTheme
import kotlinx.coroutines.delay

/** SCRUM-69: see the plan of the area and reserve a free spot. */
@Composable
fun PlanScreen(vm: ParkingViewModel, areaId: Int, onBack: () -> Unit, onReserved: () -> Unit) {
    // Reload every 15 seconds so spots taken by other users turn grey.
    LaunchedEffect(areaId) {
        while (true) {
            vm.openArea(areaId)
            delay(15_000)
        }
    }
    val info = vm.currentArea?.takeIf { it.area.id == areaId }
    val context = LocalContext.current
    var floor by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val multiFloor = (info?.layout?.floors?.size ?: 0) > 1

    AppScaffold(
        vm,
        title = info?.area?.name ?: "Parking area",
        onBack = onBack,
        heroExtra = if (multiFloor) 140.dp else 80.dp,
        actions = {
            if (info != null) {
                HeroIconButton(Icons.Default.Place, "Location on Google Maps", { openDirections(context, info.location, info.area.name) }, Modifier.padding(end = 8.dp))
            }
        },
    ) { padding ->
        val layout = info?.layout
        if (info == null) {
            Loading(Modifier.padding(padding))
            return@AppScaffold
        }
        if (layout == null || layout.floors.isEmpty()) {
            EmptyState(
                Icons.Default.Info,
                "No plan yet",
                "This parking area has no plan yet.",
                Modifier.padding(padding).padding(top = 64.dp),
            )
            return@AppScaffold
        }
        val spots = remember(info) { info.area.spots.associateBy { it.number } }
        val statuses = remember(spots) { spots.mapValues { it.value.status } }
        val level = floor.coerceIn(0, layout.floors.lastIndex)
        val brand = ParkingTheme.brand

        Column(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill("${info.freeSpots} free spots", Color(0xFF6EE7B7))
                Spacer(Modifier.weight(1f))
                Text("Pinch to zoom", style = MaterialTheme.typography.labelMedium, color = brand.onHeroMuted)
            }
            if (layout.floors.size > 1) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    layout.floors.forEachIndexed { index, items ->
                        val free = items.count { it.kind == ItemKind.SPOT && statuses[it.number] == SpotStatus.AVAILABLE }
                        FloorChip("${floorName(index)} · $free free", index == level) { floor = index; selected = null }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            key(areaId, level) {
                SoftCard(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), padding = 0.dp) {
                    ParkingPlan(
                        items = layout.floors[level],
                        planWidth = layout.width,
                        planHeight = layout.height,
                        statuses = statuses,
                        highlighted = selected,
                        onSpotTap = { selected = it.number },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            PlanLegend("Selected", Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            SpotPanel(
                spot = selected?.let { spots[it] },
                hasReservation = vm.reservation != null,
                working = vm.working,
                onReserve = { spot ->
                    vm.reserve(spot) {
                        info.location?.let { point ->
                            openDirections(context, point, info.area.name)
                        }
                        onReserved()
                    }
                },
            )
        }
    }
}

/** Floor selector pill that sits on the gradient band. */
@Composable
private fun FloorChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Color(0xFF1E1B4B) else Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun SpotPanel(spot: ParkingSpot?, hasReservation: Boolean, working: Boolean, onReserve: (ParkingSpot) -> Unit) {
    val brand = ParkingTheme.brand
    Surface(
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        shadowElevation = 18.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        AnimatedContent(
            spot,
            transitionSpec = { (fadeIn() + slideInVertically { it / 4 }) togetherWith fadeOut() },
            label = "spotPanel",
        ) { current ->
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp)) {
                if (current == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(icon = Icons.Default.Info, size = 44.dp)
                        Column(Modifier.padding(start = 14.dp)) {
                            Text("Pick your spot", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tap a coloured spot on the plan to select it.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    return@AnimatedContent
                }
                val available = current.status == SpotStatus.AVAILABLE
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(glyph = current.number.take(4), tint = Color.White, container = spotColor(current.type), size = 60.dp)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text("Spot ${current.number}", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(current.type.label, spotColor(current.type))
                            StatusPill(current.status.label, if (available) brand.success else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (hasReservation) {
                    Row(
                        Modifier
                            .padding(top = 14.dp)
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "You already have a reservation. Mark your exit before reserving another spot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                GradientButton(
                    "Reserve this spot",
                    onClick = { onReserve(current) },
                    enabled = available && !hasReservation && !working,
                    loading = working,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            }
        }
    }
}
