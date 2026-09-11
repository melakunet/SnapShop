package com.melakunet.snapshop.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.Paint
import android.graphics.Path
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.melakunet.snapshop.SnapShopApplication
import com.melakunet.snapshop.data.ScanRecord
import com.melakunet.snapshop.data.QuotaManager
import com.melakunet.snapshop.data.PriceAlert
import com.melakunet.snapshop.data.SavedItem
import com.melakunet.snapshop.models.IdentifyResult
import com.melakunet.snapshop.models.ShopItem
import com.melakunet.snapshop.network.BackendClient
import com.melakunet.snapshop.ui.theme.Brand
import com.melakunet.snapshop.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float)
data class CropImage(val uri: Uri, val bitmap: Bitmap, val displayRect: androidx.compose.ui.geometry.Rect = androidx.compose.ui.geometry.Rect.Zero)
data class CropSelection(val image: CropImage, val crop: CropRect)

private enum class ScanMode { PRECISION, DEEP }

private sealed interface ScanState {
    data object Idle : ScanState
    data object Recording : ScanState
    data class Cropping(val image: CropImage) : ScanState
    data class Sending(val label: String) : ScanState
    data class Done(val product: IdentifyResult, val prices: List<ShopItem>) : ScanState
    data class Error(val message: String) : ScanState
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val database = (context.applicationContext as SnapShopApplication).database
    val dao = database.dao()
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    if (!hasPermission) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Snap&Shop needs the camera to identify products", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.lg))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val quotaManager = remember { QuotaManager(context) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ScanState>(ScanState.Idle) }
    var mode by remember { mutableStateOf(ScanMode.PRECISION) }
    var showUrlSearch by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var captureInFlight by remember { mutableStateOf(false) }

    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val videoCapture = remember {
        VideoCapture.withOutput(
            Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build(),
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val cropImage = withContext(Dispatchers.IO) { context.loadCropImage(uri) }
            state = ScanState.Cropping(cropImage)
        }
    }
        val videoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                if (!quotaManager.canScan()) {
                    showPaywall = true
                } else {
                    runScan(scope, { state = it }, "Analyzing video...", quotaManager) {
                        val file = context.copyUriToCache(uri, "picked_video") ?: throw IllegalStateException("Could not open video")
                        val frames = extractKeyframes(file)
                        val product = BackendClient.identifyDeep(frames)
                        val prices = product.searchQuery.trim().takeIf { it.isNotEmpty() }?.let { BackendClient.shop(it) } ?: emptyList()
                        product to prices
                    }
                }
            }
        }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            runCatching { recording?.stop() }
        }
    }

    Box(Modifier.fillMaxSize().background(Brand.backgroundDark)) {
        if (state is ScanState.Cropping) {
            val croppingImage = (state as ScanState.Cropping).image
            AdjustCropScreen(
                image = croppingImage,
                onCancel = { state = ScanState.Idle },
                onScan = { selection ->
                    state = ScanState.Idle
                    if (!quotaManager.canScan()) {
                        showPaywall = true
                    } else {
                        scope.launch {
                            val cropped = withContext(Dispatchers.IO) {
                                selection.image.bitmap.cropImage(selection.crop)
                            }
                            val jpeg = toCappedJpeg(cropped, 1280, 80)
                            runScan(scope, { state = it }, "Identifying...", quotaManager) {
                                val result = BackendClient.scan(jpeg)
                                val product = result.first
                                val prices = result.second
                                val name = listOf(product.brand, product.model).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { product.category }
                                dao.insertScanRecord(ScanRecord(
                                    date = System.currentTimeMillis(),
                                    productName = name,
                                    mode = "Crop",
                                    thumbnail = toCappedJpeg(jpeg, 200, 70),
                                    lowestPrice = prices.minOfOrNull { it.extractedPrice } ?: 0.0,
                                    searchQuery = product.searchQuery
                                ))
                                result
                            }
                        }
                    }
                },
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        val provider = future.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, videoCapture)
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val insetX = size.width * 0.12f
                val insetTop = size.height * 0.18f
                val insetBottom = size.height * 0.24f
                val left = insetX
                val top = insetTop
                val right = size.width - insetX
                val bottom = size.height - insetBottom
                drawCurvedCorners(androidx.compose.ui.geometry.Rect(left, top, right, bottom), if (mode == ScanMode.PRECISION) Brand.accentDark else Brand.scanDeep)
                val center = Offset(size.width / 2f, (top + bottom) / 2f)
                drawCircle(Color.White.copy(alpha = 0.18f), 18.dp.toPx(), center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
                drawLine(Color.White.copy(alpha = 0.28f), Offset(center.x - 14.dp.toPx(), center.y), Offset(center.x + 14.dp.toPx(), center.y), 1.5.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.28f), Offset(center.x, center.y - 14.dp.toPx()), Offset(center.x, center.y + 14.dp.toPx()), 1.5.dp.toPx(), StrokeCap.Round)
            }

            Column(
                Modifier.fillMaxSize().padding(top = 12.dp, start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
            ) {
                Spacer(Modifier.height(Spacing.sm))
                SegmentedControl(
                    mode = mode,
                    enabled = state is ScanState.Idle,
                    onPrecision = { mode = ScanMode.PRECISION },
                    onDeep = {
                        if (quotaManager.isPro) {
                            mode = ScanMode.DEEP
                        } else {
                            showPaywall = true
                        }
                    }
                )
                Spacer(Modifier.height(Spacing.md))
                SearchField(
                    onSubmit = { query ->
                        runScan(scope, { state = it }, "Searching...") {
                            IdentifyResult("", "", query, confidence = 1.0, searchQuery = query) to BackendClient.shop(query)
                        }
                    },
                    onMic = { Toast.makeText(context, "Voice search coming soon", Toast.LENGTH_SHORT).show() },
                )
                Spacer(Modifier.height(Spacing.sm))
                PasteLinkChip(onClick = { showUrlSearch = true })
            }

            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptionPill(text = if (mode == ScanMode.PRECISION) "Hold steady — one precise shot" else "Pan slowly around the product")
                Spacer(Modifier.height(Spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    importButton(Icons.Filled.PhotoLibrary) { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    CaptureButton(mode = mode, isRecording = state is ScanState.Recording) {
                        if (!quotaManager.canScan()) {
                            showPaywall = true
                        } else {
                            when {
                                state is ScanState.Sending -> Unit
                                state is ScanState.Recording -> recording?.stop()
                                mode == ScanMode.PRECISION -> capturePhoto(imageCapture, ContextCompat.getMainExecutor(context)) { jpeg: ByteArray?, err: String? ->
                                    if (jpeg == null) state = ScanState.Error(err ?: "Capture failed") else runScan(scope, { state = it }, "Identifying...", quotaManager) {
                                        val result = BackendClient.scan(jpeg)
                                        val product = result.first
                                        val prices = result.second
                                        val name = listOf(product.brand, product.model).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { product.category }
                                        dao.insertScanRecord(ScanRecord(
                                            date = System.currentTimeMillis(),
                                            productName = name,
                                            mode = "Precision",
                                            thumbnail = toCappedJpeg(jpeg, 200, 70),
                                            lowestPrice = prices.minOfOrNull { it.extractedPrice } ?: 0.0,
                                            searchQuery = product.searchQuery
                                        ))
                                        result
                                    }
                                }
                                else -> {
                                    val file = File(context.cacheDir, "deep_scan.mp4")
                                    state = ScanState.Recording
                                    val rec = videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file).build()).start(ContextCompat.getMainExecutor(context)) { event ->
                                        if (event is VideoRecordEvent.Finalize) {
                                            recording = null
                                            if (event.hasError()) state = ScanState.Error("Recording failed (" + event.error + ")")
                                            else runScan(scope, { state = it }, "Analyzing video...", quotaManager) {
                                                val frames = extractKeyframes(file)
                                                val product = BackendClient.identifyDeep(frames)
                                                val prices = product.searchQuery.trim().takeIf { it.isNotEmpty() }?.let { BackendClient.shop(it) } ?: emptyList()
                                                val name = listOf(product.brand, product.model).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { product.category }
                                                dao.insertScanRecord(ScanRecord(
                                                    date = System.currentTimeMillis(),
                                                    productName = name,
                                                    mode = "Deep",
                                                    thumbnail = frames.firstOrNull()?.let { toCappedJpeg(it, 200, 70) },
                                                    lowestPrice = prices.minOfOrNull { it.extractedPrice } ?: 0.0,
                                                    searchQuery = product.searchQuery
                                                ))
                                                product to prices
                                            }
                                        }
                                    }
                                    recording = rec
                                    scope.launch { delay(10_000); recording?.stop() }
                                }
                            }
                        }
                    }
                    importButton(Icons.Filled.Videocam) {
                        videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }
                }
            }
        }

        when (val s = state) {
            is ScanState.Sending -> FullScreenLoading(s.label)
            is ScanState.Done -> ResultPanel(s.product, s.prices, dao) { state = ScanState.Idle }
            is ScanState.Error -> ErrorPanel(s.message) { state = ScanState.Idle }
            else -> Unit
        }

        if (showPaywall) {
            Paywall(onDismiss = { showPaywall = false })
        }

        showUrlSearch?.let { } // Removed legacy block
        // Rest of overlays handled below
    }
}

