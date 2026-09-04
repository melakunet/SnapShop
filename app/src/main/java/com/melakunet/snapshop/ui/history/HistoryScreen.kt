package com.melakunet.snapshop.ui.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.melakunet.snapshop.models.SampleData
import com.melakunet.snapshop.ui.theme.Brand
import com.melakunet.snapshop.ui.theme.Radius
import com.melakunet.snapshop.ui.theme.Spacing

@Composable
fun HistoryScreen() {
    var query by remember { mutableStateOf("") }
    val scans = SampleData.scans.filter {
        query.isBlank() || it.productName.contains(query, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize().padding(Spacing.lg)) {
        Text("History", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Spacing.md))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search scans") },
            singleLine = true,
            shape = RoundedCornerShape(Radius.md),
        )
        Spacer(Modifier.height(Spacing.lg))
        if (scans.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No scans found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(scans) { scan ->
                    Surface(
                        shape = RoundedCornerShape(Radius.md),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(Radius.sm))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.PhotoCamera,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.size(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(scan.productName, style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ModeChip(scan.mode)
                                    Spacer(Modifier.size(Spacing.sm))
                                    Text(
                                        scan.date,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                "$" + String.format("%.2f", scan.lowestPrice),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(mode: String) {
    val tint = if (mode == "Deep") Brand.scanDeep else MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.15f),
    ) {
        Text(
            mode,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}
