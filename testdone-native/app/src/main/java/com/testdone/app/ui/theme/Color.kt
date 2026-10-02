package com.testdone.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── TestDone brand palette (matches the web app's ink/indigo system) ─────────

val Ink950 = Color(0xFF09090B)
val Ink900 = Color(0xFF0A0B1E)
val Ink850 = Color(0xFF0F1126)
val Ink800 = Color(0xFF14162E)
val Ink700 = Color(0xFF1B1D3A)

// ── neon dark palette ("dark neon" mode: near-black + electric accents) ─────
val NeonCyan = Color(0xFF22D3EE)
val NeonBg950 = Color(0xFF07080D)
val NeonBg900 = Color(0xFF0C0E16)
val NeonBg850 = Color(0xFF10131E)
val NeonBg800 = Color(0xFF161A28)
val NeonBg700 = Color(0xFF1D2233)

val Indigo500 = Color(0xFF6366F1)
val Indigo600 = Color(0xFF4F46E5)
val Indigo400 = Color(0xFF818CF8)
val Indigo300 = Color(0xFFA5B4FC)
val Violet500 = Color(0xFF8B5CF6)
val Violet400 = Color(0xFFA78BFA)
val Violet300 = Color(0xFFC4B5FD)
val Fuchsia500 = Color(0xFFD946EF)
val Fuchsia400 = Color(0xFFE879F9)

val Emerald500 = Color(0xFF10B981)
val Emerald400 = Color(0xFF34D399)
val Emerald300 = Color(0xFF6EE7B7)
val Amber500 = Color(0xFFF59E0B)
val Amber400 = Color(0xFFFBBF24)
val Amber300 = Color(0xFFFCD34D)
val Rose500 = Color(0xFFF43F5E)
val Rose400 = Color(0xFFFB7185)
val Rose300 = Color(0xFFFCA5A5)
val Sky400 = Color(0xFF38BDF8)

// Dark theme text hierarchy
val TextPrimaryDark = Color(0xFFE5E7F5)
val TextSecondaryDark = Color(0xFF8B8FB8)
val TextMutedDark = Color(0xFF6B6E85)

// Light theme
val BgLight = Color(0xFFF8F9FC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEEF0F8)
val TextPrimaryLight = Color(0xFF1A1B2E)
val TextSecondaryLight = Color(0xFF64748B)
val TextMutedLight = Color(0xFF94A3B8)

/** Brand gradient brushes live in Theme.kt; these are plain stops for charts. */
object TdColors {
    val brandStart = Indigo500
    val brandEnd = Violet500
    val ultraStart = Violet500
    val ultraEnd = Fuchsia500
    val passStart = Indigo500
    val passEnd = Violet500
}
