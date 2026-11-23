package com.parking.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.parking.app.ParkingViewModel
import com.parking.app.formatDuration
import com.parking.app.formatMoney
import com.parking.app.formatTime
import com.parking.app.receiptLines
import com.parking.app.ui.theme.ParkingTheme

/** SCRUM-72: the receipt for the time spent, which can be shared or saved. */
@Composable
fun ReceiptScreen(vm: ParkingViewModel, reservationId: Int, onBack: () -> Unit) {
    LaunchedEffect(reservationId) { vm.loadReceipt(reservationId) }
    val receipt = vm.receipt?.takeIf { it.id == reservationId }
    val context = LocalContext.current

    AppScaffold(vm, title = "Receipt", onBack = onBack, heroExtra = 72.dp) { padding ->
        if (receipt == null) {
            Loading(Modifier.padding(padding))
            return@AppScaffold
        }
        val lines = receiptLines(receipt)
        val total = lines.lastOrNull()
        val brand = ParkingTheme.brand
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SoftCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IconTile(icon = Icons.Default.CheckCircle, tint = brand.success, size = 64.dp)
                    Spacer(Modifier.height(12.dp))
                    OverlineText("Parking complete", color = brand.success)
                    Spacer(Modifier.height(4.dp))
                    if (total != null) {
                        Text(total.second, style = MaterialTheme.typography.displaySmall)
                    }
                    Text(
                        "${receipt.areaName} · Spot ${receipt.spotNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TicketDivider()
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    lines.forEachIndexed { index, (label, value) ->
                        val isTotal = index == lines.lastIndex
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                label,
                                Modifier.weight(1f),
                                style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                color = if (isTotal) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                value,
                                style = if (isTotal) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                                color = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            GradientButton(
                "Share or save receipt",
                onClick = { shareText(context, lines.joinToString("\n") { (label, value) -> "$label: $value" }) },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                icon = Icons.Default.Share,
            )
        }
    }
}

/** Perforated line across the receipt, like a paper ticket. */
@Composable
private fun TicketDivider() {
    val color = MaterialTheme.colorScheme.outline
    val hole = MaterialTheme.colorScheme.background
    Canvas(Modifier.fillMaxWidth().height(24.dp)) {
        val y = size.height / 2
        drawCircle(hole, radius = 12.dp.toPx(), center = Offset(0f, y))
        drawCircle(hole, radius = 12.dp.toPx(), center = Offset(size.width, y))
        drawLine(
            color,
            Offset(20.dp.toPx(), y),
            Offset(size.width - 20.dp.toPx(), y),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        )
    }
}

/** All the user's past reservations. */
@Composable
fun ReceiptsScreen(vm: ParkingViewModel, onBack: () -> Unit, onOpenReceipt: (Int) -> Unit) {
    LaunchedEffect(Unit) { vm.refreshReservations() }
    val receipts = vm.history.filter { !it.active }

    AppScaffold(vm, title = "My receipts", onBack = onBack, heroExtra = 32.dp) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (receipts.isEmpty()) {
                item {
                    SoftCard(Modifier.fillMaxWidth()) {
                        EmptyState(
                            Icons.Default.DateRange,
                            "No receipts yet",
                            "They appear here after you mark your exit.",
                        )
                    }
                }
            }
            items(receipts, key = { it.id }) { receipt ->
                SoftCard(Modifier.fillMaxWidth(), onClick = { onOpenReceipt(receipt.id) }, padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(glyph = "P", size = 48.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(
                                "${receipt.areaName} · ${receipt.spotNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${formatTime(receipt.startedAt)} · ${formatDuration(receipt.minutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        receipt.amount?.let {
                            StatusPill(formatMoney(it, receipt.currency), MaterialTheme.colorScheme.primary, dot = false)
                        }
                    }
                }
            }
        }
    }
}
