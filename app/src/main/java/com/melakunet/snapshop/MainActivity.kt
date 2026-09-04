package com.melakunet.snapshop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.melakunet.snapshop.ui.alerts.AlertsScreen
import com.melakunet.snapshop.ui.camera.CameraScreen
import com.melakunet.snapshop.ui.history.HistoryScreen
import com.melakunet.snapshop.ui.onboarding.OnboardingScreen
import com.melakunet.snapshop.ui.saved.SavedScreen
import com.melakunet.snapshop.ui.settings.SettingsScreen
import com.melakunet.snapshop.ui.theme.SnapShopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnapShopTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("snapshop", 0) }
    var seenOnboarding by remember {
        mutableStateOf(prefs.getBoolean("hasSeenOnboarding", false))
    }

    if (!seenOnboarding) {
        OnboardingScreen(
            onFinished = {
                prefs.edit().putBoolean("hasSeenOnboarding", true).apply()
                seenOnboarding = true
            },
        )
    } else {
        SnapShopRoot()
    }
}

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Scan", Icons.Filled.CameraAlt),
    Tab("History", Icons.Filled.History),
    Tab("Saved", Icons.Filled.Favorite),
    Tab("Alerts", Icons.Filled.Notifications),
    Tab("Settings", Icons.Filled.Settings),
)

@Composable
fun SnapShopRoot() {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selected) {
                0 -> CameraScreen()
                1 -> HistoryScreen()
                2 -> SavedScreen()
                3 -> AlertsScreen()
                4 -> SettingsScreen()
            }
        }
    }
}
