package com.parking.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parking.app.ItemKind
import com.parking.app.ParkingSpot
import com.parking.app.ParkingViewModel
import com.parking.app.SpotStatus
import com.parking.app.floorName
import kotlinx.coroutines.delay

/** SCRUM-69: see the plan of the area and reserve a free spot. */
@OptIn(ExperimentalMaterial3Api::class)
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
    var floor by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    AppScaffold(vm, title = info?.area?.name ?: "Parking area", onBack = onBack) { padding ->
        val layout = info?.layout
        if (info == null) {
            Loading(Modifier.padding(padding))
            return@AppScaffold
        }
        if (layout == null || layout.floors.isEmpty()) {
            Text("This parking area has no plan yet.", Modifier.padding(padding).padding(16.dp))
            return@AppScaffold
        }
        val spots = remember(info) { info.area.spots.associateBy { it.number } }
        val statuses = remember(spots) { spots.mapValues { it.value.status } }
        val level = floor.coerceIn(0, layout.floors.lastIndex)

        Column(Modifier.padding(padding).fillMaxSize()) {
            if (layout.floors.size > 1) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    layout.floors.forEachIndexed { index, items ->
                        val free = items.count { it.kind == ItemKind.SPOT && statuses[it.number] == SpotStatus.AVAILABLE }
                        FilterChip(
                            selected = index == level,
                            onClick = { floor = index; selected = null },
                            label = { Text("${floorName(index)} · $free free") },
                        )
                    }
                }
            }
            Text(
                "${info.freeSpots} free spots · pinch to zoom, tap a spot",
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            key(areaId, level) {
                ParkingPlan(
                    items = layout.floors[level],
                    planWidth = layout.width,
                    planHeight = layout.height,
                    statuses = statuses,
                    highlighted = selected,
                    onSpotTap = { selected = it.number },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
            PlanLegend("Selected", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            SpotPanel(
                spot = selected?.let { spots[it] },
                hasReservation = vm.reservation != null,
                working = vm.working,
                onReserve = { spot -> vm.reserve(spot, onReserved) },
            )
        }
    }
}

@Composable
private fun SpotPanel(spot: ParkingSpot?, hasReservation: Boolean, working: Boolean, onReserve: (ParkingSpot) -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            if (spot == null) {
                Text("Tap a spot on the plan.")
                return@Column
            }
            Text("Spot ${spot.number}", style = MaterialTheme.typography.titleLarge)
            Text("${spot.type.label} · ${spot.status.label}")
            if (hasReservation) {
                Text(
                    "You already have a reservation. Mark your exit before reserving another spot.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = { onReserve(spot) },
                enabled = spot.status == SpotStatus.AVAILABLE && !hasReservation && !working,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Reserve this spot")
            }
        }
    }
}
