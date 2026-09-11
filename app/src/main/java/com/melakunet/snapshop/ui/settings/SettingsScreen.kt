package com.melakunet.snapshop.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melakunet.snapshop.BuildConfig
import com.melakunet.snapshop.data.QuotaManager
import com.melakunet.snapshop.ui.camera.Paywall
import com.melakunet.snapshop.ui.theme.Brand
import com.melakunet.snapshop.ui.theme.Radius
import com.melakunet.snapshop.ui.theme.Spacing

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("snapshop", 0) }
    val quotaManager = remember { QuotaManager(context) }
    
    var isPro by remember { mutableStateOf(quotaManager.isPro) }
    var proClickCount by remember { mutableIntStateOf(0) }
    
    var defaultScanMode by remember { mutableStateOf(prefs.getString("defaultScanMode", "Precision") ?: "Precision") }
    var priceDropAlerts by remember { mutableStateOf(prefs.getBoolean("priceDropAlerts", true)) }
    var hapticFeedback by remember { mutableStateOf(prefs.getBoolean("hapticFeedback", true)) }
    
    var showPaywallPreview by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    val retailers = listOf(
        "Amazon" to Icons.Default.ShoppingCart,
        "Walmart" to Icons.Default.ShoppingBag,
        "Best Buy" to Icons.Default.Monitor,
        "eBay" to Icons.Default.Sell,
        "Target" to Icons.Default.AdsClick,
        "Home Depot" to Icons.Default.Handyman,
        "B&H" to Icons.Default.CameraAlt,
    )
    
    val enabledRetailers = remember {
        mutableStateMapOf<String, Boolean>().apply {
            retailers.forEach { (name, _) -> put(name, prefs.getBoolean("retailer_$name", true)) }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Brand.backgroundDark).verticalScroll(rememberScrollState()).padding(Spacing.lg),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Spacer(Modifier.height(Spacing.xl))

        // Scanning
        SettingsSection("Scanning") {
            SettingsRow(
                label = "Default Scan Mode",
                icon = Icons.Default.Camera,
                action = {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Text(
                            defaultScanMode,
                            color = Brand.accentDark,
                            modifier = Modifier.clickable { expanded = true }
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Precision") },
                                onClick = {
                                    defaultScanMode = "Precision"
                                    prefs.edit().putString("defaultScanMode", "Precision").apply()
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Deep") },
                                onClick = {
                                    defaultScanMode = "Deep"
                                    prefs.edit().putString("defaultScanMode", "Deep").apply()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            )
        }

        // Trusted Retailers
        SettingsSection("Trusted Retailers") {
            retailers.forEachIndexed { index, (name, icon) ->
                SettingsToggleRow(
                    label = name,
                    icon = icon,
                    checked = enabledRetailers[name] == true,
                    onCheckedChange = {
                        enabledRetailers[name] = it
                        prefs.edit().putBoolean("retailer_$name", it).apply()
                    },
                    showDivider = index < retailers.size - 1
                )
            }
        }

        // Notifications & Feedback
        SettingsSection("Notifications & Feedback") {
            SettingsToggleRow(
                label = "Price Drop Alerts",
                icon = Icons.Default.NotificationsActive,
                checked = priceDropAlerts,
                onCheckedChange = {
                    priceDropAlerts = it
                    prefs.edit().putBoolean("priceDropAlerts", it).apply()
                },
                showDivider = true
            )
            SettingsToggleRow(
                label = "Haptic Feedback",
                icon = Icons.Default.Vibration,
                checked = hapticFeedback,
                onCheckedChange = {
                    hapticFeedback = it
                    prefs.edit().putBoolean("hapticFeedback", it).apply()
                }
            )
        }

        // Privacy & Data
        SettingsSection("Privacy & Data") {
            SettingsRow(
                label = "Privacy Policy",
                icon = Icons.Default.PrivacyTip,
                onClick = { showPrivacyPolicy = true },
                action = { Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f)) }
            )
        }

        // Account
        SettingsSection("Account") {
            Column(Modifier.padding(Spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Column {
                        Text("Demo", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Signed in", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    "Sign Out",
                    color = Brand.error,
                    modifier = Modifier.clickable {
                        prefs.edit().putBoolean("hasSignedIn", false).apply()
                        Toast.makeText(context, "Signed out. Restart app.", Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(Modifier.height(Spacing.md))
                Divider(color = Color.White.copy(alpha = 0.05f))
                Spacer(Modifier.height(Spacing.md))
                Text(
                    "Version 1.0.0",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        proClickCount++
                        if (proClickCount >= 5) {
                            isPro = !isPro
                            quotaManager.isPro = isPro
                            proClickCount = 0
                            Toast.makeText(context, if (isPro) "Pro enabled (debug)" else "Pro disabled", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // DEBUG Section
        if (BuildConfig.DEBUG) {
            SettingsSection("DEBUG") {
                SettingsToggleRow(
                    label = "Force Pro",
                    checked = isPro,
                    onCheckedChange = {
                        isPro = it
                        quotaManager.isPro = it
                    },
                    showDivider = true
                )
                val used = quotaManager.getUsedQuota()
                val limit = quotaManager.freeLimit
                SettingsRow(
                    label = "Precision Scans Used",
                    action = { Text("$used / $limit", color = Color.White.copy(alpha = 0.5f)) },
                    showDivider = true
                )
                SettingsRow(
                    label = "Reset Quota",
                    onClick = {
                        context.getSharedPreferences("quota_prefs", 0).edit().putInt("scanCount", 0).apply()
                        Toast.makeText(context, "Quota reset", Toast.LENGTH_SHORT).show()
                    },
                    labelColor = Brand.error,
                    showDivider = true
                )
                SettingsRow(
                    label = "Preview Paywall",
                    onClick = { showPaywallPreview = true },
                    labelColor = Brand.accentDark
                )
            }
        }
        
        Spacer(Modifier.height(Spacing.xxxl))
    }

    if (showPaywallPreview) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPaywallPreview = false }) {
            Paywall(onDismiss = { showPaywallPreview = false })
        }
    }
    
    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            title = { Text("Privacy Policy") },
            text = { Text("Your scan history and saved items stay on your device. We do not sell your data. Full policy arriving with Play Store release.") },
            confirmButton = { TextButton(onClick = { showPrivacyPolicy = false }) { Text("OK") } }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.md)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = Spacing.md, bottom = Spacing.sm)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Brand.surfaceDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    labelColor: Color = Color.White,
    showDivider: Boolean = false,
    action: @Composable (() -> Unit)? = null
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.md))
            }
            Text(label, color = labelColor, modifier = Modifier.weight(1f))
            action?.invoke()
        }
        if (showDivider) {
            Divider(Modifier.padding(horizontal = Spacing.md), color = Color.White.copy(alpha = 0.05f))
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = false
) {
    SettingsRow(
        label = label,
        icon = icon,
        showDivider = showDivider,
        action = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Brand.accentDark,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    )
}
