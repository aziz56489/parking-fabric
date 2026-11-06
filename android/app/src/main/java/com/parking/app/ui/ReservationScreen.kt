package com.parking.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
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

    AppScaffold(vm, title = "My reservation", onBack = onBack) { padding ->
        if (reservation == null) {
            Text("You have no active reservation.", Modifier.padding(padding).padding(16.dp))
            return@AppScaffold
        }
        LaunchedEffect(reservation.areaId) { vm.openArea(reservation.areaId) }
        val info = vm.currentArea?.takeIf { it.area.id == reservation.areaId }
        val context = LocalContext.current

        Column(Modifier.padding(padding).fillMaxSize()) {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Spot ${reservation.spotNumber}", style = MaterialTheme.typography.headlineMedium)
                    Text(reservation.areaName, style = MaterialTheme.typography.titleMedium)
                    Text("Since ${formatTime(reservation.startedAt)}")
                    RunningCost(reservation)
                }
            }

            // Show the user's spot on the plan, on the right level.
            val layout = info?.layout
            if (info != null && layout != null && layout.floors.isNotEmpty()) {
                val level = layout.floors.indexOfFirst { floor -> floor.any { it.number == reservation.spotNumber } }
                    .coerceAtLeast(0)
                Text(
                    floorName(level),
                    Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                key(reservation.areaId, level) {
                    ParkingPlan(
                        items = layout.floors[level],
                        planWidth = layout.width,
                        planHeight = layout.height,
                        statuses = info.area.spots.associate { it.number to it.status },
                        highlighted = reservation.spotNumber,
                        onSpotTap = null,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
                PlanLegend("Your spot", Modifier.padding(16.dp, 8.dp))
            } else {
                Spacer(Modifier.weight(1f))
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    info?.location?.let { point ->
                        OutlinedButton(
                            onClick = { openDirections(context, point, reservation.areaName) },
                            Modifier.weight(1f),
                        ) { Text("Directions") }
                    }
                    OutlinedButton(onClick = { reporting = true }, Modifier.weight(1f), enabled = !vm.working) {
                        Text("Report an issue")
                    }
                }
                Button(onClick = { confirmExit = true }, Modifier.fillMaxWidth(), enabled = !vm.working) {
                    Text("Mark my exit")
                }
            }
        }
    }

    if (confirmExit && reservation != null) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
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

/** Time spent and price so far, updated every second. */
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
    Text("Time: ${formatElapsed(Duration.between(start, now))}")
    Text(
        "So far: ${formatMoney(price, reservation.currency)} " +
            "(${formatMoney(reservation.pricePerHour, reservation.currency)} / hour)",
    )
}

@Composable
private fun ReportIssueDialog(onDismiss: () -> Unit, onSend: (IssueType, String) -> Unit) {
    var type by remember { mutableStateOf(IssueType.SPOT_TAKEN) }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report an issue") },
        text = {
            Column {
                IssueType.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = type == option, onClick = { type = option })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = type == option, onClick = null)
                        Text(option.label, Modifier.padding(start = 8.dp))
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(1000) },
                    label = { Text("Details (optional)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSend(type, description) }) { Text("Send") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
