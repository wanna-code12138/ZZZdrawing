package com.zzz.drawing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zzz.drawing.model.Mode1Result
import com.zzz.drawing.model.QuantileTableRow
import com.zzz.drawing.ui.ZzzCard
import com.zzz.drawing.ui.ZzzColors
import java.util.Locale

private val TYPE_COL_WIDTH = 98.dp
private val PCT_COL_WIDTH = 54.dp
private val EXP_COL_WIDTH = 78.dp
private val GRID_LINE_COLOR = Color(0xFF394152)

@Composable
fun PercentileTableCard(
    result: Mode1Result,
    modifier: Modifier = Modifier,
) {
    val discountBadge = if (result.discountConfig.enabled) "余波打折" else "无兑换折扣"
    val bannerTitle = when {
        result.targetMindscape >= 0 && result.targetRefinement > 0 ->
            "限定 S 角色影画 0~6 + ${result.targetRefinement}星专武"
        result.targetMindscape >= 0 ->
            "限定 S 角色影画 0~6（无专武）"
        else ->
            "限定 S 音擎 1~5星（仅专武）"
    }

    ZzzCard(
        modifier = modifier,
        borderColor = ZzzColors.GoldDim,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "概率分位表",
                color = ZzzColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (result.discountConfig.enabled) {
                            ZzzColors.Lime.copy(alpha = 0.16f)
                        } else {
                            ZzzColors.SurfaceElevated
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = discountBadge,
                    color = if (result.discountConfig.enabled) ZzzColors.Lime else ZzzColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val horizontalScroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
                .clip(RoundedCornerShape(10.dp))
                .border(1.5.dp, ZzzColors.Gold, RoundedCornerShape(10.dp))
                .background(Color(0xFF12151D)),
        ) {
            Box(
                modifier = Modifier
                    .width(TYPE_COL_WIDTH + PCT_COL_WIDTH * 7 + EXP_COL_WIDTH)
                    .background(Color(0xFF0A0C11))
                    .padding(vertical = 9.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = bannerTitle,
                    color = ZzzColors.GoldBright,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .background(Color(0xFF1A1F29)),
            ) {
                HeaderCell(
                    text = "配置",
                    width = TYPE_COL_WIDTH,
                    modifier = Modifier.fillMaxHeight(),
                )
                Column {
                    Box(
                        modifier = Modifier
                            .width(PCT_COL_WIDTH * 7)
                            .border(0.5.dp, GRID_LINE_COLOR)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "达成概率所需抽数",
                            color = ZzzColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Row {
                        listOf("10%", "25%", "50%", "75%", "90%", "95%", "99%").forEach { pct ->
                            HeaderCell(text = pct, width = PCT_COL_WIDTH)
                        }
                    }
                }
                HeaderCell(
                    text = "期望抽数",
                    width = EXP_COL_WIDTH,
                    modifier = Modifier.fillMaxHeight(),
                )
            }

            result.comparisonTableRows.forEachIndexed { index, row ->
                QuantileRowView(row = row, isAlt = index % 2 == 1)
            }
        }
    }
}

@Composable
private fun HeaderCell(
    text: String,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(width)
            .border(0.5.dp, GRID_LINE_COLOR)
            .padding(vertical = 7.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = ZzzColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuantileRowView(row: QuantileTableRow, isAlt: Boolean) {
    val bgColor = when {
        row.isSelected -> Color(0xFF3E2F12)
        isAlt -> Color(0xFF1D222D)
        else -> Color(0xFF171B24)
    }
    val typeBg = when {
        row.isSelected -> Color(0xFF4F3B15)
        else -> Color(0xFF131720)
    }
    val textColor = if (row.isSelected) ZzzColors.GoldBright else ZzzColors.TextPrimary
    val labelColor = if (row.isSelected) ZzzColors.GoldBright else ZzzColors.TextSecondary

    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .background(bgColor),
    ) {
        DataCell(
            text = row.label,
            width = TYPE_COL_WIDTH,
            bgColor = typeBg,
            textColor = labelColor,
            bold = row.isSelected,
        )
        DataCell(text = row.q10.toString(), width = PCT_COL_WIDTH, bgColor = bgColor, textColor = textColor, bold = row.isSelected)
        DataCell(text = row.q25.toString(), width = PCT_COL_WIDTH, bgColor = bgColor, textColor = textColor, bold = row.isSelected)
        DataCell(text = row.q50.toString(), width = PCT_COL_WIDTH, bgColor = bgColor, textColor = textColor, bold = row.isSelected)
        DataCell(text = row.q75.toString(), width = PCT_COL_WIDTH, bgColor = bgColor, textColor = textColor, bold = row.isSelected)
        DataCell(text = row.q90.toString(), width = PCT_COL_WIDTH, bgColor = bgColor, textColor = textColor, bold = row.isSelected)
        DataCell(text = row.q95.toString(), width = PCT_COL_WIDTH, bgColor = bgColor, textColor = textColor, bold = row.isSelected)
        DataCell(text = row.q99.toString(), width = PCT_COL_WIDTH, bgColor = bgColor, textColor = textColor, bold = row.isSelected)
        DataCell(
            text = String.format(Locale.US, "%.2f", row.expectedPulls),
            width = EXP_COL_WIDTH,
            bgColor = if (row.isSelected) Color(0xFF4F3B15) else Color(0xFF151923),
            textColor = if (row.isSelected) ZzzColors.GoldBright else ZzzColors.Gold,
            bold = true,
        )
    }
}

@Composable
private fun DataCell(
    text: String,
    width: Dp,
    bgColor: Color,
    textColor: Color,
    bold: Boolean,
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(bgColor)
            .border(0.5.dp, GRID_LINE_COLOR)
            .padding(vertical = 9.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}