@Composable
private fun SegmentedControl(mode: ScanMode, enabled: Boolean, onPrecision: () -> Unit, onDeep: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(50)).padding(4.dp),
    ) {
        Segment("Precision", Icons.Filled.CameraAlt, mode == ScanMode.PRECISION, Brand.accentDark, enabled, onPrecision, Modifier.weight(1f))
        Segment("Deep", Icons.Filled.Videocam, mode == ScanMode.DEEP, Brand.scanDeep, enabled, onDeep, Modifier.weight(1f))
    }
}

@Composable
private fun Segment(label: String, icon: ImageVector, selected: Boolean, tint: Color, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(50),
        color = if (selected) tint else Color.Transparent,
    ) {
        Row(Modifier.padding(vertical = Spacing.sm), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) Brand.accentOn else Color.White)
            Spacer(Modifier.width(Spacing.sm))
            Text(label, color = if (selected) Brand.accentOn else Color.White)
        }
    }
}

@Composable
private fun SearchField(onSubmit: (String) -> Unit, onMic: () -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50)).padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = Color.White.copy(alpha = 0.82f))
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = if (text.isEmpty()) "Search by product name…" else text,
            color = if (text.isEmpty()) Color.White.copy(alpha = 0.6f) else Color.White,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.KeyboardVoice, null, tint = Color.White, modifier = Modifier.clickable { onMic() })
    }
    // Text entry kept minimal to preserve the iOS-style shell; submit uses the existing search flow.
    androidx.compose.foundation.text.BasicTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(0.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { if (text.isNotBlank()) onSubmit(text.trim()) }),
    )
}

