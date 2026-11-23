package com.parking.app.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.parking.app.GeoPoint
import com.parking.app.ParkingViewModel
import com.parking.app.ui.theme.Overline
import com.parking.app.ui.theme.ParkingTheme

/** Shows the view model's messages in a rounded snackbar. */
@Composable
fun rememberMessageHost(vm: ParkingViewModel): SnackbarHostState {
    val snackbar = remember { SnackbarHostState() }
    val message = vm.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.message = null
        }
    }
    return snackbar
}

@Composable
fun MessageHost(state: SnackbarHostState) {
    SnackbarHost(state) { data ->
        Snackbar(
            data,
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier.padding(12.dp),
        )
    }
}

/**
 * Gradient band behind the top of the screen, with soft "headlight" circles.
 * [extra] is how far it reaches below the top bar, so the first cards overlap it.
 */
@Composable
fun HeroBand(extra: Dp, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val brand = ParkingTheme.brand
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier
            .fillMaxWidth()
            .height(top + 64.dp + extra)
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(brand.heroGradient)
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.07f), radius = size.width * 0.45f, center = Offset(size.width * 0.95f, 0f))
                drawCircle(Color.White.copy(alpha = 0.05f), radius = size.width * 0.3f, center = Offset(size.width * 0.1f, size.height * 1.05f))
            },
        content = content,
    )
}

/** Top bar over a gradient band, with an optional back button and the view model's messages. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    vm: ParkingViewModel,
    title: String,
    onBack: (() -> Unit)? = null,
    heroExtra: Dp = 48.dp,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val snackbar = rememberMessageHost(vm)
    val onHero = ParkingTheme.brand.onHero
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HeroBand(heroExtra)
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (onBack != null) {
                            HeroIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack, Modifier.padding(start = 8.dp))
                        }
                    },
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = onHero,
                        navigationIconContentColor = onHero,
                        actionIconContentColor = onHero,
                    ),
                )
            },
            snackbarHost = { MessageHost(snackbar) },
            content = content,
        )
    }
}

/** Round, glassy icon button for the gradient top bar. */
@Composable
fun HeroIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier.padding(horizontal = 2.dp).size(42.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.White.copy(alpha = 0.14f),
            contentColor = ParkingTheme.brand.onHero,
        ),
    ) {
        Icon(icon, contentDescription = description, Modifier.size(22.dp))
    }
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
            CircularProgressIndicator(Modifier.padding(14.dp).size(32.dp), strokeWidth = 3.dp)
        }
    }
}

/** White rounded card with a soft, tinted shadow. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    Column(
        modifier
            .shadow(10.dp, shape, ambientColor = tint, spotColor = tint)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Card filled with a gradient, for the moments that matter (closest parking, active reservation). */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val brand = ParkingTheme.brand
    val shape = MaterialTheme.shapes.large
    val glow = if (accent) brand.warning else MaterialTheme.colorScheme.primary
    Column(
        modifier
            .shadow(16.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(if (accent) brand.accentGradient else brand.heroGradient)
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.08f), radius = size.height * 0.9f, center = Offset(size.width, 0f))
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(20.dp),
        content = content,
    )
}

/** Full-width pill button with the brand gradient. Shows a spinner while [loading]. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val brand = ParkingTheme.brand
    val pressScale by animateFloatAsState(if (enabled) 1f else 0.98f, label = "buttonScale")
    val shape = RoundedCornerShape(50)
    val glow = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .scale(pressScale)
            .heightIn(min = 54.dp)
            .then(if (enabled) Modifier.shadow(12.dp, shape, ambientColor = glow, spotColor = glow) else Modifier)
            .alpha(if (enabled || loading) 1f else 0.45f)
            .clip(shape)
            .background(brand.buttonGradient)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Secondary pill button: tinted background, no border. */
@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Row(
        modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(50))
            .background(container)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = content, style = MaterialTheme.typography.labelLarge)
    }
}

/** Rounded square with an icon or a short glyph ("P", "⚡"). */
@Composable
fun IconTile(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    glyph: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    container: Color = tint.copy(alpha = 0.12f),
    size: Dp = 48.dp,
) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(container),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
        if (glyph != null) Text(glyph, color = tint, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.42f).sp)
    }
}

/** Small rounded label with a coloured dot. */
@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier, dot: Boolean = true) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

/** Pulsing "LIVE" dot for things happening right now. */
@Composable
fun LiveDot(color: Color = Color.White, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "live")
    val ring by pulse.animateFloat(
        1f, 2.4f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring",
    )
    Box(modifier.size(20.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(8.dp).scale(ring).alpha((2.4f - ring) / 1.4f * 0.5f).clip(CircleShape).background(color))
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
    }
}

/** Availability colour: green when plenty of room, amber when filling up, red when (almost) full. */
@Composable
fun availabilityColor(free: Int, total: Int): Color {
    val brand = ParkingTheme.brand
    val ratio = if (total == 0) 0f else free.toFloat() / total
    return when {
        free == 0 -> brand.danger
        ratio < 0.25f -> brand.warning
        else -> brand.success
    }
}

/** Rounded progress bar of free spots, animated on change. */
@Composable
fun AvailabilityBar(free: Int, total: Int, modifier: Modifier = Modifier, track: Color = MaterialTheme.colorScheme.surfaceVariant) {
    val target = if (total == 0) 0f else free.toFloat() / total
    val ratio by animateFloatAsState(target, tween(700), label = "availability")
    val color = availabilityColor(free, total)
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxWidth(ratio).height(8.dp).clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun OverlineText(text: String, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = Overline, color = color, modifier = modifier)
}

/** Friendly placeholder for empty lists. */
@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconTile(icon = icon, size = 72.dp)
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** Rounded text field with a leading icon; password fields get a Show / Hide toggle. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = if (password) {
            { TextButton(onClick = { visible = !visible }) { Text(if (visible) "Hide" else "Show") } }
        } else null,
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Brand mark: a "P" on a glowing rounded square. */
@Composable
fun BrandLogo(size: Dp = 72.dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        modifier
            .size(size)
            .shadow(20.dp, shape, ambientColor = ParkingTheme.brand.warning, spotColor = ParkingTheme.brand.warning)
            .clip(shape)
            .background(ParkingTheme.brand.accentGradient)
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text("P", color = Color.White, fontWeight = FontWeight.Black, fontSize = (size.value * 0.5f).sp)
    }
}

fun hasLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/** Opens Google Maps (or any maps app) on the parking area using latitude and longitude. */
fun openDirections(context: Context, point: GeoPoint?, label: String) {
    val uri = if (point != null) {
        // geo:latitude,longitude?q=latitude,longitude(label) opens Google Maps directly at the exact location pin
        ("geo:${point.latitude},${point.longitude}?q=${point.latitude},${point.longitude}(" + Uri.encode(label) + ")").toUri()
    } else {
        ("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(label)).toUri()
    }
    val googleMaps = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(googleMaps)
    } catch (e: ActivityNotFoundException) {
        val webUri = if (point != null) {
            ("https://www.google.com/maps/search/?api=1&query=${point.latitude},${point.longitude}").toUri()
        } else {
            ("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(label)).toUri()
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No maps app or browser installed", Toast.LENGTH_SHORT).show()
        }
    }
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(intent, null))
}
