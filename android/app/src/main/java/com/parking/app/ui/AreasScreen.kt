package com.parking.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.parking.app.AreaInfo
import com.parking.app.ParkingViewModel
import com.parking.app.formatDistance

/** SCRUM-68: select the parking area, with the closest vacant one first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreasScreen(
    vm: ParkingViewModel,
    onOpenArea: (Int) -> Unit,
    onOpenReservation: () -> Unit,
    onOpenReceipts: () -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.values.any { granted -> granted }) {
            vm.locate()
        } else {
            vm.message = "Without your location, parking areas can't be sorted by distance"
        }
    }
    fun findMe() {
        if (hasLocationPermission(context)) {
            vm.locate()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    var askedForLocation by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        vm.refreshAreas()
        vm.refreshReservations()
        if (!askedForLocation) {
            askedForLocation = true
            findMe()
        }
    }

    AppScaffold(
        vm,
        title = "Find parking",
        actions = {
            TextButton(onClick = onOpenReceipts) { Text("Receipts") }
            IconButton(onClick = { vm.refreshAreas(); findMe() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            vm.reservation?.let { active ->
                item {
                    Card(
                        onClick = onOpenReservation,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("Your reservation", style = MaterialTheme.typography.labelLarge)
                            Text("Spot ${active.spotNumber} · ${active.areaName}", style = MaterialTheme.typography.titleMedium)
                            Text("Tap to see it, mark your exit or report an issue")
                        }
                    }
                }
            }

            item {
                val closest = vm.closestVacant
                when {
                    vm.locating -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Finding your location…", Modifier.padding(start = 12.dp))
                    }
                    vm.location == null -> Card {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("Share your location to find the closest vacant parking.")
                            Button(onClick = { findMe() }, Modifier.padding(top = 8.dp)) { Text("Use my location") }
                        }
                    }
                    closest == null -> Card {
                        Text(
                            "No vacant parking area with a known location right now.",
                            Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                    else -> ClosestCard(closest, onOpenArea)
                }
            }

            item { Text("All parking areas", style = MaterialTheme.typography.titleMedium) }
            if (!vm.areasLoaded) {
                item { CircularProgressIndicator() }
            } else if (vm.areas.isEmpty()) {
                item { Text("No parking areas yet.") }
            }
            items(vm.areas, key = { it.area.id }) { info ->
                Card(onClick = { onOpenArea(info.area.id) }) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(info.area.name, style = MaterialTheme.typography.titleMedium)
                            Text("${info.freeSpots} of ${info.area.spots.size} spots free")
                        }
                        Text(
                            info.distanceMeters?.let(::formatDistance)
                                ?: if (info.location == null) "No location" else "",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClosestCard(info: AreaInfo, onOpenArea: (Int) -> Unit) {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Closest vacant parking", style = MaterialTheme.typography.labelLarge)
            Text(info.area.name, style = MaterialTheme.typography.headlineSmall)
            Text("${formatDistance(info.distanceMeters ?: 0.0)} away · ${info.freeSpots} free spots")
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOpenArea(info.area.id) }) { Text("Choose a spot") }
                info.location?.let { point ->
                    OutlinedButton(onClick = { openDirections(context, point, info.area.name) }) { Text("Directions") }
                }
            }
        }
    }
}
