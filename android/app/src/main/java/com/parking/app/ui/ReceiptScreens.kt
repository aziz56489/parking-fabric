package com.parking.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.ParkingViewModel
import com.parking.app.formatDuration
import com.parking.app.formatMoney
import com.parking.app.formatTime
import com.parking.app.receiptLines

/** SCRUM-72: the receipt for the time spent, which can be shared or saved. */
@Composable
fun ReceiptScreen(vm: ParkingViewModel, reservationId: Int, onBack: () -> Unit) {
    LaunchedEffect(reservationId) { vm.loadReceipt(reservationId) }
    val receipt = vm.receipt?.takeIf { it.id == reservationId }
    val context = LocalContext.current

    AppScaffold(vm, title = "Receipt", onBack = onBack) { padding ->
        if (receipt == null) {
            Loading(Modifier.padding(padding))
            return@AppScaffold
        }
        val lines = receiptLines(receipt)
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lines.forEachIndexed { index, (label, value) ->
                        val isTotal = index == lines.lastIndex
                        if (isTotal) HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f))
                            Text(
                                value,
                                style = if (isTotal) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isTotal) FontWeight.Bold else null,
                            )
                        }
                    }
                }
            }
            Button(
                onClick = { shareText(context, lines.joinToString("\n") { (label, value) -> "$label: $value" }) },
                Modifier.fillMaxWidth(),
            ) {
                Text("Share or save receipt")
            }
        }
    }
}

/** All the user's past reservations. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsScreen(vm: ParkingViewModel, onBack: () -> Unit, onOpenReceipt: (Int) -> Unit) {
    LaunchedEffect(Unit) { vm.refreshReservations() }
    val receipts = vm.history.filter { !it.active }

    AppScaffold(vm, title = "My receipts", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (receipts.isEmpty()) {
                item { Text("No receipts yet. They appear here after you mark your exit.") }
            }
            items(receipts, key = { it.id }) { receipt ->
                Card(onClick = { onOpenReceipt(receipt.id) }) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${receipt.areaName} · ${receipt.spotNumber}", style = MaterialTheme.typography.titleMedium)
                            Text("${formatTime(receipt.startedAt)} · ${formatDuration(receipt.minutes)}")
                        }
                        Text(receipt.amount?.let { formatMoney(it, receipt.currency) } ?: "", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
