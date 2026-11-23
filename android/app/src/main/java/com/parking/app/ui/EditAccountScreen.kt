package com.parking.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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

    AppScaffold(vm, title = "Account details", onBack = onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SoftCard(Modifier.fillMaxWidth(), padding = 22.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OverlineText("Your information")
                    Text(
                        "Update the name and email linked to your account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AppTextField(name, { name = it }, "Full name", Icons.Default.Person)
                    AppTextField(email, { email = it }, "Email", Icons.Default.Email, keyboardType = KeyboardType.Email)
                }
            }
            GradientButton(
                "Save changes",
                onClick = { vm.updateAccount(name, email) { onBack() } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !vm.working,
                loading = vm.working,
                icon = Icons.Default.Check,
            )
        }
    }
}
