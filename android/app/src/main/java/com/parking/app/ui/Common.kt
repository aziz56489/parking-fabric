package com.parking.app.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.parking.app.GeoPoint
import com.parking.app.ParkingViewModel
import androidx.core.net.toUri

/** Top bar with an optional back button, and a snackbar that shows the view model's messages. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    vm: ParkingViewModel,
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val message = vm.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.message = null
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = actions,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        content = content,
    )
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
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
