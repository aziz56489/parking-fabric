package com.parking.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parking.app.ParkingViewModel
import com.parking.app.ui.theme.ParkingTheme

/**
 * Full-screen gradient with the brand mark, a title, and the form in a floating card.
 * [switchPrompt] / [switchAction] is the "Already have an account? Log in" line under the card.
 */
@Composable
fun AuthLayout(
    vm: ParkingViewModel,
    title: String,
    subtitle: String,
    onBack: (() -> Unit)?,
    switchPrompt: String,
    switchAction: String,
    onSwitch: () -> Unit,
    form: @Composable ColumnScope.() -> Unit,
) {
    val brand = ParkingTheme.brand
    val snackbar = rememberMessageHost(vm)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Box(
        Modifier
            .fillMaxSize()
            .background(brand.heroGradient)
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.06f), radius = size.width * 0.7f, center = Offset(size.width, 0f))
                drawCircle(Color.White.copy(alpha = 0.05f), radius = size.width * 0.5f, center = Offset(0f, size.height * 0.85f))
                drawCircle(brand.warning.copy(alpha = 0.10f), radius = size.width * 0.25f, center = Offset(size.width * 0.15f, size.height * 0.12f))
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) HeroIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
            }
            Spacer(Modifier.height(16.dp))
            BrandLogo()
            Spacer(Modifier.height(24.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, color = brand.onHero, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = brand.onHeroMuted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))

            AnimatedVisibility(shown, enter = fadeIn() + slideInVertically { it / 6 }) {
                SoftCard(Modifier.fillMaxWidth(), padding = 24.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = form)
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(switchPrompt, color = brand.onHeroMuted, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onSwitch) {
                    Text(switchAction, color = brand.onHero, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Box(Modifier.align(Alignment.BottomCenter).systemBarsPadding().imePadding()) { MessageHost(snackbar) }
    }
}
