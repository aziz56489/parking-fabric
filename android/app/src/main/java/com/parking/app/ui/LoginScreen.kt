package com.parking.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.parking.app.ParkingViewModel

/** SCRUM-63: Log in use case */
@Composable
fun LoginScreen(
    vm: ParkingViewModel,
    onBack: (() -> Unit)?,
    onLoggedIn: () -> Unit,
    onNavigateRegister: () -> Unit,
    onBanned: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    AuthLayout(
        vm,
        title = "Welcome back",
        subtitle = "Log in to find and reserve your parking spot in seconds.",
        onBack = onBack,
        switchPrompt = "Don't have an account?",
        switchAction = "Register",
        onSwitch = onNavigateRegister,
    ) {
        AppTextField(email, { email = it }, "Email", Icons.Default.Email, keyboardType = KeyboardType.Email)
        AppTextField(password, { password = it }, "Password", Icons.Default.Lock, keyboardType = KeyboardType.Password, password = true)
        GradientButton(
            "Log in",
            onClick = { vm.login(email, password, onLoggedIn, onBanned) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !vm.working,
            loading = vm.working,
            icon = Icons.AutoMirrored.Filled.ArrowForward,
        )
    }
}
