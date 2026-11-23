package com.parking.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parking.app.BanAppeal
import com.parking.app.ParkingViewModel
import com.parking.app.formatTime
import com.parking.app.ui.theme.ParkingTheme

/** A banned user sees the admin's reason and can appeal. */
@Composable
fun BannedScreen(vm: ParkingViewModel, onBack: () -> Unit) {
    val ban = vm.banInfo
    var appealText by rememberSaveable { mutableStateOf("") }
    val brand = ParkingTheme.brand

    // Without the ban (process restarted, password forgotten), there is nothing to show: back to login.
    LaunchedEffect(ban == null) { if (ban == null) onBack() }
    if (ban == null) return

    AppScaffold(
        vm,
        title = "Account suspended",
        onBack = onBack,
        heroExtra = 64.dp,
        actions = { HeroIconButton(Icons.Default.Refresh, "Refresh", { vm.refreshBanStatus() }, Modifier.padding(end = 8.dp)) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!ban.banned) {
                // The admin accepted the appeal (or lifted the ban) while this screen was open.
                SoftCard(Modifier.fillMaxWidth(), padding = 24.dp) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        IconTile(icon = Icons.Default.CheckCircle, tint = brand.success, size = 72.dp)
                        Spacer(Modifier.height(14.dp))
                        Text("Your account is active again", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        ban.appeal?.adminResponse?.let { AdminAnswer(it) }
                        Spacer(Modifier.height(18.dp))
                        GradientButton("Log in", onBack, Modifier.fillMaxWidth())
                    }
                }
                return@Column
            }

            SoftCard(Modifier.fillMaxWidth(), padding = 24.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconTile(icon = Icons.Default.Lock, tint = brand.danger, size = 72.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (ban.name != null) "${ban.name}, your account is banned" else "Your account is banned",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "An administrator suspended your account. You can't use the app until the ban is lifted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(20.dp))
                OverlineText("Reason given by the admin", color = brand.danger)
                Spacer(Modifier.height(8.dp))
                Text(
                    ban.reason ?: "No reason was given.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = if (ban.reason == null) FontStyle.Italic else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                        .padding(16.dp),
                )
                ban.bannedAt?.let {
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text("Banned on ${formatTime(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            ban.appeal?.let { AppealCard(it) }

            if (ban.canAppeal) {
                SoftCard(Modifier.fillMaxWidth(), padding = 22.dp) {
                    OverlineText(if (ban.appeal == null) "Appeal the ban" else "Appeal again")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Explain to the admin why the ban should be lifted (for example: you paid later, or it was a mistake).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = appealText,
                        onValueChange = { appealText = it.take(1000) },
                        label = { Text("Your appeal") },
                        minLines = 4,
                        shape = MaterialTheme.shapes.medium,
                        supportingText = { Text("${appealText.length} / 1000") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    GradientButton(
                        "Send appeal",
                        onClick = { vm.sendAppeal(appealText) { appealText = "" } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = appealText.isNotBlank() && !vm.working,
                        loading = vm.working,
                        icon = Icons.AutoMirrored.Filled.Send,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** The latest appeal: waiting, rejected (with the admin's answer) or accepted. */
@Composable
private fun AppealCard(appeal: BanAppeal) {
    val brand = ParkingTheme.brand
    val (label, color, icon) = when (appeal.status) {
        "PENDING" -> Triple("Waiting for an admin", brand.warning, Icons.Default.DateRange)
        "REJECTED" -> Triple("Appeal rejected", brand.danger, Icons.Default.Warning)
        else -> Triple("Appeal accepted", brand.success, Icons.Default.CheckCircle)
    }
    SoftCard(Modifier.fillMaxWidth(), padding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = icon, tint = color, size = 44.dp)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                OverlineText("Your appeal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                StatusPill(label, color, Modifier.padding(top = 4.dp))
            }
            Text(formatTime(appeal.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "“${appeal.message}”",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 14.dp),
        )
        appeal.adminResponse?.let { AdminAnswer(it) }
        if (appeal.status == "PENDING") {
            Text(
                "You'll see the admin's answer here. Tap the refresh button above to check.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun AdminAnswer(text: String) {
    Column(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp),
    ) {
        OverlineText("Admin's answer")
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.Unspecified)
    }
}