@Composable
private fun PasteLinkChip(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.38f)) {
        Row(Modifier.padding(horizontal = Spacing.md, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Link, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Paste link", color = Color.White)
        }
    }
}

@Composable
private fun CaptionPill(text: String) {
    Surface(color = Color.Black.copy(alpha = 0.42f), shape = RoundedCornerShape(50)) {
        Text(text, modifier = Modifier.padding(horizontal = Spacing.md, vertical = 6.dp), color = Color.White)
    }
}

@Composable
private fun importButton(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(54.dp).background(Color.Black.copy(alpha = 0.38f), CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White)
    }
}

@Composable
private fun CaptureButton(mode: ScanMode, isRecording: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(82.dp).background(Color.White.copy(alpha = 0.16f), CircleShape).padding(6.dp).background(if (isRecording) Brand.error else if (mode == ScanMode.DEEP) Brand.scanDeep else Brand.accentDark, CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.CropSquare, null, tint = Brand.accentOn)
    }
}

@Composable
private fun FullScreenLoading(label: String) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.56f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Brand.accentDark)
            Spacer(Modifier.height(Spacing.md))
            Text(label, color = Color.White)
        }
    }
}

@Composable
private fun ErrorPanel(message: String, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.52f)), contentAlignment = Alignment.BottomCenter) {
        Surface(shape = RoundedCornerShape(16.dp), color = Brand.backgroundDark, modifier = Modifier.padding(Spacing.lg).fillMaxWidth()) {
            Column(Modifier.padding(Spacing.lg)) {
                Text("Scan failed", color = Brand.error)
                Spacer(Modifier.height(Spacing.sm))
                Text(message, color = Color.White)
                Spacer(Modifier.height(Spacing.md))
                Button(onClick = onDismiss) { Text("Try again") }
            }
        }
    }
}

