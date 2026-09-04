package com.melakunet.snapshop.ui.onboarding

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.melakunet.snapshop.ui.theme.Spacing
import kotlinx.coroutines.launch

private data class Slide(val icon: ImageVector, val title: String, val body: String)

private val slides = listOf(
    Slide(
        Icons.Filled.CameraAlt,
        "Point. Snap. Know.",
        "Aim your camera at any product and Snap&Shop identifies it in seconds with AI vision.",
    ),
    Slide(
        Icons.Filled.ShoppingCart,
        "Best price, instantly",
        "Live prices from Amazon, Walmart, Best Buy and more - ranked so the best deal is always on top.",
    ),
    Slide(
        Icons.Filled.Lock,
        "Private by design",
        "Your scans stay on your device. No ads, no tracking, no selling your data.",
    ),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(Spacing.xl),
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
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        slide.icon,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(Spacing.xl))
                Text(
                    slide.title,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    slide.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        ),
                )
            }
        }
        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = {
                if (pagerState.currentPage < slides.size - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    onFinished()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (pagerState.currentPage < slides.size - 1) "Next" else "Get started")
        }
    }
}
