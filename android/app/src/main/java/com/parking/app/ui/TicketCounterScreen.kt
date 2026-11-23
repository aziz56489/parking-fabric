package com.parking.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.parking.app.CounterReservation
import com.parking.app.NonPayingReport
import com.parking.app.ParkingViewModel
import com.parking.app.formatDuration
import com.parking.app.formatMoney
import com.parking.app.formatTime
import com.parking.app.ui.theme.ParkingTheme

/** SCRUM-88: As a ticket counter, I can report a non paying user. */
@Composable
fun TicketCounterScreen(
    vm: ParkingViewModel,
    onBack: (() -> Unit)?,
    onOpenProfile: () -> Unit,
    onNavigateLogin: () -> Unit,
) {
    val user = vm.currentUser
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var reporting by remember { mutableStateOf<CounterReservation?>(null) }

    LaunchedEffect(user?.id) { if (user?.isTicketCounter == true) vm.refreshCounter() }

    AppScaffold(
        vm,
        title = "Ticket counter",
        onBack = onBack,
        heroExtra = if (user?.isTicketCounter == true) 150.dp else 40.dp,
        actions = {
            HeroIconButton(Icons.Default.Refresh, "Refresh", { vm.refreshCounter() })
            HeroIconButton(Icons.Default.AccountCircle, "Profile", onOpenProfile, Modifier.padding(end = 8.dp))
        },
    ) { padding ->
        if (user?.isTicketCounter != true) {
            Column(Modifier.padding(padding).padding(20.dp)) {
                SoftCard(Modifier.fillMaxWidth()) {
                    EmptyState(Icons.Default.Lock, "Ticket counter only", "Log in with a ticket counter account to report non paying users.")
                    GradientButton("Log in", onNavigateLogin, Modifier.fillMaxWidth())
                }
            }
            return@AppScaffold
        }

        val needle = query.trim().lowercase()
        val reservations = vm.counterReservations.filter { r ->
            needle.isEmpty() || listOfNotNull(r.userName, r.userEmail, r.spotNumber, r.areaName)
                .any { it.lowercase().contains(needle) }
        }
        val reports = vm.nonPayingReports.filter { r ->
            needle.isEmpty() || listOfNotNull(r.userName, r.userEmail, r.spotNumber, r.areaName, r.note)
                .any { it.lowercase().contains(needle) }
        }

        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { CounterHeader(vm, tab) { tab = it } }
            item {
                AppTextField(query, { query = it }, "Search name, email or spot", Icons.Default.Search)
            }
            if (!vm.counterLoaded) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            } else if (tab == 0) {
                if (reservations.isEmpty()) {
                    item { EmptyState(Icons.Default.Search, "No reservations", "Reservations made by drivers appear here.") }
                }
                items(reservations, key = { it.id }) { r -> ReservationRow(r, vm.working) { reporting = r } }
            } else {
                if (reports.isEmpty()) {
                    item { EmptyState(Icons.Default.CheckCircle, "No reports", "Users you report as non paying appear here.") }
                }
                items(reports, key = { it.id }) { r -> ReportRow(r) }
            }
        }
    }

    reporting?.let { target ->
        ReportNonPayingDialog(
            reservation = target,
            working = vm.working,
            onDismiss = { reporting = null },
            onConfirm = { note -> vm.reportNonPaying(target, note) { reporting = null } },
        )
    }
}

/** Totals and the Reservations / Reports switch, on the gradient band. */
@Composable
private fun CounterHeader(vm: ParkingViewModel, tab: Int, onTab: (Int) -> Unit) {
    val brand = ParkingTheme.brand
    Column(Modifier.padding(bottom = 4.dp)) {
        Text("Report drivers who leave without paying.", style = MaterialTheme.typography.bodyMedium, color = brand.onHeroMuted)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CounterStat("${vm.counterReservations.count { it.active }}", "parked now", Modifier.weight(1f))
            CounterStat("${vm.counterReservations.count { !it.active }}", "recent exits", Modifier.weight(1f))
            CounterStat("${vm.nonPayingReports.size}", "reported", Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.14f)).padding(4.dp),
        ) {
            listOf("Reservations", "Reports").forEachIndexed { index, label ->
                val selected = tab == index
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color(0xFF1E1B4B) else Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable { onTab(index) }
                        .padding(vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun CounterStat(value: String, label: String, modifier: Modifier = Modifier) {
    val brand = ParkingTheme.brand
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.13f)).padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = brand.onHero)
        Text(label, style = MaterialTheme.typography.labelMedium, color = brand.onHeroMuted)
    }
}

private fun initials(name: String?): String =
    name?.split(' ')?.filter { it.isNotBlank() }?.take(2)?.joinToString("") { it.first().uppercase() }?.ifEmpty { null } ?: "?"

@Composable
private fun ReservationRow(r: CounterReservation, working: Boolean, onReport: () -> Unit) {
    val brand = ParkingTheme.brand
    SoftCard(Modifier.fillMaxWidth(), padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(glyph = initials(r.userName), size = 48.dp)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(r.userName ?: "Guest (no account)", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    r.userEmail ?: "Cannot be reported",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                r.active -> StatusPill("Parked", brand.warning)
                r.amount != null -> Text(formatMoney(r.amount, r.currency), style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "${r.areaName} · Spot ${r.spotNumber} · ${formatTime(r.startedAt)} · ${formatDuration(r.minutes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            r.reported -> StatusPill("Reported as non paying", brand.danger, Modifier.padding(top = 12.dp))
            r.canReport -> SoftButton(
                "Report non-payment",
                onClick = onReport,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                icon = Icons.Default.Warning,
                enabled = !working,
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun ReportRow(r: NonPayingReport) {
    val brand = ParkingTheme.brand
    SoftCard(Modifier.fillMaxWidth(), padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = Icons.Default.Face, tint = brand.danger, size = 48.dp)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(r.userName ?: "User #${r.userId}", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    r.userEmail ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (r.amount != null && r.currency != null) {
                StatusPill(formatMoney(r.amount, r.currency), brand.danger, dot = false)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            listOfNotNull(r.areaName, r.spotNumber?.let { "Spot $it" }, formatTime(r.reportedAt)).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        r.note?.let {
            Text(
                "“$it”",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(12.dp),
            )
        }
        r.ticketCounterName?.let {
            Text("Reported by $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ReportNonPayingDialog(
    reservation: CounterReservation,
    working: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { IconTile(icon = Icons.Default.Warning, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Report ${reservation.userName}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${reservation.userEmail}\n${reservation.areaName} · Spot ${reservation.spotNumber}" +
                        (reservation.amount?.let { " · ${formatMoney(it, reservation.currency)}" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("This user will be reported for leaving without paying.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(1000) },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. Left through the barrier without paying") },
                    minLines = 3,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note) }, enabled = !working) {
                Text("Report", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