@Composable
private fun SearchOverlay(title: String, placeholder: String, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))) {
        Surface(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(Spacing.lg),
            shape = RoundedCornerShape(24.dp),
            color = Brand.surfaceDark,
        ) {
            Column(Modifier.padding(Spacing.lg)) {
                Text(title, color = Color.White)
                Spacer(Modifier.height(Spacing.md))
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = Brand.error) } }
                )
                Spacer(Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text(modifier = Modifier.clickable { onDismiss() }, text = "Cancel", color = Color.White)
                    Spacer(Modifier.width(Spacing.lg))
                    Text(modifier = Modifier.clickable {
                        if (text.isNotBlank()) {
                            if (text.startsWith("http://") || text.startsWith("https://")) {
                                onSubmit(text.trim())
                            } else {
                                error = "That doesn't look like a link — use the search bar for product names."
                            }
                        }
                    }, text = "Search", color = Brand.accentDark)
                }
            }
        }
    }
}

@Composable
private fun ResultPanel(product: IdentifyResult, prices: List<ShopItem>, dao: com.melakunet.snapshop.data.SnapShopDao, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAlertDialog by remember { mutableStateOf<ShopItem?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = Brand.backgroundDark) {
        Column(Modifier.padding(Spacing.lg)) {
            val name = listOf(product.brand, product.model).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { product.category }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.weight(1f))
                Text("Close", color = Brand.accentDark, modifier = Modifier.clickable { onDismiss() })
            }
            Spacer(Modifier.height(Spacing.lg))
            if (prices.isEmpty()) {
                Text("No prices to show.", color = Color.White)
                Spacer(Modifier.weight(1f))
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(prices) { item ->
                        Surface(shape = RoundedCornerShape(12.dp), color = Brand.surfaceDark, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(Spacing.md)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.link))) }
                                }) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.source, color = Color.White)
                                        Text(item.delivery, color = Color.White.copy(alpha = 0.7f))
                                    }
                                    Text(item.price, color = Brand.accentDark)
                                }
                                Spacer(Modifier.height(Spacing.sm))
                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                dao.insertSavedItem(SavedItem(
                                                    productName = name,
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
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Save")
                                    }
                                    Button(
                                        onClick = { showAlertDialog = item },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Set alert")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Scan again") }
        }
    }

    showAlertDialog?.let { item ->
        val name = listOf(product.brand, product.model).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { product.category }
        PriceAlertDialog(
            productName = name,
            currentPrice = item.extractedPrice,
            onDismiss = { showAlertDialog = null },
            onConfirm = { targetPrice ->
                scope.launch {
                    dao.insertPriceAlert(PriceAlert(
                        savedItemId = "", // Optional: link to a SavedItem if needed
                        productName = name,
                        searchQuery = product.searchQuery,
                        targetPrice = targetPrice,
                        createdDate = System.currentTimeMillis(),
                        lastCheckedDate = System.currentTimeMillis(),
                        triggered = item.extractedPrice <= targetPrice
                    ))
                    showAlertDialog = null
                    Toast.makeText(context, "Alert set!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun PriceAlertDialog(productName: String, currentPrice: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var text by remember { mutableStateOf(String.format("%.2f", currentPrice * 0.9)) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
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

@Composable
private fun AdjustCropScreen(image: CropImage, onCancel: () -> Unit, onScan: (CropSelection) -> Unit) {
    var crop by remember(image) {
        mutableStateOf(CropRect(0.15f, 0.2f, 0.85f, 0.8f))
    }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val displayRect = remember(image, boxSize) {
        if (boxSize.width == 0 || boxSize.height == 0) androidx.compose.ui.geometry.Rect.Zero
        else fitRect(image.bitmap.width, image.bitmap.height, boxSize.width.toFloat(), boxSize.height.toFloat())
    }

    Box(Modifier.fillMaxSize().background(Brand.backgroundDark).onGloballyPositioned { boxSize = it.size }) {
        if (boxSize != IntSize.Zero) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(image.bitmap).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            if (displayRect != androidx.compose.ui.geometry.Rect.Zero) {
                CropOverlay(
                    crop = crop,
                    displayRect = displayRect,
                    onCropChange = { crop = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        
        // Header
        Box(Modifier.fillMaxWidth().padding(Spacing.lg)) {
            Text("Cancel", color = Color.White, modifier = Modifier.align(Alignment.CenterStart).clickable { onCancel() })
            Text("Adjust Crop", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Center))
            Text("Scan", color = Brand.accentDark, style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), modifier = Modifier.align(Alignment.CenterEnd).clickable { onScan(CropSelection(image, crop)) })
        }
    }
}

private enum class DragMode { NONE, CORNER_TL, CORNER_TR, CORNER_BL, CORNER_BR, EDGE_T, EDGE_B, EDGE_L, EDGE_R, INTERIOR }

@Composable
private fun CropOverlay(crop: CropRect, displayRect: androidx.compose.ui.geometry.Rect, onCropChange: (CropRect) -> Unit, modifier: Modifier = Modifier) {
    val currentCrop by rememberUpdatedState(crop)
    val currentDisplayRect by rememberUpdatedState(displayRect)
    val currentOnCropChange by rememberUpdatedState(onCropChange)
    
    var dragMode by remember { mutableStateOf(DragMode.NONE) }
    val handleSize = 18.dp
    val touchSlop = 48.dp

    Box(modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { offset ->
                val rect = currentCrop.toScreenRect(currentDisplayRect)
                val tl = Offset(rect.left, rect.top)
                val tr = Offset(rect.right, rect.top)
                val bl = Offset(rect.left, rect.bottom)
                val br = Offset(rect.right, rect.bottom)
                val slopPx = touchSlop.toPx()
                
                dragMode = when {
                    offset.distanceTo(tl) <= slopPx -> DragMode.CORNER_TL
                    offset.distanceTo(tr) <= slopPx -> DragMode.CORNER_TR
                    offset.distanceTo(bl) <= slopPx -> DragMode.CORNER_BL
                    offset.distanceTo(br) <= slopPx -> DragMode.CORNER_BR
                    offset.y in (rect.top - slopPx)..(rect.top + slopPx) && offset.x in rect.left..rect.right -> DragMode.EDGE_T
                    offset.y in (rect.bottom - slopPx)..(rect.bottom + slopPx) && offset.x in rect.left..rect.right -> DragMode.EDGE_B
                    offset.x in (rect.left - slopPx)..(rect.left + slopPx) && offset.y in rect.top..rect.bottom -> DragMode.EDGE_L
                    offset.x in (rect.right - slopPx)..(rect.right + slopPx) && offset.y in rect.top..rect.bottom -> DragMode.EDGE_R
                    rect.contains(offset) -> DragMode.INTERIOR
                    else -> DragMode.NONE
                }
            },
            onDrag = { change, dragAmount ->
                if (dragMode != DragMode.NONE) {
                    change.consume()
                    val next = currentCrop.handleDrag(currentDisplayRect, dragAmount, dragMode)
                    currentOnCropChange(next.clamp(currentDisplayRect))
                }
            },
            onDragEnd = { dragMode = DragMode.NONE },
            onDragCancel = { dragMode = DragMode.NONE }
        )
    }) {
        Canvas(Modifier.fillMaxSize()) {
            val screen = crop.toScreenRect(displayRect)
            // Dimmed background
            drawRect(Color.Black.copy(alpha = 0.55f), size = Size(size.width, screen.top)) // Top
            drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(0f, screen.bottom), size = Size(size.width, size.height - screen.bottom)) // Bottom
            drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(0f, screen.top), size = Size(screen.left, screen.height)) // Left
            drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(screen.right, screen.top), size = Size(size.width - screen.right, screen.height)) // Right

            drawRect(Color.White, topLeft = Offset(screen.left, screen.top), size = Size(screen.width, screen.height), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            
            val thirdW = screen.width / 3f
            val thirdH = screen.height / 3f
            repeat(2) { i ->
                val x = screen.left + thirdW * (i + 1)
                drawLine(Color.White.copy(alpha = 0.45f), Offset(x, screen.top), Offset(x, screen.bottom), 1.dp.toPx())
                val y = screen.top + thirdH * (i + 1)
                drawLine(Color.White.copy(alpha = 0.45f), Offset(screen.left, y), Offset(screen.right, y), 1.dp.toPx())
            }
            
            screen.cornerPoints().forEach { center ->
                drawCircle(Color.White, handleSize.toPx() / 2, center)
            }
        }
    }
}

private fun CropRect.handleDrag(displayRect: androidx.compose.ui.geometry.Rect, drag: Offset, mode: DragMode): CropRect {
    val dx = drag.x / displayRect.width
    val dy = drag.y / displayRect.height
    return when (mode) {
        DragMode.CORNER_TL -> CropRect(left + dx, top + dy, right, bottom)
        DragMode.CORNER_TR -> CropRect(left, top + dy, right + dx, bottom)
        DragMode.CORNER_BL -> CropRect(left + dx, top, right, bottom + dy)
        DragMode.CORNER_BR -> CropRect(left, top, right + dx, bottom + dy)
        DragMode.EDGE_T -> CropRect(left, top + dy, right, bottom)
        DragMode.EDGE_B -> CropRect(left, top, right, bottom + dy)
        DragMode.EDGE_L -> CropRect(left + dx, top, right, bottom)
        DragMode.EDGE_R -> CropRect(left, top, right + dx, bottom)
        DragMode.INTERIOR -> CropRect(left + dx, top + dy, right + dx, bottom + dy)
        else -> this
    }
}

private fun runScan(scope: kotlinx.coroutines.CoroutineScope, stateSetter: (ScanState) -> Unit, label: String, quotaManager: QuotaManager? = null, block: suspend () -> Pair<IdentifyResult, List<ShopItem>>) {
    stateSetter(ScanState.Sending(label))
    scope.launch {
        stateSetter(
            try {
                val (product, prices) = block()
                quotaManager?.consumeQuota()
                ScanState.Done(product, prices)
            } catch (e: Exception) {
                if (e is java.io.IOException) {
                    android.util.Log.e("BackendClient", "Connection failure", e)
                    ScanState.Error("Can't reach the server — check your connection")
                } else {
                    ScanState.Error(e.message ?: "Something went wrong")
                }
            },
        )
    }
}

private suspend fun extractKeyframes(file: File, count: Int = 8): List<ByteArray> = withContext(Dispatchers.IO) {
    val mmr = MediaMetadataRetriever()
    try {
        mmr.setDataSource(file.path)
        val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        if (durationMs <= 0) throw IllegalStateException("The video has no duration.")
        val stepMs = durationMs / count
        (0 until count).mapNotNull { i ->
            val timeUs = (stepMs * i + stepMs / 2) * 1000
            mmr.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { toCappedJpeg(it, 512) }
        }.ifEmpty { throw IllegalStateException("Could not extract frames from the video.") }
    } finally {
        runCatching { mmr.release() }
    }
}

private fun toCappedJpeg(bitmap: Bitmap, maxSide: Int, quality: Int = 80): ByteArray {
    var b = bitmap
    val longest = maxOf(b.width, b.height)
    if (longest > maxSide) {
        val scale = maxSide.toFloat() / longest
        b = Bitmap.createScaledBitmap(b, (b.width * scale).toInt(), (b.height * scale).toInt(), true)
    }
    val out = ByteArrayOutputStream()
    b.compress(Bitmap.CompressFormat.JPEG, quality, out)
    return out.toByteArray()
}

private fun Bitmap.cropImage(rect: CropRect): Bitmap {
    val l = (rect.left * width).toInt().coerceIn(0, width - 1)
    val t = (rect.top * height).toInt().coerceIn(0, height - 1)
    val r = (rect.right * width).toInt().coerceIn(l + 1, width)
    val b = (rect.bottom * height).toInt().coerceIn(t + 1, height)
    return Bitmap.createBitmap(this, l, t, r - l, b - t)
}

private fun toCappedJpeg(data: ByteArray, maxSide: Int, quality: Int = 80): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return data
    return toCappedJpeg(bitmap, maxSide, quality)
}

private suspend fun android.content.Context.loadCropImage(uri: Uri): CropImage = withContext(Dispatchers.IO) {
    val bitmap = if (android.os.Build.VERSION.SDK_INT >= 28) {
        val source = ImageDecoder.createSource(contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val maxSide = 2048
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > maxSide) {
                val scale = maxSide.toFloat() / longest
                decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
            }
            decoder.isMutableRequired = false
        }
    } else {
        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not read image" }
            BitmapFactory.decodeStream(input) ?: error("Could not decode image")
        }
    }.rotateIfNeeded(uri)
    CropImage(uri, bitmap)
}

