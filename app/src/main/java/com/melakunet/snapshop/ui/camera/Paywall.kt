package com.melakunet.snapshop.ui.camera

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melakunet.snapshop.ui.theme.Brand
import com.melakunet.snapshop.ui.theme.Spacing

@Composable
fun Paywall(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val goldBrush = Brush.verticalGradient(listOf(Color(0xFFE6BE8A), Color(0xFFC8A86B), Color(0xFF9A7B43)))

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Brand.backgroundDark
    ) {
        Box(Modifier.fillMaxSize()) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.lg)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Snap & Shop Pro",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = Brand.accentDark,
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(Spacing.xl))

                FeatureList()

                Spacer(Modifier.height(Spacing.xxl))

                Button(
                    onClick = { Toast.makeText(context, "Available with Play Store release", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand.accentDark, contentColor = Brand.accentOn),
                    shape = RoundedCornerShape(Spacing.md)
                ) {
                    Text("Monthly — $4.99/mo", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(Spacing.md))

                OutlinedButton(
                    onClick = { Toast.makeText(context, "Available with Play Store release", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = goldBrush),
                    shape = RoundedCornerShape(Spacing.md)
                ) {
                    Text("Annual — $39.99/yr", color = Brand.accentDark, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(Spacing.xl))

                Text(
                    "Restore Purchase",
                    modifier = Modifier.clickable { Toast.makeText(context, "Available with Play Store release", Toast.LENGTH_SHORT).show() },
                    color = Brand.accentDark,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun FeatureList() {
    val features = listOf(
        "Unlimited Scans",
        "Deep / Video Scan",
        "Live Price Tracking",
        "Price Drop Alerts"
    )

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        features.forEach { feature ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(24.dp).background(Brand.accentDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Brand.accentOn)
                }
                Spacer(Modifier.width(Spacing.md))
                Text(feature, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
