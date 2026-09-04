package com.melakunet.snapshop.ui.saved

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bookmark
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
fun SavedScreen() {
    Column(Modifier.fillMaxSize().padding(Spacing.lg)) {
        Text("Saved", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Spacing.lg))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(SampleData.saved) { item ->
                val dropped = item.currentPrice < item.savedPrice
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
                            Icons.Filled.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.size(Spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(item.productName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                item.source + "  ·  saved " + item.savedDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "$" + String.format("%.2f", item.currentPrice),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (dropped) Brand.success else MaterialTheme.colorScheme.onSurface,
                            )
                            if (dropped) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.ArrowDownward,
                                        contentDescription = null,
                                        tint = Brand.success,
                                        modifier = Modifier.size(Spacing.md),
                                    )
                                    Text(
                                        "was $" + String.format("%.2f", item.savedPrice),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Brand.success,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
