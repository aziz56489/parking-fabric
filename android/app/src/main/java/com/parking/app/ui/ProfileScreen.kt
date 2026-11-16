package com.parking.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parking.app.ParkingViewModel

/** SCRUM-65: Access my profile, SCRUM-66: Log out, SCRUM-67: Change account details, SCRUM-84: Declare status */
@Composable
fun ProfileScreen(
    vm: ParkingViewModel,
    onBack: () -> Unit,
    onNavigateLogin: () -> Unit,
    onNavigateEdit: () -> Unit,
    onNavigateDeclareStatus: () -> Unit,
) {
    val user = vm.currentUser

    AppScaffold(vm, title = "My Profile", onBack = onBack) { padding ->
        if (user == null) {
            Column(
                Modifier.padding(padding).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("You are not logged in", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onNavigateLogin, Modifier.padding(top = 16.dp)) {
                    Text("Log in / Register")
                }
            }
        } else {
            Column(
                Modifier.padding(padding).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Database User Profile", style = MaterialTheme.typography.labelLarge)
                        Text("ID: ${user.id}", style = MaterialTheme.typography.bodySmall)
                        Text("Name: ${user.name}", style = MaterialTheme.typography.titleMedium)
                        Text("Email: ${user.email}", style = MaterialTheme.typography.bodyMedium)
                        Text("Role: ${user.role}", style = MaterialTheme.typography.bodyMedium)
                        Text("Status: ${user.statusLabel}", style = MaterialTheme.typography.bodyMedium)
                        Text("Handicapped (is_handicapped): ${if (user.isHandicapped) "Yes" else "No"}", style = MaterialTheme.typography.bodySmall)
                        Text("EV Owner (is_ev_owner): ${if (user.isEvOwner) "Yes" else "No"}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Button(onClick = onNavigateEdit, modifier = Modifier.fillMaxWidth()) {
                    Text("Change my account details")
                }

                Button(onClick = onNavigateDeclareStatus, modifier = Modifier.fillMaxWidth()) {
                    Text("Declare status (Handicapped / EV)")
                }

                OutlinedButton(
                    onClick = { vm.logout { onBack() } },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text("Log out")
                }
            }
        }
    }
}