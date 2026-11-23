package com.parking.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.parking.app.ParkingViewModel

/** SCRUM-64: Register use case */
@Composable
fun RegisterScreen(
    vm: ParkingViewModel,
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    onNavigateLogin: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    AuthLayout(
        vm,
        title = "Create your account",
        subtitle = "Join ParkingApp and never circle the block again.",
        onBack = onBack,
        switchPrompt = "Already have an account?",
        switchAction = "Log in",
        onSwitch = onNavigateLogin,
    ) {
        AppTextField(name, { name = it }, "Full name", Icons.Default.Person)
        AppTextField(email, { email = it }, "Email", Icons.Default.Email, keyboardType = KeyboardType.Email)
        AppTextField(password, { password = it }, "Password", Icons.Default.Lock, keyboardType = KeyboardType.Password, password = true)
        GradientButton(
            "Create account",
            onClick = { vm.register(name, email, password, onRegistered) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !vm.working,
            loading = vm.working,
        )
    }
}
