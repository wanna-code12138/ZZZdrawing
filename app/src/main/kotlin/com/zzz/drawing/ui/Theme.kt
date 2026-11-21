package com.zzz.drawing.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

object ZzzColors {
    val Bg = Color(0xFF0B0D12)
    val Surface = Color(0xFF141821)
    val SurfaceElevated = Color(0xFF1C222F)
    val SurfaceHighlight = Color(0xFF282012)
    val Border = Color(0xFF2B3242)
    val BorderBright = Color(0xFF3E485E)
    val Gold = Color(0xFFF5A623)
    val GoldBright = Color(0xFFFFD15C)
    val GoldDim = Color(0xFF8C621B)
    val Cyan = Color(0xFF00E5FF)
    val Lime = Color(0xFFB2FF59)
    val Pink = Color(0xFFFF5AA5)
    val RedAccent = Color(0xFFFF5C77)
    val TextPrimary = Color(0xFFF5F7FB)
    val TextSecondary = Color(0xFFB0B8C9)
    val TextMuted = Color(0xFF747D91)
}

private val ZzzDarkColorScheme = darkColorScheme(
    primary = ZzzColors.Gold,
    onPrimary = Color(0xFF15120A),
    secondary = ZzzColors.Cyan,
    onSecondary = Color(0xFF041418),
    tertiary = ZzzColors.Lime,
    background = ZzzColors.Bg,
    onBackground = ZzzColors.TextPrimary,
    surface = ZzzColors.Surface,
    onSurface = ZzzColors.TextPrimary,
    surfaceVariant = ZzzColors.SurfaceElevated,
    onSurfaceVariant = ZzzColors.TextSecondary,
    outline = ZzzColors.Border,
    error = ZzzColors.RedAccent,
)

@Composable
fun ZzzDrawingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZzzDarkColorScheme,
        content = content,
    )
}

@Composable
fun ZzzCard(
    modifier: Modifier = Modifier,
    borderColor: Color = ZzzColors.Border,
    backgroundColor: Color = ZzzColors.Surface,
    highlightGradient: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        val bgModifier = if (highlightGradient) {
            Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2C2314),
                        backgroundColor,
                    )
                )
            )
        } else {
            Modifier
        }
        Column(
            modifier = bgModifier.padding(16.dp),
            content = content,
        )
    }
}

@Composable
fun ZzzTogglePill(
    text: String,
    selected: Boolean,
    accent: Color = ZzzColors.Gold,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.20f) else ZzzColors.SurfaceElevated)
            .border(
                width = if (selected) 1.4.dp else 1.dp,
                color = if (selected) accent else ZzzColors.Border,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) accent else ZzzColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}
