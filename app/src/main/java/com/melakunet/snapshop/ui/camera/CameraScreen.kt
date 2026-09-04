package com.melakunet.snapshop.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.melakunet.snapshop.models.IdentifyResult
import com.melakunet.snapshop.models.ShopItem
import com.melakunet.snapshop.network.BackendClient
import com.melakunet.snapshop.ui.theme.Brand
import com.melakunet.snapshop.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

private sealed interface ScanState {
    data object Idle : ScanState
    data object Sending : ScanState
    data class Done(val product: IdentifyResult, val prices: List<ShopItem>) : ScanState
    data class Error(val message: String) : ScanState
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    if (!hasPermission) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Snap&Shop needs the camera to identify products",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Spacing.lg))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Allow camera")
            }
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ScanState>(ScanState.Idle) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    Box(Modifier.fillMaxSize()) {
        // Live camera preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )

        // Corner-bracket viewfinder (mirrors iOS Precision mode)
        Canvas(Modifier.fillMaxSize().padding(48.dp)) {
            val len = 44.dp.toPx()
            val stroke = 4.dp.toPx()
            val w = size.width
            val h = size.height
            val gold = Color(0xFFC8A86B)
            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                drawLine(gold, Offset(x, y), Offset(x + len * dx, y), stroke, StrokeCap.Round)
                drawLine(gold, Offset(x, y), Offset(x, y + len * dy), stroke, StrokeCap.Round)
            }
            corner(0f, 0f, 1f, 1f)
            corner(w, 0f, -1f, 1f)
            corner(0f, h, 1f, -1f)
            corner(w, h, -1f, -1f)
        }

        // Capture button
        if (state is ScanState.Idle) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.xxl)
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Brand.accentDark)
                    .clickable {
                        state = ScanState.Sending
                        capture(imageCapture, context.let { ContextCompat.getMainExecutor(it) }) { jpeg, err ->
                            if (jpeg == null) {
                                state = ScanState.Error(err ?: "Capture failed")
                            } else {
                                scope.launch {
                                    state = try {
                                        val (product, prices) = BackendClient.scan(jpeg)
                                        ScanState.Done(product, prices)
                                    } catch (e: Exception) {
                                        ScanState.Error(e.message ?: "Something went wrong")
                                    }
                                }
                            }
                        }
                    },
            )
        }

        when (val s = state) {
            is ScanState.Sending -> Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Brand.accentDark)
                    Spacer(Modifier.height(Spacing.md))
                    Text("Identifying...", color = Color.White)
                }
            }
            is ScanState.Done -> ResultPanel(s.product, s.prices) { state = ScanState.Idle }
            is ScanState.Error -> Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(Spacing.lg),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(Modifier.padding(Spacing.lg)) {
                    Text("Scan failed", style = MaterialTheme.typography.titleMedium, color = Brand.error)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(s.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(Spacing.md))
                    Button(onClick = { state = ScanState.Idle }) { Text("Try again") }
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun ResultPanel(product: IdentifyResult, prices: List<ShopItem>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            val name = listOf(product.brand, product.model)
                .filter { it.isNotEmpty() }.joinToString(" ")
                .ifEmpty { product.category }
            Text(name, style = MaterialTheme.typography.headlineMedium)
            Text(
                product.category + "  ·  " + (product.confidence * 100).toInt() + "% confident",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            product.plant?.warning?.let { warning ->
                Spacer(Modifier.height(Spacing.md))
                Surface(shape = RoundedCornerShape(12.dp), color = Brand.error) {
                    Text(
                        "CAUTION (" + warning.level + "): " + warning.note,
                        modifier = Modifier.padding(Spacing.md),
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            if (prices.isEmpty()) {
                Text("No prices to show.", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(prices) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().clickable {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.link)))
                                }
                            },
                        ) {
                            Row(
                                Modifier.padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.source, style = MaterialTheme.typography.titleMedium)
                                    if (item.delivery.isNotEmpty()) {
                                        Text(
                                            item.delivery,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(
                                    item.price,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Scan again")
            }
        }
    }
}

private fun capture(
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    onResult: (ByteArray?, String?) -> Unit,
) {
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            try {
                var bitmap = image.toBitmap()
                val rotation = image.imageInfo.rotationDegrees
                if (rotation != 0) {
                    val m = Matrix().apply { postRotate(rotation.toFloat()) }
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
                }
                // Cap the longest side at 1280px to keep uploads fast (mirrors iOS ImageCropper.cap)
                val maxSide = maxOf(bitmap.width, bitmap.height)
                if (maxSide > 1280) {
                    val scale = 1280f / maxSide
                    bitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt(),
                        (bitmap.height * scale).toInt(),
                        true,
                    )
                }
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                onResult(out.toByteArray(), null)
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
