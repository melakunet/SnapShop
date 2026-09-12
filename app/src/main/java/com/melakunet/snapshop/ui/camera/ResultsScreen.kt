package com.melakunet.snapshop.ui.camera

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.melakunet.snapshop.data.PriceAlert
import com.melakunet.snapshop.data.SavedItem
import com.melakunet.snapshop.data.SnapShopDao
import com.melakunet.snapshop.models.IdentifyResult
import com.melakunet.snapshop.models.ProductReviews
import com.melakunet.snapshop.models.ShopItem
import com.melakunet.snapshop.network.BackendClient
import com.melakunet.snapshop.ui.theme.Brand
import com.melakunet.snapshop.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun ResultPanel(
    product: IdentifyResult,
    prices: List<ShopItem>,
    thumbnail: ByteArray?,
    modeLabel: String,
    dao: SnapShopDao,
    onDismiss: () -> Unit
) {
    var selectedItem by remember { mutableStateOf<ShopItem?>(null) }
    var sortByPrice by remember { mutableStateOf(true) }

    val sortedPrices = remember(prices, sortByPrice) {
        if (sortByPrice) {
            prices.sortedBy { it.extractedPrice }
        } else {
            prices.sortedByDescending { item ->
                val rating = item.rating ?: 0.0
                val count = item.reviewCount ?: 0
                (rating * count + 3.5 * 20) / (count + 20)
            }.let { list ->
                val (rated, unrated) = list.partition { it.rating != null }
                rated + unrated
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = Brand.backgroundDark) {
            Column(Modifier.fillMaxSize()) {
                ResultsHeader(onDismiss)
                
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    item {
                        ScanHeaderCard(product, thumbnail, modeLabel, dao)
                    }

                    if (product.confidence < 0.7 && modeLabel.contains("Precision", true)) {
                        item {
                            ConfidenceEscalationBanner(onSwitchToDeep = onDismiss)
                        }
                    }
                    
                    item {
                        SortToggle(sortByPrice) { sortByPrice = it }
                    }
                    
                    if (prices.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(top = Spacing.xxl), contentAlignment = Alignment.Center) {
                                Text("No prices to show.", color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    } else {
                        val minPrice = prices.minOf { it.extractedPrice }
                        items(sortedPrices) { item ->
                            val isCheapest = item.extractedPrice <= minPrice
                            RetailerCard(item, isCheapest) { selectedItem = item }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedItem != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedItem?.let { item ->
                ItemDetailScreen(
                    product = product,
                    item = item,
                    isCheapest = item.extractedPrice <= (prices.minOfOrNull { it.extractedPrice } ?: 0.0),
                    dao = dao,
                    onBack = { selectedItem = null }
                )
            }
        }
    }
}

@Composable
private fun ConfidenceEscalationBanner(onSwitchToDeep: () -> Unit) {
    Surface(
        color = Brand.accentDark.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Brand.accentDark.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Help, null, tint = Brand.accentDark)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Not sure?", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text("Precision confidence is low. Try a Deep Scan for better results.", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            }
            TextButton(onClick = onSwitchToDeep) {
                Text("Deep Scan", color = Brand.accentDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResultsHeader(onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onDismiss, Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)) {
            Icon(Icons.Default.ChevronLeft, "Back", tint = Color.White)
        }
        Text("Results", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun ScanHeaderCard(product: IdentifyResult, thumbnail: ByteArray?, modeLabel: String, dao: SnapShopDao) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val name = listOf(product.brand, product.model).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { product.category }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Brand.surfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).background(Brand.backgroundDark),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    val bitmap = remember(thumbnail) { BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.size) }
                    bitmap?.let {
                        Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                } else {
                    Icon(Icons.Default.Image, null, tint = Color.White.copy(alpha = 0.2f))
                }
            }
            
            Spacer(Modifier.width(Spacing.md))
            
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif), color = Color.White)
                Text(product.category, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.height(Spacing.xs))
                Surface(
                    color = Brand.accentDark.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Brand.accentDark.copy(alpha = 0.3f))
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, Modifier.size(10.dp), tint = Brand.accentDark)
                        Spacer(Modifier.width(4.dp))
                        Text(modeLabel, style = MaterialTheme.typography.labelSmall, color = Brand.accentDark, fontSize = 10.sp)
                    }
                }
            }
            
            IconButton(onClick = {
                scope.launch {
                    val prefs = context.getSharedPreferences("snapshop", 0)
                    if (prefs.getBoolean("hapticFeedback", true)) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    dao.insertSavedItem(SavedItem(
                        productName = name,
                        searchQuery = product.searchQuery,
                        thumbnail = null,
                        savedPrice = 0.0,
                        savedDate = System.currentTimeMillis(),
                        link = "",
                        source = "",
                        currentLowestPrice = 0.0
                    ))
                    Toast.makeText(context, "Saved to history", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(Icons.Default.BookmarkBorder, null, tint = Brand.accentDark)
            }
        }
    }
}

