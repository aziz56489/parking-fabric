package com.parking.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.parking.app.ParkingViewModel
import com.parking.app.SpotType

/** SCRUM-84: Declare my status as handicapped or EV owner (matching schema.sql is_handicapped and is_ev_owner) */
@Composable
fun DeclareStatusScreen(
    vm: ParkingViewModel,
    onBack: () -> Unit,
) {
    val user = vm.currentUser
    var isHandicapped by rememberSaveable { mutableStateOf(user?.isHandicapped ?: false) }
    var isEvOwner by rememberSaveable { mutableStateOf(user?.isEvOwner ?: false) }

    AppScaffold(vm, title = "Declare status", onBack = onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StatusToggle(
                glyph = "♿",
                title = "Handicapped",
                description = "Gives you access to handicapped spots.",
                color = spotColor(SpotType.HANDICAPPED),
                checked = isHandicapped,
                onChange = { isHandicapped = it },
            )
            StatusToggle(
                glyph = "⚡",
                title = "EV owner",
                description = "Gives you access to spots with a charger.",
                color = spotColor(SpotType.EV),
                checked = isEvOwner,
                onChange = { isEvOwner = it },
            )
            GradientButton(
                "Save my status",
                onClick = { vm.declareStatus(isHandicapped, isEvOwner) { onBack() } },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = !vm.working,
                loading = vm.working,
                icon = Icons.Default.Check,
            )
        }
    }
}

/** Whole card toggles; the border lights up in the spot type's colour when on. */
@Composable
private fun StatusToggle(
    glyph: String,
    title: String,
    description: String,
    color: Color,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val border by animateColorAsState(if (checked) color else Color.Transparent, label = "statusBorder")
    SoftCard(
        Modifier
            .fillMaxWidth()
            .border(2.dp, border, MaterialTheme.shapes.large),
        onClick = { onChange(!checked) },
        padding = 18.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(glyph = glyph, tint = color, size = 52.dp)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = color, checkedThumbColor = Color.White),
            )
        }
    }
}
