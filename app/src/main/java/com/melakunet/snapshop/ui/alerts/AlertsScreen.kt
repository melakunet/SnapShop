package com.melakunet.snapshop.ui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.melakunet.snapshop.models.SampleData
import com.melakunet.snapshop.ui.theme.Brand
import com.melakunet.snapshop.ui.theme.Radius
import com.melakunet.snapshop.ui.theme.Spacing

@Composable
fun AlertsScreen() {
    Column(Modifier.fillMaxSize().padding(Spacing.lg)) {
        Text("Price Alerts", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Spacing.lg))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(SampleData.alerts) { alert ->
                Surface(
                    shape = RoundedCornerShape(Radius.md),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
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
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.size(Spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(alert.productName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Target $" + String.format("%.2f", alert.targetPrice) +
                                    "  ·  now $" + String.format("%.2f", alert.currentPrice),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (alert.triggered) {
                            Surface(shape = RoundedCornerShape(50), color = Brand.success) {
                                Text(
                                    "Below target",
                                    modifier = Modifier.padding(
                                        horizontal = Spacing.sm,
                                        vertical = androidx.compose.ui.unit.Dp(2f),
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = androidx.compose.ui.graphics.Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

