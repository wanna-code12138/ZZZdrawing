package com.zzz.drawing.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zzz.drawing.model.Mode1Result
import com.zzz.drawing.ui.ZzzCard
import com.zzz.drawing.ui.ZzzColors
import com.zzz.drawing.ui.ZzzTogglePill
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun BellCurveChartCard(
    result: Mode1Result,
    modifier: Modifier = Modifier,
) {
    val range = remember(result) {
        val cdf = result.cdf
        if (cdf.size <= 2) {
            0 to max(1, cdf.size - 1)
        } else {
            var minX = 1
            while (minX < cdf.size - 1 && cdf[minX] < 0.0005) minX++
            var maxX = cdf.size - 1
            while (maxX > minX + 1 && cdf[maxX - 1] > 0.9995) maxX--
            val pad = max(2, ((maxX - minX) * 0.05).roundToInt())
            max(1, minX - pad) to min(cdf.size - 1, maxX + pad)
        }
    }
    val minPull = range.first
    val maxPull = max(minPull + 1, range.second)

    var inspectedPull by remember(result) {
        mutableIntStateOf(result.q50.coerceIn(minPull, maxPull))
    }
    LaunchedEffect(result) {
        inspectedPull = result.q50.coerceIn(minPull, maxPull)
    }

    var showCdfOverlay by remember { mutableStateOf(true) }
    var useSmoothedCurve by remember { mutableStateOf(true) }

    val displayCurve = remember(result, minPull, maxPull, useSmoothedCurve) {
        val len = maxPull - minPull + 1
        val raw = DoubleArray(len) { idx -> result.probabilityAtPull(minPull + idx) }
        if (!useSmoothedCurve || len < 15) {
            raw
        } else {
            val radius = (len / 55).coerceIn(1, 5)
            DoubleArray(len) { idx ->
                var sum = 0.0
                var weightSum = 0.0
                for (d in -radius..radius) {
                    val j = idx + d
                    if (j in 0 until len) {
                        val w = (radius + 1 - abs(d)).toDouble()
                        sum += raw[j] * w
                        weightSum += w
                    }
                }
                if (weightSum > 0.0) sum / weightSum else raw[idx]
            }
        }
    }

    val exactProbAtCursor = result.probabilityAtPull(inspectedPull) * 100.0
    val cumProbAtCursor = result.cumulativeAtPull(inspectedPull) * 100.0

    ZzzCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "概率分布钟形曲线",
                color = ZzzColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ZzzColors.SurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = String.format(Locale.US, "μ %.1f · σ %.1f", result.expectedPulls, result.stdDev),
                    color = ZzzColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 三栏大号准星读数条
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ZzzColors.SurfaceElevated)
                .border(1.dp, ZzzColors.GoldDim, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "抽数",
                    color = ZzzColors.TextSecondary,
                    fontSize = 12.sp,
                )
                Text(
                    text = "${inspectedPull} 抽",
                    color = ZzzColors.GoldBright,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "累计毕业",
                    color = ZzzColors.TextSecondary,
                    fontSize = 12.sp,
                )
                Text(
                    text = String.format(Locale.US, "%.1f%%", cumProbAtCursor),
                    color = ZzzColors.Cyan,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = "当抽概率",
                    color = ZzzColors.TextSecondary,
                    fontSize = 12.sp,
                )
                Text(
                    text = String.format(Locale.US, "%.2f%%", exactProbAtCursor),
                    color = ZzzColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ZzzTogglePill(
                text = "累计曲线",
                selected = showCdfOverlay,
                accent = ZzzColors.Cyan,
                onClick = { showCdfOverlay = !showCdfOverlay },
                modifier = Modifier.weight(1f),
            )
            ZzzTogglePill(
                text = "平滑包络",
                selected = useSmoothedCurve,
                accent = ZzzColors.Gold,
                onClick = { useSmoothedCurve = !useSmoothedCurve },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val textMeasurer = rememberTextMeasurer()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0E1118))
                .border(1.dp, ZzzColors.Border, RoundedCornerShape(12.dp)),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(minPull, maxPull) {
                        val leftPad = 40.dp.toPx()
                        val rightPad = 14.dp.toPx()
                        detectTapGestures { offset ->
                            val chartW = max(1f, size.width - leftPad - rightPad)
                            val frac = ((offset.x - leftPad) / chartW).coerceIn(0f, 1f)
                            inspectedPull = (minPull + frac * (maxPull - minPull)).roundToInt()
                                .coerceIn(minPull, maxPull)
                        }
                    }
                    .pointerInput(minPull, maxPull) {
                        val leftPad = 40.dp.toPx()
                        val rightPad = 14.dp.toPx()
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val chartW = max(1f, size.width - leftPad - rightPad)
                            val frac = ((change.position.x - leftPad) / chartW).coerceIn(0f, 1f)
                            inspectedPull = (minPull + frac * (maxPull - minPull)).roundToInt()
                                .coerceIn(minPull, maxPull)
                        }
                    },
            ) {
                val leftPad = 42.dp.toPx()
                val rightPad = 14.dp.toPx()
                val topPad = 16.dp.toPx()
                val bottomPad = 28.dp.toPx()

                val chartW = max(1f, size.width - leftPad - rightPad)
                val chartH = max(1f, size.height - topPad - bottomPad)
                val maxY = (displayCurve.maxOrNull() ?: 0.01).coerceAtLeast(1e-5) * 1.12
                val span = max(1, maxPull - minPull)

                fun xForPull(pull: Int): Float =
                    leftPad + ((pull - minPull).toFloat() / span.toFloat()) * chartW

                fun yForProb(p: Double): Float =
                    topPad + chartH - ((p / maxY).toFloat().coerceIn(0f, 1f)) * chartH

                fun yForCdf(c: Double): Float =
                    topPad + chartH - (c.toFloat().coerceIn(0f, 1f)) * chartH

                val axisStyle = TextStyle(color = ZzzColors.TextMuted, fontSize = 10.sp)
                for (step in 0..4) {
                    val frac = step / 4f
                    val y = topPad + chartH * (1f - frac)
                    drawLine(
                        color = Color(0xFF212633),
                        start = Offset(leftPad, y),
                        end = Offset(leftPad + chartW, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                    if (step > 0) {
                        val labelVal = maxY * frac * 100.0
                        val label = if (labelVal >= 1.0) {
                            String.format(Locale.US, "%.1f%%", labelVal)
                        } else {
                            String.format(Locale.US, "%.2f%%", labelVal)
                        }
                        val measured = textMeasurer.measure(label, axisStyle)
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                x = max(2f, leftPad - measured.size.width - 5.dp.toPx()),
                                y = y - measured.size.height / 2f,
                            ),
                        )
                    }
                }

                val tickCount = 5
                for (t in 0..tickCount) {
                    val pullTick = minPull + ((span.toFloat() * t) / tickCount).roundToInt()
                    val x = xForPull(pullTick)
                    drawLine(
                        color = Color(0xFF212633),
                        start = Offset(x, topPad),
                        end = Offset(x, topPad + chartH),
                        strokeWidth = 0.8.dp.toPx(),
                    )
                    val measured = textMeasurer.measure("$pullTick", axisStyle)
                    val tx = (x - measured.size.width / 2f).coerceIn(leftPad, size.width - measured.size.width - 2f)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(tx, topPad + chartH + 6.dp.toPx()),
                    )
                }

                // 25% ~ 75% 核心区
                val iqrPath = Path()
                val q25Clamped = result.q25.coerceIn(minPull, maxPull)
                val q75Clamped = result.q75.coerceIn(q25Clamped, maxPull)
                if (q75Clamped > q25Clamped) {
                    iqrPath.moveTo(xForPull(q25Clamped), topPad + chartH)
                    for (p in q25Clamped..q75Clamped) {
                        val v = displayCurve[(p - minPull).coerceIn(displayCurve.indices)]
                        iqrPath.lineTo(xForPull(p), yForProb(v))
                    }
                    iqrPath.lineTo(xForPull(q75Clamped), topPad + chartH)
                    iqrPath.close()
                    drawPath(
                        path = iqrPath,
                        color = ZzzColors.Gold.copy(alpha = 0.14f),
                    )
                }

                val stride = max(1, span / 220)
                val areaPath = Path()
                val strokePath = Path()
                areaPath.moveTo(xForPull(minPull), topPad + chartH)
                var firstPoint = true
                var idx = 0
                while (idx < displayCurve.size) {
                    val pull = minPull + idx
                    val x = xForPull(pull)
                    val y = yForProb(displayCurve[idx])
                    areaPath.lineTo(x, y)
                    if (firstPoint) {
                        strokePath.moveTo(x, y)
                        firstPoint = false
                    } else {
                        strokePath.lineTo(x, y)
                    }
                    if (idx == displayCurve.size - 1) break
                    idx = min(displayCurve.size - 1, idx + stride)
                }
                areaPath.lineTo(xForPull(maxPull), topPad + chartH)
                areaPath.close()

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ZzzColors.Gold.copy(alpha = 0.44f),
                            ZzzColors.Gold.copy(alpha = 0.04f),
                        ),
                        startY = topPad,
                        endY = topPad + chartH,
                    ),
                )
                drawPath(
                    path = strokePath,
                    color = ZzzColors.GoldBright,
                    style = Stroke(
                        width = 2.4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                if (showCdfOverlay) {
                    val cdfPath = Path()
                    var cdfFirst = true
                    var cIdx = minPull
                    while (cIdx <= maxPull) {
                        val x = xForPull(cIdx)
                        val y = yForCdf(result.cumulativeAtPull(cIdx))
                        if (cdfFirst) {
                            cdfPath.moveTo(x, y)
                            cdfFirst = false
                        } else {
                            cdfPath.lineTo(x, y)
                        }
                        if (cIdx == maxPull) break
                        cIdx = min(maxPull, cIdx + stride)
                    }
                    drawPath(
                        path = cdfPath,
                        color = ZzzColors.Cyan.copy(alpha = 0.85f),
                        style = Stroke(
                            width = 1.7.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                        ),
                    )
                }

                val meanPullRounded = result.expectedPulls.roundToInt().coerceIn(minPull, maxPull)
                val meanX = xForPull(meanPullRounded)
                drawLine(
                    color = ZzzColors.Lime.copy(alpha = 0.72f),
                    start = Offset(meanX, topPad),
                    end = Offset(meanX, topPad + chartH),
                    strokeWidth = 1.3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                )

                val cursorX = xForPull(inspectedPull)
                val cursorCurveVal = displayCurve[(inspectedPull - minPull).coerceIn(displayCurve.indices)]
                val cursorY = yForProb(cursorCurveVal)

                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(cursorX, topPad),
                    end = Offset(cursorX, topPad + chartH),
                    strokeWidth = 1.2.dp.toPx(),
                )
                drawCircle(
                    color = ZzzColors.GoldBright,
                    radius = 5.5.dp.toPx(),
                    center = Offset(cursorX, cursorY),
                )
                drawCircle(
                    color = Color(0xFF0E1118),
                    radius = 2.4.dp.toPx(),
                    center = Offset(cursorX, cursorY),
                )

                if (showCdfOverlay) {
                    val cdfY = yForCdf(result.cumulativeAtPull(inspectedPull))
                    drawCircle(
                        color = ZzzColors.Cyan,
                        radius = 4.8.dp.toPx(),
                        center = Offset(cursorX, cdfY),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendDot(color = ZzzColors.GoldBright, text = "当抽概率")
            if (showCdfOverlay) {
                LegendDot(color = ZzzColors.Cyan, text = "累计概率")
            }
            LegendDot(color = ZzzColors.Lime, text = "期望均值")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = inspectedPull.toFloat(),
            onValueChange = { inspectedPull = it.roundToInt().coerceIn(minPull, maxPull) },
            valueRange = minPull.toFloat()..maxPull.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = ZzzColors.GoldBright,
                activeTrackColor = ZzzColors.Gold,
                inactiveTrackColor = ZzzColors.Border,
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(-10, -1, 1, 10).forEach { delta ->
                val label = if (delta > 0) "+${delta}" else "$delta"
                OutlinedButton(
                    onClick = { inspectedPull = (inspectedPull + delta).coerceIn(minPull, maxPull) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, ZzzColors.BorderBright),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                ) {
                    Text(
                        text = label,
                        color = ZzzColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = ZzzColors.TextSecondary, fontSize = 12.sp)
    }
}
