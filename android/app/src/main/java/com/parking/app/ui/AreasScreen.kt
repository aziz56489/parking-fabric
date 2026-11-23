package com.parking.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.parking.app.AreaInfo
import com.parking.app.ParkingViewModel
import com.parking.app.formatDistance
import com.parking.app.ui.theme.ParkingTheme

/** SCRUM-68: select the parking area, with the closest vacant one first. */
@Composable
fun AreasScreen(
    vm: ParkingViewModel,
    onOpenArea: (Int) -> Unit,
    onOpenReservation: () -> Unit,
    onOpenReceipts: () -> Unit,
    onOpenProfile: () -> Unit,
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
        heroExtra = 150.dp,
        actions = {
            HeroIconButton(Icons.AutoMirrored.Filled.List, "Receipts", onOpenReceipts)
            HeroIconButton(Icons.Default.Refresh, "Refresh", { vm.refreshAreas(); findMe() })
            HeroIconButton(Icons.Default.AccountCircle, "Profile", onOpenProfile, Modifier.padding(end = 8.dp))
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Greeting(vm) }

            vm.reservation?.let { active ->
                item {
                    GradientCard(Modifier.fillMaxWidth(), accent = true, onClick = onOpenReservation) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LiveDot()
                            Spacer(Modifier.width(6.dp))
                            OverlineText("Your reservation · live", color = Color.White)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Spot ${active.spotNumber}", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                                Text(active.areaName, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
                            }
                            IconTile(
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                tint = Color.White,
                                container = Color.White.copy(alpha = 0.2f),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tap to see it, mark your exit or report an issue",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            item {
                val closest = vm.closestVacant
                when {
                    vm.locating -> SoftCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Text("Finding your location…", Modifier.padding(start = 14.dp), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    vm.location == null -> SoftCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(icon = Icons.Default.LocationOn)
                            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                Text("Where are you?", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Share your location to find the closest vacant parking.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        GradientButton(
                            "Use my location",
                            onClick = { findMe() },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            icon = Icons.Default.LocationOn,
                        )
                    }
                    closest == null -> SoftCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(icon = Icons.Default.Place, tint = ParkingTheme.brand.warning)
                            Text(
                                "No vacant parking area with a known location right now.",
                                Modifier.padding(start = 14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    else -> ClosestCard(closest, onOpenArea)
                }
            }

            item {
                SectionHeader("All parking areas") {
                    if (vm.areasLoaded) StatusPill("${vm.areas.size}", MaterialTheme.colorScheme.primary, dot = false)
                }
            }
            if (!vm.areasLoaded) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (vm.areas.isEmpty()) {
                item { EmptyState(Icons.Default.Place, "No parking areas yet", "New areas appear here as soon as they're published.") }
            }
            items(vm.areas, key = { it.area.id }) { info ->
                AreaCard(info, onOpenArea)
            }
        }
    }
}

/** White text that sits on the gradient band: greeting and live totals. */
@Composable
private fun Greeting(vm: ParkingViewModel) {
    val brand = ParkingTheme.brand
    val firstName = vm.currentUser?.name?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
    Column(Modifier.padding(bottom = 6.dp)) {
        Text(
            if (firstName != null) "Hello, $firstName 👋" else "Hello 👋",
            style = MaterialTheme.typography.titleMedium,
            color = brand.onHeroMuted,
        )
        Text("Where are we parking today?", style = MaterialTheme.typography.headlineSmall, color = brand.onHero)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroStat("${vm.areas.sumOf { it.freeSpots }}", "free spots", Modifier.weight(1f))
            HeroStat("${vm.areas.size}", "areas", Modifier.weight(1f))
            HeroStat(
                vm.closestVacant?.distanceMeters?.let(::formatDistance) ?: "—",
                "closest",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier = Modifier) {
    val brand = ParkingTheme.brand
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = brand.onHero, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelMedium, color = brand.onHeroMuted)
    }
}

@Composable
private fun AreaCard(info: AreaInfo, onOpenArea: (Int) -> Unit) {
    val context = LocalContext.current
    val total = info.area.spots.size
    val color = availabilityColor(info.freeSpots, total)
    SoftCard(Modifier.fillMaxWidth(), onClick = { onOpenArea(info.area.id) }, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(glyph = "P", tint = color, size = 52.dp)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(info.area.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        info.distanceMeters?.let { "${formatDistance(it)} away" }
                            ?: if (info.location == null) "No location" else "Distance unknown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }
            }
            IconButton(
                onClick = { openDirections(context, info.location, info.area.name) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(Icons.Default.Place, contentDescription = "Map")
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvailabilityBar(info.freeSpots, total, Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text("${info.freeSpots}/$total free", style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

@Composable
private fun ClosestCard(info: AreaInfo, onOpenArea: (Int) -> Unit) {
    val context = LocalContext.current
    GradientCard(Modifier.fillMaxWidth()) {
        OverlineText("★ Closest vacant parking", color = ParkingTheme.brand.warning)
        Spacer(Modifier.height(6.dp))
        Text(info.area.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("${formatDistance(info.distanceMeters ?: 0.0)} away", Color.White)
            StatusPill("${info.freeSpots} free spots", Color(0xFF6EE7B7))
        }
        Spacer(Modifier.height(16.dp))
        AvailabilityBar(info.freeSpots, info.area.spots.size, track = Color.White.copy(alpha = 0.18f))
        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton(
                "Choose a spot",
                onClick = { onOpenArea(info.area.id) },
                modifier = Modifier.weight(1f),
                container = Color.White,
                content = Color(0xFF1E1B4B),
            )
            info.location?.let { point ->
                SoftButton(
                    "Directions",
                    onClick = { openDirections(context, point, info.area.name) },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Place,
                    container = Color.White.copy(alpha = 0.16f),
                    content = Color.White,
                )
            }
        }
    }
}
