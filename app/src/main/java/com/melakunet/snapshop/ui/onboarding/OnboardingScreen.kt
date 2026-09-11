package com.melakunet.snapshop.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melakunet.snapshop.ui.theme.Brand
import com.melakunet.snapshop.ui.theme.Spacing
import kotlinx.coroutines.launch

private data class Slide(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val checklist: List<Pair<String, List<String>>> = emptyList()
)

private val slides = listOf(
    Slide(
        Icons.Filled.CameraAlt,
        "Snap It",
        "Point your camera at any product — no barcode needed. Compare live prices from Amazon, Walmart, Best Buy, and more in seconds.",
    ),
    Slide(
        Icons.Filled.Lens, // Closest to aperture in standard icons if Aperture is missing
        "Two Ways to Scan",
        "Precision captures one sharp photo for everyday items. Deep pans a video to fully identify complex or multi-sided products. Switch modes any time.",
    ),
    Slide(
        Icons.Filled.Shield,
        "Your Privacy",
        "Here's exactly what happens with your data.",
        checklist = listOf(
            "Uploads" to listOf(
                "One compressed photo per Precision scan",
                "Up to 8 keyframes per Deep scan — nothing else"
            ),
            "Guarantees" to listOf(
                "Photo library never accessed without your action",
                "Location & contacts never collected",
                "Your data is never sold"
            )
        )
    ),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(Brand.backgroundLight)
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val slide = slides[page]
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Brand.accentDark.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        slide.icon,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Brand.accentDark,
                    )
                }
                Spacer(Modifier.height(Spacing.xl))
                Text(
                    slide.title,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    color = Brand.textPrimaryLight
                )
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    slide.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Brand.textSecondaryLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )

                if (slide.checklist.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.lg))
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        slide.checklist.forEach { (header, items) ->
                            Column {
                                Text(
                                    header.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Brand.textSecondaryLight
                                )
                                Spacer(Modifier.height(Spacing.xs))
                                items.forEach { item ->
                                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                                        Icon(
                                            Icons.Filled.Check,
                                            null,
                                            modifier = Modifier.size(16.dp).padding(top = 2.dp),
                                            tint = Brand.success
                                        )
                                        Spacer(Modifier.width(Spacing.sm))
                                        Text(item, style = MaterialTheme.typography.bodyMedium, color = Brand.textPrimaryLight)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            repeat(slides.size) { i ->
                val active = pagerState.currentPage == i
                Box(
                    Modifier
                        .height(8.dp)
                        .width(if (active) 24.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) Brand.accentDark
                            else Brand.borderLight,
                        ),
                )
            }
        }
        Spacer(Modifier.height(Spacing.xl))
        
        val isLastPage = pagerState.currentPage == slides.size - 1

        Button(
            onClick = {
                if (isLastPage) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand.accentDark, contentColor = Color.White),
            shape = RoundedCornerShape(Spacing.md)
        ) {
            Text(
                if (isLastPage) "Get started" else "Next",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        if (!isLastPage) {
            TextButton(
                onClick = onFinished,
                modifier = Modifier.padding(top = Spacing.sm)
            ) {
                Text("Skip", color = Brand.textSecondaryLight)
            }
        } else {
            Spacer(Modifier.height(Spacing.xl + 24.dp)) // Maintain layout height
        }
    }
}
