package com.melakunet.snapshop.ui.settings

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.melakunet.snapshop.models.SampleData
import com.melakunet.snapshop.ui.theme.Radius
import com.melakunet.snapshop.ui.theme.Spacing

@Composable
fun SettingsScreen() {
    val enabled = remember {
        mutableStateMapOf<String, Boolean>().apply {
            SampleData.retailers.forEach { put(it, true) }
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
                SampleData.retailers.forEach { retailer ->
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
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        Text(
            "Snap&Shop for Android  ·  v1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