private fun Bitmap.rotateIfNeeded(uri: Uri): Bitmap = this

private fun android.content.Context.copyUriToCache(uri: Uri, prefix: String): File? {
    val extension = when (contentResolver.getType(uri)) {
        "video/mp4" -> ".mp4"
        else -> ".bin"
    }
    val file = File(cacheDir, "$prefix${System.currentTimeMillis()}$extension")
    contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { output -> input.copyTo(output) } } ?: return null
    return file
}

private fun drawCurvedCorners(rect: androidx.compose.ui.geometry.Rect, color: Color) {}

private fun capturePhoto(imageCapture: ImageCapture, executor: java.util.concurrent.Executor, onResult: (ByteArray?, String?) -> Unit) {
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            try {
                var bitmap = image.toBitmap()
                val rotation = image.imageInfo.rotationDegrees
                if (rotation != 0) {
                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
                onResult(toCappedJpeg(bitmap, 1280, 80), null)
            } catch (e: Exception) {
                onResult(null, e.message)
            } finally {
                image.close()
            }
        }

        override fun onError(exception: ImageCaptureException) {
            onResult(null, exception.message)
        }
    })
}

private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

private fun CropRect.toScreenRect(displayRect: androidx.compose.ui.geometry.Rect): androidx.compose.ui.geometry.Rect {
    return androidx.compose.ui.geometry.Rect(
        displayRect.left + left * displayRect.width,
        displayRect.top + top * displayRect.height,
        displayRect.left + right * displayRect.width,
        displayRect.top + bottom * displayRect.height,
    )
}

