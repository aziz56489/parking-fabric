package com.parking.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parking.app.ParkingViewModel

/** SCRUM-84: Declare my status as handicapped or EV owner (matching schema.sql is_handicapped and is_ev_owner) */
@Composable
fun DeclareStatusScreen(
    vm: ParkingViewModel,
    onBack: () -> Unit,
) {
    val user = vm.currentUser
    var isHandicapped by rememberSaveable { mutableStateOf(user?.isHandicapped ?: false) }
    var isEvOwner by rememberSaveable { mutableStateOf(user?.isEvOwner ?: false) }

    AppScaffold(vm, title = "Declare Status", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Update your status (schema.sql database fields)", style = MaterialTheme.typography.titleMedium)

            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isHandicapped,
                    onCheckedChange = { isHandicapped = it },
                )
                Text(
                    text = "Handicapped status (is_handicapped)",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isEvOwner,
                    onCheckedChange = { isEvOwner = it },
                )
                Text(
                    text = "EV Owner status (is_ev_owner)",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Button(
                onClick = { vm.declareStatus(isHandicapped, isEvOwner) { onBack() } },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                enabled = !vm.working,
            ) {
                Text("Save Status to Database")
            }
        }
    }
}