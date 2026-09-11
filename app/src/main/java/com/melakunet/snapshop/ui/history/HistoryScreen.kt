package com.melakunet.snapshop.ui.history

import android.graphics.BitmapFactory
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.melakunet.snapshop.SnapShopApplication
import com.melakunet.snapshop.data.ScanRecord
import com.melakunet.snapshop.ui.theme.Brand
import com.melakunet.snapshop.ui.theme.Radius
import com.melakunet.snapshop.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val dao = (context.applicationContext as SnapShopApplication).database.dao()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    
    val allScans by dao.getAllScanRecords().collectAsState(initial = emptyList())
    val scans = allScans.filter {
        query.isBlank() || it.productName.contains(query, ignoreCase = true)
    }
    
    var showClearDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Brand.backgroundDark).padding(Spacing.lg)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            if (allScans.isNotEmpty()) {
                TextButton(onClick = { showClearDialog = true }) {
                    Text("Clear", color = Brand.error)
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search scans") },
            singleLine = true,
            shape = RoundedCornerShape(Radius.md),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedBorderColor = Brand.accentDark,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White
            )
        )
        Spacer(Modifier.height(Spacing.lg))
        if (scans.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No scans found",
                    color = Color.White.copy(alpha = 0.4f),
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(scans, key = { it.id }) { scan ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                scope.launch { dao.deleteScanRecord(scan) }
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
                            ScanRow(scan)
                        }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to delete all scan records? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        dao.clearAllScanRecords()
                        showClearDialog = false
                    }
                }) {
                    Text("Clear All", color = Brand.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ScanRow(scan: ScanRecord) {
    Surface(
        shape = RoundedCornerShape(Radius.md),
        color = Brand.surfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center,
            ) {
                if (scan.thumbnail != null) {
                    val bitmap = remember(scan.thumbnail) {
                        BitmapFactory.decodeByteArray(scan.thumbnail, 0, scan.thumbnail.size)
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                    )
                }
            }
            Spacer(Modifier.size(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(scan.productName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModeChip(scan.mode)
                    Spacer(Modifier.size(Spacing.sm))
                    Text(
                        DateUtils.getRelativeTimeSpanString(scan.date).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                }
            }
            Text(
                "$" + String.format("%.2f", scan.lowestPrice),
                style = MaterialTheme.typography.titleMedium,
                color = Brand.accentDark,
            )
        }
    }
}

@Composable
private fun ModeChip(mode: String) {
    val tint = if (mode == "Deep") Brand.scanDeep else Brand.accentDark
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
