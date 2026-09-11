package com.melakunet.snapshop.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.melakunet.snapshop.data.QuotaManager
import com.melakunet.snapshop.ui.theme.Brand
import com.melakunet.snapshop.ui.theme.Radius
import com.melakunet.snapshop.ui.theme.Spacing

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val quotaManager = remember { QuotaManager(context) }
    var isPro by remember { mutableStateOf(quotaManager.isPro) }
    var proClickCount by remember { mutableIntStateOf(0) }

    val retailers = listOf(
        "Amazon.com", "Walmart", "Best Buy", "Target", "eBay", "Home Depot", "B&H",
    )
    val enabled = remember {
        mutableStateMapOf<String, Boolean>().apply {
            retailers.forEach { put(it, true) }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Spacing.xl))

        Text("Retailers", style = MaterialTheme.typography.titleMedium)
        Text(
            "Choose which stores appear in price results",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        Surface(
            shape = RoundedCornerShape(Radius.md),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(horizontal = Spacing.md)) {
                retailers.forEach { retailer ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(retailer, modifier = Modifier.weight(1f))
                        Switch(
                            checked = enabled[retailer] == true,
                            onCheckedChange = { enabled[retailer] = it },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        Text("Usage", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.md))
        Surface(
            shape = RoundedCornerShape(Radius.md),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(Spacing.md)) {
                if (isPro) {
                    Text("Snap&Shop Pro Active", color = Brand.success, style = MaterialTheme.typography.bodyMedium)
                    Text("Unlimited scans and all features unlocked.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val used = quotaManager.getUsedQuota()
                    val limit = quotaManager.freeLimit
                    Text("$used of $limit free scans used this month", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = used.toFloat() / limit,
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                        color = if (used >= limit) Brand.error else Brand.accentDark,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        Text("Account", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.md))
        Surface(
            shape = RoundedCornerShape(Radius.md),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(Spacing.md)) {
                Text("Not signed in", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Google Sign-In arrives with the auth milestone",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                Button(
                    onClick = {
                        context.getSharedPreferences("snapshop", 0).edit().putBoolean("hasSignedIn", false).apply()
                        Toast.makeText(context, "Signed out. Restart app to see Sign In screen.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand.error)
                ) {
                    Text("Sign out", color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        Text(
            "Snap&Shop for Android  ·  v1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().clickable {
                proClickCount++
                if (proClickCount >= 5) {
                    isPro = !isPro
                    quotaManager.isPro = isPro
                    proClickCount = 0
                    Toast.makeText(context, if (isPro) "Pro enabled (debug)" else "Pro disabled", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}

@Composable
fun LinearProgressIndicator(progress: Float, modifier: Modifier, color: androidx.compose.ui.graphics.Color, trackColor: androidx.compose.ui.graphics.Color) {
    androidx.compose.material3.LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = color,
        trackColor = trackColor,
    )
}
