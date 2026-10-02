package com.testdone.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.testdone.app.R

/**
 * Real-style exam emblems (AI-generated official-style crests, circular tiles).
 * One emblem covers the whole family: SSC variants share the SSC crest, etc.
 */
private val EXAM_LOGOS: Map<String, Int> = mapOf(
    "ssc-cgl" to R.drawable.exam_logo_ssc,
    "ssc-chsl" to R.drawable.exam_logo_ssc,
    "ssc_gd" to R.drawable.exam_logo_ssc,
    "ssc_mts" to R.drawable.exam_logo_ssc,
    "sbi_po" to R.drawable.exam_logo_sbi,
    "sbi_clerk" to R.drawable.exam_logo_sbi,
    "ibps_po" to R.drawable.exam_logo_ibps,
    "ibps_clerk" to R.drawable.exam_logo_ibps,
    "rbi_assistant" to R.drawable.exam_logo_rbi,
    "rbi_grade_b" to R.drawable.exam_logo_rbi,
    "jee-main" to R.drawable.exam_logo_jee,
    "jee-advanced" to R.drawable.exam_logo_jee,
    "neet" to R.drawable.exam_logo_neet,
    "upsc" to R.drawable.exam_logo_upsc,
    "ca_foundation" to R.drawable.exam_logo_ca,
    "gate-cse" to R.drawable.exam_logo_gate,
    "ugc_net_paper1" to R.drawable.exam_logo_ugc,
    "nda" to R.drawable.exam_logo_nda,
)

/** Drawable res id for the exam's emblem, or null when unknown. */
fun examLogoRes(examId: String?): Int? = examId?.let { EXAM_LOGOS[it] }

/**
 * Circular exam emblem tile — white app-icon style disc with a subtle
 * theme-aware ring. Works on light and dark surfaces.
 */
@Composable
fun ExamLogoTile(
    examId: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    ring: Boolean = true,
) {
    val res = examLogoRes(examId)
    if (res == null) return
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .let { if (ring) it.border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape) else it },
        )
    }
}

/**
 * Gradient-ring variant for explore grids: colorful gradient disc behind a
 * slightly smaller white disc with the emblem — reads like a real app icon.
 */
@Composable
fun ExamLogoBadge(
    examId: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    ringBrush: Brush? = null,
) {
    val res = examLogoRes(examId)
    if (res == null) return
    val brush = ringBrush ?: Brush.linearGradient(
        listOf(
            androidx.compose.material3.MaterialTheme.colorScheme.primary,
            androidx.compose.material3.MaterialTheme.colorScheme.secondary,
        )
    )
    Box(
        modifier = modifier
            .size(size)
            .background(brush, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size - 8.dp)
                .clip(CircleShape),
        )
    }
}
