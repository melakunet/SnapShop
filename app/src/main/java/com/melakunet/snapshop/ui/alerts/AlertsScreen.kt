package com.melakunet.snapshop.ui.alerts

import android.Manifest
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.melakunet.snapshop.SnapShopApplication
import com.melakunet.snapshop.data.AlertRepository
import com.melakunet.snapshop.data.PriceAlert
import com.melakunet.snapshop.ui.theme.Brand
import com.melakunet.snapshop.ui.theme.Radius
import com.melakunet.snapshop.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen() {
    val context = LocalContext.current
    val dao = (context.applicationContext as SnapShopApplication).database.dao()
    val scope = rememberCoroutineScope()
    val alerts by dao.getAllPriceAlerts().collectAsState(initial = emptyList())
    
    var isChecking by remember { mutableStateOf(false) }
    val repository = remember { AlertRepository(dao) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(alerts.size) {
        if (alerts.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(Modifier.fillMaxSize().background(Brand.backgroundDark).padding(Spacing.lg)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Price Alerts", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            if (alerts.isNotEmpty()) {
                if (isChecking) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = Brand.accentDark, strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = {
                        scope.launch {
                            isChecking = true
                            repository.checkAllAlerts()
                            isChecking = false
                        }
                    }) {
                        Text("Check Now", color = Brand.accentDark)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(Spacing.lg))
        
        if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No price alerts", color = Color.White.copy(alpha = 0.4f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(alerts, key = { it.id }) { alert ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                scope.launch { dao.deletePriceAlert(alert) }
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Brand.error else Color.Transparent
                            Box(
                                Modifier.fillMaxSize().clip(RoundedCornerShape(Radius.md)).background(color).padding(horizontal = Spacing.lg),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        },
                        content = {
                            AlertRow(alert)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertRow(alert: PriceAlert) {
    Surface(
        shape = RoundedCornerShape(Radius.md),
        color = Brand.surfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (alert.triggered) Icons.Filled.NotificationsActive
                else Icons.Outlined.Notifications,
                contentDescription = null,
                tint = if (alert.triggered) Brand.success
                else Color.White.copy(alpha = 0.3f),
            )
            Spacer(Modifier.size(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(alert.productName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(
                    "Target $" + String.format("%.2f", alert.targetPrice) + 
                        "  ·  Checked " + DateUtils.getRelativeTimeSpanString(alert.lastCheckedDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
            if (alert.triggered) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Brand.success.copy(alpha = 0.15f)
                ) {
                    Text(
                        "Fired!",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Brand.success,
                    )
                }
            }
        }
    }
}
