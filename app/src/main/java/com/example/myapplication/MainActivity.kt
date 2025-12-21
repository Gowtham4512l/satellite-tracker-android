package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.data.repository.SettingsRepository
import com.example.myapplication.ui.screen.SatelliteTrackingScreen
import com.example.myapplication.ui.screen.SettingsScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SatelliteTrackerApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun SatelliteTrackerApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Loading) }
    var isFirstLaunch by remember { mutableStateOf(false) }
    var isTrackingActive by remember { mutableStateOf(false) }
    
    // Check first launch and API key configuration
    LaunchedEffect(Unit) {
        val settingsRepo = SettingsRepository(context)
        val hasApiKey = settingsRepo.hasApiKey()
        isFirstLaunch = settingsRepo.isFirstLaunch()
        
        currentScreen = if (!hasApiKey || isFirstLaunch) {
            Timber.d("First launch or no API key - showing settings")
            Screen.Settings
        } else {
            Timber.d("API key configured - showing satellite tracker")
            Screen.SatelliteTracking
        }
    }
    
    when (currentScreen) {
        Screen.Loading -> {
            // Show nothing while checking
        }
        Screen.Settings -> {
            SettingsScreen(
                modifier = modifier,
                onSettingsSaved = {
                    Timber.d("Settings saved, navigating to satellite tracker")
                    currentScreen = Screen.SatelliteTracking
                },
                isFirstLaunch = isFirstLaunch,
                isTrackingActive = isTrackingActive
            )
        }
        Screen.SatelliteTracking -> {
            SatelliteTrackingScreen(
                modifier = modifier,
                onNavigateToSettings = {
                    Timber.d("Navigating to settings")
                    isFirstLaunch = false // Not first launch anymore
                    currentScreen = Screen.Settings
                },
                onTrackingStateChanged = { isTracking ->
                    isTrackingActive = isTracking
                }
            )
        }
    }
}

sealed class Screen {
    object Loading : Screen()
    object Settings : Screen()
    object SatelliteTracking : Screen()
}