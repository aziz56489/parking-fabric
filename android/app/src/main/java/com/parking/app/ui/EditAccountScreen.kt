package com.parking.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parking.app.ParkingViewModel

/** SCRUM-67: Change my account details */
@Composable
fun EditAccountScreen(
    vm: ParkingViewModel,
    onBack: () -> Unit,
) {
    val user = vm.currentUser
    var name by rememberSaveable { mutableStateOf(user?.name ?: "") }
    var email by rememberSaveable { mutableStateOf(user?.email ?: "") }

    AppScaffold(vm, title = "Change Account Details", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Update your profile information", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { vm.updateAccount(name, email) { onBack() } },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = !vm.working,
            ) {
                Text("Save changes")
            }
        }
    }
}