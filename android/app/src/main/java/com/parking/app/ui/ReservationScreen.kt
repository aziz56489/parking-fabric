package com.parking.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.parking.app.IssueType
import com.parking.app.ParkingViewModel
import com.parking.app.Reservation
import com.parking.app.billedMinutes
import com.parking.app.cost
import com.parking.app.floorName
import com.parking.app.formatElapsed
import com.parking.app.formatMoney
import com.parking.app.formatTime
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/** The active reservation: where the spot is, SCRUM-71 mark exit, SCRUM-82 report an issue. */
@Composable
fun ReservationScreen(vm: ParkingViewModel, onBack: () -> Unit, onExited: (Reservation) -> Unit) {
    val reservation = vm.reservation
    var confirmExit by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }

    AppScaffold(vm, title = "My reservation", onBack = onBack, heroExtra = 40.dp) { padding ->
        if (reservation == null) {
            EmptyState(
                Icons.Default.DateRange,
                "No active reservation",
                "Pick a spot on a parking plan to reserve it.",
                Modifier.padding(padding).padding(top = 48.dp),
            )
            return@AppScaffold
        }
        LaunchedEffect(reservation.areaId) { vm.openArea(reservation.areaId) }
        val info = vm.currentArea?.takeIf { it.area.id == reservation.areaId }
        val context = LocalContext.current

        Column(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
            GradientCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), accent = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveDot()
                    Spacer(Modifier.width(6.dp))
                    OverlineText("Parked now", color = Color.White)
                    Spacer(Modifier.weight(1f))
                    Text("Since ${formatTime(reservation.startedAt)}", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                }
                Spacer(Modifier.height(4.dp))
                Text("Spot ${reservation.spotNumber}", style = MaterialTheme.typography.displaySmall, color = Color.White)
                Text(reservation.areaName, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
                Spacer(Modifier.height(16.dp))
                RunningCost(reservation)
            }

            // Show the user's spot on the plan, on the right level.
            val layout = info?.layout
            if (info != null && layout != null && layout.floors.isNotEmpty()) {
                val level = layout.floors.indexOfFirst { floor -> floor.any { it.number == reservation.spotNumber } }
                    .coerceAtLeast(0)
                OverlineText(
                    floorName(level),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
                )
                key(reservation.areaId, level) {
                    SoftCard(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), padding = 0.dp) {
                        ParkingPlan(
                            items = layout.floors[level],
                            planWidth = layout.width,
                            planHeight = layout.height,
                            statuses = info.area.spots.associate { it.number to it.status },
                            highlighted = reservation.spotNumber,
                            onSpotTap = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                PlanLegend("Your spot", Modifier.padding(16.dp, 12.dp))
            } else {
                Spacer(Modifier.weight(1f))
            }

            Column(
                Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    info?.location?.let { point ->
                        SoftButton(
                            "Directions",
                            onClick = { openDirections(context, point, reservation.areaName) },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Place,
                        )
                    }
                    SoftButton(
                        "Report an issue",
                        onClick = { reporting = true },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        enabled = !vm.working,
                        container = MaterialTheme.colorScheme.errorContainer,
                        content = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                GradientButton(
                    "Mark my exit",
                    onClick = { confirmExit = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !vm.working,
                    loading = vm.working,
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                )
            }
        }
    }

    if (confirmExit && reservation != null) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            icon = { IconTile(icon = Icons.AutoMirrored.Filled.ExitToApp) },
            title = { Text("Leaving spot ${reservation.spotNumber}?") },
            text = { Text("Your reservation ends now and you get your receipt. The spot becomes free for others.") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; vm.exit(onExited) }) { Text("Mark exit") }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Cancel") } },
        )
    }
    if (reporting) {
        ReportIssueDialog(
            onDismiss = { reporting = false },
            onSend = { type, description ->
                reporting = false
                vm.reportIssue(type, description)
            },
        )
    }
}

/** Time spent and price so far, updated every second, as two glassy tiles. */
@Composable
private fun RunningCost(reservation: Reservation) {
    val start = remember(reservation.startedAt) { Instant.parse(reservation.startedAt) }
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(1_000)
        }
    }
    val price = cost(reservation.pricePerHour, billedMinutes(start, now))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LiveTile("Time", formatElapsed(Duration.between(start, now)), null, Modifier.weight(1f))
        LiveTile(
            "So far",
            formatMoney(price, reservation.currency),
            "${formatMoney(reservation.pricePerHour, reservation.currency)} / hour",
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun LiveTile(label: String, value: String, caption: String?, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontFamily = FontFamily.Monospace)
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun ReportIssueDialog(onDismiss: () -> Unit, onSend: (IssueType, String) -> Unit) {
    var type by remember { mutableStateOf(IssueType.SPOT_TAKEN) }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { IconTile(icon = Icons.Default.Warning, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Report an issue") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                IssueType.entries.forEach { option ->
                    val chosen = type == option
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (chosen) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                            )
                            .selectable(selected = chosen, onClick = { type = option })
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = chosen, onClick = null, modifier = Modifier.padding(8.dp))
                        Text(option.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(1000) },
                    label = { Text("Details (optional)") },
                    minLines = 3,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSend(type, description) }) { Text("Send") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
