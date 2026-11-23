package com.parking.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parking.app.ParkingViewModel
import com.parking.app.SpotType
import com.parking.app.UserAccount
import com.parking.app.ui.theme.ParkingTheme

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

    AppScaffold(vm, title = "My profile", onBack = onBack, heroExtra = if (user == null) 40.dp else 150.dp) { padding ->
        if (user == null) {
            Column(
                Modifier.padding(padding).fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SoftCard(Modifier.fillMaxWidth()) {
                    EmptyState(Icons.Default.Face, "You are not logged in", "Log in to reserve spots and keep your receipts.")
                    GradientButton("Log in / Register", onNavigateLogin, Modifier.fillMaxWidth())
                }
            }
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProfileHeader(user)

                SoftCard(Modifier.fillMaxWidth()) {
                    OverlineText("Account")
                    Spacer(Modifier.height(10.dp))
                    InfoRow("Name", user.name)
                    InfoRow("Email", user.email)
                    InfoRow("Role", user.role.lowercase().replaceFirstChar { it.uppercase() })
                    InfoRow("Member ID", "#${user.id}")
                    Spacer(Modifier.height(10.dp))
                    OverlineText("Declared status")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(
                            "♿ Handicapped: ${if (user.isHandicapped) "Yes" else "No"}",
                            if (user.isHandicapped) spotColor(SpotType.HANDICAPPED) else MaterialTheme.colorScheme.onSurfaceVariant,
                            dot = false,
                        )
                        StatusPill(
                            "⚡ EV owner: ${if (user.isEvOwner) "Yes" else "No"}",
                            if (user.isEvOwner) spotColor(SpotType.EV) else MaterialTheme.colorScheme.onSurfaceVariant,
                            dot = false,
                        )
                    }
                }

                SoftCard(Modifier.fillMaxWidth(), padding = 6.dp) {
                    MenuRow(Icons.Default.Edit, "Change my account details", "Name and email", onNavigateEdit)
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    MenuRow(Icons.Default.Star, "Declare status", "Handicapped or EV owner", onNavigateDeclareStatus)
                }

                SoftButton(
                    "Log out",
                    onClick = { vm.logout { onBack() } },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp),
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

/** Avatar with initials, name and status, on the gradient band. */
@Composable
private fun ProfileHeader(user: UserAccount) {
    val brand = ParkingTheme.brand
    val initials = user.name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(brand.accentGradient)
                .border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = 30.sp)
        }
        Column(Modifier.padding(start = 18.dp).weight(1f)) {
            Text(user.name, style = MaterialTheme.typography.headlineSmall, color = brand.onHero, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(user.email, style = MaterialTheme.typography.bodyMedium, color = brand.onHeroMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            StatusPill(user.statusLabel, Color.White)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon = icon, size = 44.dp)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
