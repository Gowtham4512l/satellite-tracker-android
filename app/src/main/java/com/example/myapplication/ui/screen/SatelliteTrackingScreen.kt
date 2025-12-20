package com.example.myapplication.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.components.AnimatedMetricCard
import com.example.myapplication.ui.components.CompassVisualization
import com.example.myapplication.ui.components.GlassCard
import com.example.myapplication.ui.components.SpaceButton
import com.example.myapplication.ui.components.StarfieldBackground
import com.example.myapplication.ui.theme.AuroraGreen
import com.example.myapplication.ui.theme.CosmicBlue
import com.example.myapplication.ui.theme.DeepSpaceBlack
import com.example.myapplication.ui.theme.DeepSpaceBlue
import com.example.myapplication.ui.theme.ErrorRed
import com.example.myapplication.ui.theme.GlassBorder
import com.example.myapplication.ui.theme.NebulaPink
import com.example.myapplication.ui.theme.NebulaPurple
import com.example.myapplication.ui.theme.StarWhite
import com.example.myapplication.ui.theme.TextPrimary
import com.example.myapplication.ui.theme.TextSecondary
import com.example.myapplication.ui.theme.TextTertiary
import com.example.myapplication.viewmodel.SatelliteViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalAnimationApi::class)
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

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    // Permission Rationale Dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = {
                Text(
                    "Location Permission Required",
                    color = StarWhite
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This app requires location permissions to:", color = TextPrimary)
                    Text(
                        "• Automatically detect your position for satellite tracking",
                        color = TextSecondary
                    )
                    Text(
                        "• Calculate satellite azimuth and elevation from your location",
                        color = TextSecondary
                    )
                    Text("• Provide accurate real-time tracking data", color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "You can also use manual location entry if you prefer not to grant this permission.",
                        color = TextTertiary
                    )
                }
            },
            confirmButton = {
                SpaceButton(
                    text = "Grant Permission",
                    onClick = {
                        showPermissionRationale = false
                        hasRequestedPermissions = true
                        locationPermissions.launchMultiplePermissionRequest()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    hasRequestedPermissions = true
                }) {
                    Text("Use Manual Location", color = TextSecondary)
                }
            },
            containerColor = DeepSpaceBlue,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Starfield background
        StarfieldBackground()

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DeepSpaceBlack.copy(alpha = 0.7f),
                            DeepSpaceBlue.copy(alpha = 0.5f),
                            DeepSpaceBlack.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Main content
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(1000, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🛰️ SATELLITE TRACKER",
                        style = MaterialTheme.typography.headlineMedium,
                        color = StarWhite,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (uiState.isTracking) "TRACKING ACTIVE" else "READY TO TRACK",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.isTracking) AuroraGreen else TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                // Compass Visualization (only when tracking)
                AnimatedVisibility(
                    visible = uiState.isTracking && uiState.currentPosition != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    uiState.currentPosition?.let { position ->
                        GlassCard {
                            CompassVisualization(
                                azimuth = position.azimuth ?: 0.0,
                                elevation = position.elevation ?: 0.0,
                                satelliteName = position.satName ?: "Unknown",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // NORAD ID Input
                GlassCard {
                    Text(
                        text = "Satellite Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        color = StarWhite,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = uiState.noradId,
                        onValueChange = { viewModel.onNoradIdChange(it) },
                        label = { Text("NORAD ID", color = TextSecondary) },
                        placeholder = { Text("e.g., 25544 for ISS", color = TextTertiary) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isTracking,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NebulaPurple,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = StarWhite,
                            unfocusedTextColor = TextPrimary,
                            disabledTextColor = TextSecondary,
                            disabledBorderColor = GlassBorder.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Location Section
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Observer Location",
                            style = MaterialTheme.typography.titleMedium,
                            color = StarWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (uiState.useManualLocation) "Manual" else "GPS",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Switch(
                                checked = uiState.useManualLocation,
                                onCheckedChange = { viewModel.toggleManualLocation(it) },
                                enabled = !uiState.isTracking,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NebulaPink,
                                    checkedTrackColor = NebulaPink.copy(alpha = 0.5f),
                                    uncheckedThumbColor = CosmicBlue,
                                    uncheckedTrackColor = CosmicBlue.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    if (!uiState.useManualLocation) {
                        // GPS Location
                        if (!locationPermissions.allPermissionsGranted) {
                            val allPermissionsDenied = locationPermissions.permissions.all {
                                !it.status.isGranted && !it.status.shouldShowRationale
                            }

                            if (hasRequestedPermissions && allPermissionsDenied) {
                                // Permission permanently denied
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = ErrorRed.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = ErrorRed
                                            )
                                            Text(
                                                text = "Location Permission Denied",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = ErrorRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "Enable location permission in Settings or use manual location entry.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        SpaceButton(
                                            text = "Open Settings",
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
                                            modifier = Modifier.fillMaxWidth(),
                                            isPrimary = false
                                        )
                                    }
                                }
                            } else {
                                SpaceButton(
                                    text = "Grant Location Permission",
                                    onClick = { locationPermissions.launchMultiplePermissionRequest() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            SpaceButton(
                                text = "Get GPS Location",
                                onClick = { viewModel.fetchGpsLocation() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isTracking,
                                isLoading = uiState.isLoading && !uiState.isTracking
                            )

                            uiState.userLocation?.let { loc ->
                                if (!loc.isManual) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            AnimatedMetricCard(
                                                label = "Latitude",
                                                value = "${
                                                    String.format(
                                                        "%.4f",
                                                        loc.latitude ?: 0.0
                                                    )
                                                }°",
                                                modifier = Modifier.weight(1f)
                                            )
                                            AnimatedMetricCard(
                                                label = "Longitude",
                                                value = "${
                                                    String.format(
                                                        "%.4f",
                                                        loc.longitude ?: 0.0
                                                    )
                                                }°",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        AnimatedMetricCard(
                                            label = "Altitude",
                                            value = "${
                                                String.format(
                                                    "%.1f",
                                                    loc.altitude ?: 0.0
                                                )
                                            } m",
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Manual Location Input
                        OutlinedTextField(
                            value = manualLat,
                            onValueChange = { manualLat = it },
                            label = { Text("Latitude", color = TextSecondary) },
                            placeholder = { Text("-90 to 90", color = TextTertiary) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isTracking,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NebulaPurple,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = StarWhite,
                                unfocusedTextColor = TextPrimary,
                                disabledTextColor = TextSecondary,
                                disabledBorderColor = GlassBorder.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = manualLng,
                            onValueChange = { manualLng = it },
                            label = { Text("Longitude", color = TextSecondary) },
                            placeholder = { Text("-180 to 180", color = TextTertiary) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isTracking,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NebulaPurple,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = StarWhite,
                                unfocusedTextColor = TextPrimary,
                                disabledTextColor = TextSecondary,
                                disabledBorderColor = GlassBorder.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = manualAlt,
                            onValueChange = { manualAlt = it },
                            label = { Text("Altitude (meters)", color = TextSecondary) },
                            placeholder = { Text("e.g., 100", color = TextTertiary) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isTracking,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NebulaPurple,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = StarWhite,
                                unfocusedTextColor = TextPrimary,
                                disabledTextColor = TextSecondary,
                                disabledBorderColor = GlassBorder.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        SpaceButton(
                            text = "Set Manual Location",
                            onClick = {
                                val lat = manualLat.toDoubleOrNull()
                                val lng = manualLng.toDoubleOrNull()
                                val alt = manualAlt.toDoubleOrNull()

                                // Proper validation with error messages
                                when {
                                    lat == null || lng == null || alt == null -> {
                                        viewModel.setError("Please enter valid numbers for all location fields")
                                    }

                                    lat < -90 || lat > 90 -> {
                                        viewModel.setError("Latitude must be between -90 and 90 degrees")
                                    }

                                    lng < -180 || lng > 180 -> {
                                        viewModel.setError("Longitude must be between -180 and 180 degrees")
                                    }

                                    alt < -500 || alt > 100000 -> {
                                        viewModel.setError("Altitude must be between -500 and 100,000 meters")
                                    }

                                    else -> {
                                        viewModel.onManualLocationChange(lat, lng, alt)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isTracking
                        )

                        uiState.userLocation?.let { loc ->
                            if (loc.isManual) {
                                AnimatedMetricCard(
                                    label = "Manual Location Set",
                                    value = "${
                                        String.format(
                                            "%.2f",
                                            loc.latitude ?: 0.0
                                        )
                                    }°, ${String.format("%.2f", loc.longitude ?: 0.0)}°",
                                    icon = Icons.Default.LocationOn,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Start/Stop Tracking Button
                SpaceButton(
                    text = if (uiState.isTracking) "⏹ STOP TRACKING" else "▶ START TRACKING",
                    onClick = {
                        if (uiState.isTracking) {
                            viewModel.stopTracking()
                        } else {
                            viewModel.startTracking()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.userLocation != null,
                    isPrimary = !uiState.isTracking
                )

                // Error Display
                AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    uiState.error?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = ErrorRed.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ErrorRed
                                )
                                Text(
                                    text = error,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Satellite Position Details (when tracking but not showing compass)
                if (uiState.isTracking && uiState.currentPosition != null) {
                    uiState.currentPosition?.let { position ->
                        GlassCard {
                            Text(
                                text = "Tracking Details",
                                style = MaterialTheme.typography.titleMedium,
                                color = StarWhite,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AnimatedMetricCard(
                                    label = "Azimuth",
                                    value = "${String.format("%.2f", position.azimuth ?: 0.0)}°",
                                    isActive = true,
                                    modifier = Modifier.weight(1f)
                                )
                                AnimatedMetricCard(
                                    label = "Elevation",
                                    value = "${String.format("%.2f", position.elevation ?: 0.0)}°",
                                    isActive = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HorizontalDivider(
                                color = GlassBorder,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Cache SimpleDateFormat to avoid recreating on every recomposition
                            val dateFormat = remember {
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            }

                            // Validate timestamp and format safely
                            val timestamp = position.timestamp ?: 0L
                            val dateStr = when {
                                timestamp <= 0L -> "No timestamp"
                                timestamp > Int.MAX_VALUE -> "Invalid timestamp"
                                else -> try {
                                    dateFormat.format(Date(timestamp * 1000))
                                } catch (e: Exception) {
                                    "Invalid date"
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Last Updated",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Loading indicator
                if (uiState.isLoading && uiState.isTracking) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NebulaPurple,
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Bottom spacing
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