@Composable
private fun SortToggle(sortByPrice: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(50)).padding(4.dp)
    ) {
        SortTab("Best price", sortByPrice, Modifier.weight(1f)) { onToggle(true) }
        SortTab("Best reviewed", !sortByPrice, Modifier.weight(1f)) { onToggle(false) }
    }
}

@Composable
private fun SortTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = if (selected) Color.White.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun RetailerCard(item: ShopItem, isCheapest: Boolean, onClick: () -> Unit) {
    val majorRetailers = listOf("Amazon", "Walmart", "Best Buy", "Target", "Home Depot", "B&H", "Newegg")
    val isMajor = majorRetailers.any { item.source.contains(it, ignoreCase = true) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Brand.surfaceDark,
        border = if (isCheapest) BorderStroke(1.dp, Brand.success.copy(alpha = 0.5f)) else null,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.thumbnail,
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)),
                contentScale = ContentScale.Fit
            )
            
            Spacer(Modifier.width(Spacing.md))
            
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title ?: item.source,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isCheapest) {
                        BadgeChip("✓ Best Price", Brand.success)
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.source, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                    Spacer(Modifier.width(8.dp))
                    BadgeChip(if (isMajor) "Major retailer" else "Marketplace", if (isMajor) Brand.success else Color.Gray)
                }
                
                if (item.rating != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, Modifier.size(12.dp), tint = Brand.warning)
                        Spacer(Modifier.width(4.dp))
                        Text("${item.rating} (${item.reviewCount ?: 0})", style = MaterialTheme.typography.labelSmall, color = Brand.textSecondaryDark)
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(item.price, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = if (isCheapest) Brand.success else Color.White)
                if (item.delivery.contains("$", ignoreCase = true) || item.delivery.contains("shipping", ignoreCase = true)) {
                    Text("+ shipping", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
private fun BadgeChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun ItemDetailScreen(
    product: IdentifyResult,
    item: ShopItem,
    isCheapest: Boolean,
    dao: SnapShopDao,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var reviews by remember { mutableStateOf<ProductReviews?>(null) }
    var loadingReviews by remember { mutableStateOf(false) }

    LaunchedEffect(item.productId) {
        if (!item.productId.isNullOrEmpty()) {
            loadingReviews = true
            runCatching { BackendClient.productReviews(item.productId!!) }
                .onSuccess { reviews = it }
            loadingReviews = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Brand.backgroundDark) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().padding(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Text(item.source, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                
                IconButton(onClick = {
                    scope.launch {
                        val prefs = context.getSharedPreferences("snapshop", 0)
                        if (prefs.getBoolean("hapticFeedback", true)) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        dao.insertSavedItem(SavedItem(
                            productName = item.title ?: product.brand,
                            searchQuery = product.searchQuery,
                            thumbnail = item.thumbnail,
                            savedPrice = item.extractedPrice,
                            savedDate = System.currentTimeMillis(),
                            link = item.link,
                            source = item.source,
                            currentLowestPrice = item.extractedPrice
                        ))
                        Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.BookmarkBorder, null, tint = Brand.accentDark)
                }
            }
            
            Column(Modifier.padding(horizontal = Spacing.lg)) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)),
                    contentScale = ContentScale.Fit
                )
                
                Spacer(Modifier.height(Spacing.lg))
                
                Text(item.title ?: product.brand, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif), color = Color.White)
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = Spacing.sm)) {
                    BadgeChip(item.source, Brand.accentDark)
                    if (isCheapest) BadgeChip("✓ Best Price", Brand.success)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    if (item.rating != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Brand.warning, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${item.rating} (${item.reviewCount} reviews)", color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                    Text(item.price, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), color = Brand.success)
                }
                
                Spacer(Modifier.height(Spacing.xl))
                
                if (loadingReviews) {
                    CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = Brand.accentDark)
                } else if (reviews != null) {
                    RatingBreakdownCard(reviews!!)
                }

                product.plant?.let { plant ->
                    if (plant.warning != null) {
                        Spacer(Modifier.height(Spacing.lg))
                        PoisonControlRow(plant.warning.level)
                    }
                }
                
                Spacer(Modifier.height(Spacing.xxl))
                
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Button(
                        onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.link))) } },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand.accentDark, contentColor = Brand.accentOn),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open in store", fontWeight = FontWeight.Bold)
                    }

                    var showPriceAlert by remember { mutableStateOf(false) }
                    val prefs = remember { context.getSharedPreferences("snapshop", 0) }

                    OutlinedButton(
                        onClick = { showPriceAlert = true },
                        modifier = Modifier.height(56.dp),
                        border = BorderStroke(1.dp, Brand.accentDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.NotificationsNone, null, tint = Brand.accentDark)
                    }

                    if (showPriceAlert) {
                        PriceAlertDialog(
                            productName = item.title ?: product.brand,
                            currentPrice = item.extractedPrice,
                            onDismiss = { showPriceAlert = false },
                            onConfirm = { targetPrice ->
                                scope.launch {
                                    if (prefs.getBoolean("hapticFeedback", true)) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                    dao.insertPriceAlert(PriceAlert(
                                        savedItemId = item.productId ?: "", // Best effort link
                                        productName = item.title ?: product.brand,
                                        searchQuery = product.searchQuery,
                                        targetPrice = targetPrice,
                                        createdDate = System.currentTimeMillis(),
                                        lastCheckedDate = System.currentTimeMillis(),
                                        triggered = item.extractedPrice <= targetPrice
                                    ))
                                    showPriceAlert = false
                                    Toast.makeText(context, "Alert set!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
                
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

@Composable
private fun PoisonControlRow(level: String) {
    val context = LocalContext.current
    val color = when (level.lowercase()) {
        "danger", "critical" -> Brand.error
        else -> Brand.warning
    }

    Surface(
        onClick = {
            val phone = if (context.resources.configuration.locales[0].country == "CA") "tel:18447647669" else "tel:18002221222"
            runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(phone))) }
        },
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Phone, null, tint = color)
            Spacer(Modifier.width(Spacing.md))
            Text("Call Poison Control", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun RatingBreakdownCard(reviews: ProductReviews) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Brand.surfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reviews.rating.toString(), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Spacer(Modifier.width(Spacing.md))
                Column {
                    Row {
                        repeat(5) { i ->
                            Icon(
                                Icons.Default.Star, 
                                null, 
                                modifier = Modifier.size(16.dp),
                                tint = if (i < reviews.rating.toInt()) Brand.warning else Color.White.copy(alpha = 0.2f)
                            )
                        }
                    }
                    Text("${reviews.reviewCount} reviews", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                }
            }
            
            Spacer(Modifier.height(Spacing.lg))
            
            reviews.breakdown?.let { breakdown ->
                breakdown.counts.forEach { (stars, count) ->
                    val progress = if (reviews.reviewCount > 0) count.toFloat() / reviews.reviewCount else 0f
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("${stars}★", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.width(24.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape),
                            color = Brand.warning,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceAlertDialog(productName: String, currentPrice: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var text by remember { mutableStateOf(String.format("%.2f", currentPrice * 0.9)) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Brand.surfaceDark, modifier = Modifier.padding(Spacing.lg).fillMaxWidth()) {
            Column(Modifier.padding(Spacing.lg)) {
                Text("Set Price Alert", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(Spacing.sm))
                Text("Notify me when $productName drops below:", color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(Spacing.md))
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    prefix = { Text("$") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                )
                Spacer(Modifier.height(Spacing.lg))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = Color.White, modifier = Modifier.clickable { onDismiss() })
                    Spacer(Modifier.width(Spacing.xl))
                    Text("Set Alert", color = Brand.accentDark, modifier = Modifier.clickable {
                        text.toDoubleOrNull()?.let { onConfirm(it) }
                    })
                }
            }
        }
    }
}
