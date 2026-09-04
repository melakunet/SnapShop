package com.melakunet.snapshop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Brand.accentLight,
    onPrimary = Color.White,
    background = Brand.backgroundLight,
    surface = Brand.surfaceLight,
    surfaceVariant = Brand.surfaceAltLight,
    onBackground = Brand.textPrimaryLight,
    onSurface = Brand.textPrimaryLight,
    onSurfaceVariant = Brand.textSecondaryLight,
    outline = Brand.borderLight,
    error = Brand.error,
)

private val DarkColors = darkColorScheme(
    primary = Brand.accentDark,
    onPrimary = Brand.accentOn,
    background = Brand.backgroundDark,
    surface = Brand.surfaceDark,
    surfaceVariant = Brand.surfaceAltDark,
    onBackground = Brand.textPrimaryDark,
    onSurface = Brand.textPrimaryDark,
    onSurfaceVariant = Brand.textSecondaryDark,
    outline = Brand.borderDark,
    error = Brand.error,
)

private val SnapShopTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 17.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp),
)

@Composable
fun SnapShopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SnapShopTypography,
        content = content,
    )
}
