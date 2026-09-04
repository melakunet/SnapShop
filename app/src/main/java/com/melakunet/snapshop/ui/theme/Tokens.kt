package com.melakunet.snapshop.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Design tokens — 1:1 port of iOS Theme/Tokens.swift. */
object Brand {
    val backgroundLight = Color(0xFFFAF8F5)
    val backgroundDark = Color(0xFF0E0E10)
    val surfaceLight = Color(0xFFFFFFFF)
    val surfaceDark = Color(0xFF1A1A1D)
    val surfaceAltLight = Color(0xFFF2EEE7)
    val surfaceAltDark = Color(0xFF232327)
    val textPrimaryLight = Color(0xFF1A1A1A)
    val textPrimaryDark = Color(0xFFF5F2EC)
    val textSecondaryLight = Color(0xFF6B6357)
    val textSecondaryDark = Color(0xFFA8A29A)
    val borderLight = Color(0xFFE5E0D8)
    val borderDark = Color(0xFF2A2A2E)
    val accentLight = Color(0xFF856127)
    val accentDark = Color(0xFFC8A86B)
    val accentDeep = Color(0xFF9A7B43)
    val accentOn = Color(0xFF1A1A1A)
    val scanDeep = Color(0xFF567CAB)
    val success = Color(0xFF4F7A5B)
    val error = Color(0xFFB3433B)
    val warning = Color(0xFFB58A2E)
}

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

object Radius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
}
