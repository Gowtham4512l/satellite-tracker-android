package com.example.myapplication.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.viewmodel.SatelliteViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SatelliteTrackingScreen(
    modifier: Modifier = Modifier,
    viewModel: SatelliteViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Location permissions
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Initialize ViewModel
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    // Permission rationale state
    var showPermissionRationale by remember { mutableStateOf(false) }
    var hasRequestedPermissions by remember { mutableStateOf(false) }

    // Auto-request permissions on first launch
    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted && !hasRequestedPermissions) {
            showPermissionRationale = true
        }
    }

    // Request permissions if needed
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            viewModel.onPermissionGranted()
            if (!uiState.useManualLocation) {
                viewModel.fetchGpsLocation()
            }
        }
    }

    // Manual location state
    var manualLat by remember { mutableStateOf("") }
    var manualLng by remember { mutableStateOf("") }
    var manualAlt by remember { mutableStateOf("") }

    // Permission Rationale Dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Location Permission Required") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This app requires location permissions to:")
                    Text("• Automatically detect your position for satellite tracking")
                    Text("• Calculate satellite azimuth and elevation from your location")
                    Text("• Provide accurate real-time tracking data")
                    Text("")
                    Text("You can also use manual location entry if you prefer not to grant this permission.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    hasRequestedPermissions = true
                    locationPermissions.launchMultiplePermissionRequest()
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    hasRequestedPermissions = true
                }) {
                    Text("Use Manual Location")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Satellite Tracker",
            style = MaterialTheme.typography.headlineMedium
        )

        // NORAD ID Input
        OutlinedTextField(
            value = uiState.noradId,
            onValueChange = { viewModel.onNoradIdChange(it) },
            label = { Text("NORAD ID") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isTracking
        )

        // Location Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Location", style = MaterialTheme.typography.titleMedium)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use Manual Location")
                    Switch(
                        checked = uiState.useManualLocation,
                        onCheckedChange = { viewModel.toggleManualLocation(it) },
                        enabled = !uiState.isTracking
                    )
                }

                if (!uiState.useManualLocation) {
                    // GPS Location
                    if (!locationPermissions.allPermissionsGranted) {
                        // Check if permission was permanently denied
                        val allPermissionsDenied = locationPermissions.permissions.all {
                            !it.status.isGranted && !it.status.shouldShowRationale
                        }

                        if (hasRequestedPermissions && allPermissionsDenied) {
                            // Permission permanently denied - show settings option
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Location Permission Denied",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Location permission is required for automatic satellite tracking. Please enable it in Settings or use manual location entry.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Button(
                                        onClick = {
                                            val intent =
                                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.fromParts(
                                                        "package",
                                                        context.packageName,
                                                        null
                                                    )
                                                }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Open Settings")
                                    }
                                }
                            }
                        } else {
                            // Permission not yet requested or can be requested again
                            Button(
                                onClick = { locationPermissions.launchMultiplePermissionRequest() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Location Permission")
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.fetchGpsLocation() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isTracking
                        ) {
                            Text("Get GPS Location")
                        }

                        uiState.userLocation?.let { loc ->
                            if (!loc.isManual) {
                                Text(
                                    "Lat: ${loc.latitude ?: "N/A"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Lng: ${loc.longitude ?: "N/A"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Alt: ${loc.altitude ?: 0.0}m",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    // Manual Location Input
                    OutlinedTextField(
                        value = manualLat,
                        onValueChange = { manualLat = it },
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isTracking
                    )

                    OutlinedTextField(
                        value = manualLng,
                        onValueChange = { manualLng = it },
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isTracking
                    )

                    OutlinedTextField(
                        value = manualAlt,
                        onValueChange = { manualAlt = it },
                        label = { Text("Altitude (meters)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isTracking
                    )

                    Button(
                        onClick = {
                            val lat = manualLat.toDoubleOrNull()
                            val lng = manualLng.toDoubleOrNull()
                            val alt = manualAlt.toDoubleOrNull() ?: 0.0

                            when {
                                lat == null || lng == null -> {
                                    // Show error via ViewModel
                                    viewModel.onNoradIdChange(uiState.noradId) // Trigger error state update
                                }

                                lat < -90 || lat > 90 -> {
                                    // Invalid latitude range
                                }

                                lng < -180 || lng > 180 -> {
                                    // Invalid longitude range
                                }

                                else -> {
                                    viewModel.onManualLocationChange(lat, lng, alt)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isTracking
                    ) {
                        Text("Set Manual Location")
                    }

                    uiState.userLocation?.let { loc ->
                        if (loc.isManual) {
                            Text(
                                "Set: Lat ${loc.latitude ?: "N/A"}, Lng ${loc.longitude ?: "N/A"}, Alt ${loc.altitude ?: 0.0}m",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Start/Stop Tracking Button
        Button(
            onClick = {
                if (uiState.isTracking) {
                    viewModel.stopTracking()
                } else {
                    viewModel.startTracking()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.userLocation != null
        ) {
            Text(if (uiState.isTracking) "Stop Tracking" else "Start Tracking")
        }

        // Error Display
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Satellite Position Display
        uiState.currentPosition?.let { position ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Satellite: ${position.satName ?: "Unknown"}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Azimuth:", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${String.format("%.2f", position.azimuth ?: 0.0)}°",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Elevation:", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${String.format("%.2f", position.elevation ?: 0.0)}°",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }

                    HorizontalDivider()

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    Text(
                        text = "Updated: ${dateFormat.format(Date((position.timestamp ?: 0L) * 1000))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
