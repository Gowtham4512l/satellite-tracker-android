package com.example.myapplication.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.example.myapplication.ui.components.GlassCard
import com.example.myapplication.ui.components.SpaceButton
import com.example.myapplication.ui.components.StarfieldBackground
import com.example.myapplication.ui.theme.AuroraGreen
import com.example.myapplication.ui.theme.DeepSpaceBlack
import com.example.myapplication.ui.theme.DeepSpaceBlue
import com.example.myapplication.ui.theme.ErrorRed
import com.example.myapplication.ui.theme.GlassBorder
import com.example.myapplication.ui.theme.NebulaPurple
import com.example.myapplication.ui.theme.StarWhite
import com.example.myapplication.ui.theme.TextPrimary
import com.example.myapplication.ui.theme.TextSecondary
import com.example.myapplication.ui.theme.TextTertiary
import com.example.myapplication.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
    onSettingsSaved: () -> Unit = {},
    isFirstLaunch: Boolean = false,
    isTrackingActive: Boolean = false
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Initialize ViewModel and clear any previous success state
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
        viewModel.clearSuccessMessage() // Clear any lingering success state
    }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    // Navigate away on successful save
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess && viewModel.areSettingsValid()) {
            onSettingsSaved()
            // Clear success state after navigation to prevent re-triggering
            viewModel.clearSuccessMessage()
        }
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
                        text = if (isFirstLaunch) "⚙️ INITIAL SETUP" else "⚙️ SETTINGS",
                        style = MaterialTheme.typography.headlineMedium,
                        color = StarWhite,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isFirstLaunch)
                            "Configure your API key to get started"
                        else
                            "Manage your configuration",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }

                // Info Card (only on first launch)
                if (isFirstLaunch) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = NebulaPurple.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = NebulaPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Welcome to Satellite Tracker!",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StarWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "To use this app, you need a free N2YO API key. This allows you to track satellites in real-time with your own transaction quota.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // API Key Section
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "N2YO API Key",
                            style = MaterialTheme.typography.titleMedium,
                            color = StarWhite,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.isApiKeyValid) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid",
                                tint = AuroraGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.apiKey,
                        onValueChange = { viewModel.onApiKeyChange(it) },
                        label = { Text("API Key", color = TextSecondary) },
                        placeholder = { Text("Enter your N2YO API key", color = TextTertiary) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NebulaPurple,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = StarWhite,
                            unfocusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed,
                            disabledTextColor = TextSecondary,
                            disabledBorderColor = GlassBorder.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        isError = uiState.apiKeyError != null,
                        enabled = !isTrackingActive,
                        supportingText = if (uiState.apiKeyError != null) {
                            { Text(uiState.apiKeyError!!, color = ErrorRed) }
                        } else if (isTrackingActive) {
                            { Text("Cannot change API key while tracking", color = TextSecondary) }
                        } else null
                    )

                    SpaceButton(
                        text = "Get API Key from N2YO",
                        onClick = {
                            val intent =
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.n2yo.com/login/"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isPrimary = false
                    )

                    Text(
                        text = "💡 Sign up on N2YO website to get your free API key. It will be shown in your account settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }

                // BLE MAC Address Section
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BLE Device MAC Address",
                                style = MaterialTheme.typography.titleMedium,
                                color = StarWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Optional",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                        }
                        if (uiState.isBleMacValid && uiState.bleMacAddress.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid",
                                tint = AuroraGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.bleMacAddress,
                        onValueChange = { viewModel.onBleMacChange(it) },
                        label = { Text("MAC Address", color = TextSecondary) },
                        placeholder = { Text("XX:XX:XX:XX:XX:XX", color = TextTertiary) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NebulaPurple,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = StarWhite,
                            unfocusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        shape = RoundedCornerShape(12.dp),
                        isError = uiState.bleMacError != null,
                        supportingText = if (uiState.bleMacError != null) {
                            { Text(uiState.bleMacError!!, color = ErrorRed) }
                        } else null
                    )

                    Text(
                        text = "💡 Enter the MAC address of your IoT device to enable real-time data transmission. You can configure this later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }

                // Save Button
                SpaceButton(
                    text = if (isFirstLaunch) "✓ Complete Setup" else "💾 Save Settings",
                    onClick = { viewModel.saveAllSettings() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.isApiKeyValid && !uiState.isSaving,
                    isLoading = uiState.isSaving
                )

                // Success Message
                AnimatedVisibility(
                    visible = uiState.saveSuccess,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = AuroraGreen.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AuroraGreen
                            )
                            Text(
                                text = if (isFirstLaunch)
                                    "Setup complete! Starting satellite tracker..."
                                else
                                    "Settings saved successfully!",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Validation Requirements (only show if API key is not valid)
                if (!uiState.isApiKeyValid && uiState.apiKey.isNotBlank()) {
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ErrorRed
                                )
                                Text(
                                    text = "API Key Requirements",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ErrorRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "• Must be 10-30 characters long\n• Can contain letters, numbers, hyphens, and underscores",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