private fun CropRect.clamp(displayRect: androidx.compose.ui.geometry.Rect): CropRect {
    val minW = (64f / displayRect.width).coerceAtMost(1f)
    val minH = (64f / displayRect.height).coerceAtMost(1f)
    val width = (right - left).coerceAtLeast(minW)
    val height = (bottom - top).coerceAtLeast(minH)
    var l = left.coerceIn(0f, 1f - width)
    var t = top.coerceIn(0f, 1f - height)
    var r = l + width
    var b = t + height
    if (r > 1f) {
        l -= r - 1f
        r = 1f
    }
    if (b > 1f) {
        t -= b - 1f
        b = 1f
    }
    return CropRect(l.coerceIn(0f, 1f - minW), t.coerceIn(0f, 1f - minH), r.coerceIn(minW, 1f), b.coerceIn(minH, 1f))
}

private fun CropRect.dragWhole(displayRect: androidx.compose.ui.geometry.Rect, drag: Offset): CropRect {
    val dx = drag.x / displayRect.width
    val dy = drag.y / displayRect.height
    return CropRect(left + dx, top + dy, right + dx, bottom + dy)
}

private fun CropRect.dragCorner(displayRect: androidx.compose.ui.geometry.Rect, position: Offset): CropRect {
    val x = ((position.x - displayRect.left) / displayRect.width).coerceIn(0f, 1f)
    val y = ((position.y - displayRect.top) / displayRect.height).coerceIn(0f, 1f)
    val corners = listOf(
        Offset(displayRect.left, displayRect.top),
        Offset(displayRect.right, displayRect.top),
        Offset(displayRect.left, displayRect.bottom),
        Offset(displayRect.right, displayRect.bottom),
    )
    val nearest = corners.minByOrNull { it.distanceTo(position) } ?: corners.first()
    return when (nearest) {
        corners[0] -> CropRect(x, y, right, bottom)
        corners[1] -> CropRect(left, y, x, bottom)
        corners[2] -> CropRect(x, top, right, y)
        else -> CropRect(left, top, x, y)
    }
}

private fun fitRect(bitmapW: Int, bitmapH: Int, boxW: Float, boxH: Float): androidx.compose.ui.geometry.Rect {
    val scale = min(boxW / bitmapW, boxH / bitmapH)
    val w = bitmapW * scale
    val h = bitmapH * scale
    val left = (boxW - w) / 2f
    val top = (boxH - h) / 2f
    return androidx.compose.ui.geometry.Rect(left, top, left + w, top + h)
}

private fun androidx.compose.ui.geometry.Rect.cornerPoints() = listOf(
    Offset(left, top),
    Offset(right, top),
    Offset(left, bottom),
    Offset(right, bottom),
)

private fun androidx.compose.ui.geometry.Rect.contains(point: Offset): Boolean = point.x in left..right && point.y in top..bottom

private fun Offset.distanceTo(other: Offset): Float = kotlin.math.hypot(x - other.x, y - other.y)
