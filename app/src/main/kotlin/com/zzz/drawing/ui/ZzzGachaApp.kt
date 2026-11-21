package com.zzz.drawing.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zzz.drawing.model.DiscountConfig
import com.zzz.drawing.model.Mode1Result
import com.zzz.drawing.model.Mode2ConfigOutcome
import com.zzz.drawing.model.Mode2Result
import com.zzz.drawing.model.PullStepType
import com.zzz.drawing.model.ZzzGachaCalculator
import com.zzz.drawing.ui.components.BellCurveChartCard
import com.zzz.drawing.ui.components.PercentileTableCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ZzzGachaApp() {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    // 共用状态
    var discountEnabled by remember { mutableStateOf(false) }
    var isARankFull by remember { mutableStateOf(true) }
    var isStandardSFull by remember { mutableStateOf(false) }

    var charPity by remember { mutableIntStateOf(0) }
    var charGuaranteed by remember { mutableStateOf(false) }
    var weaponPity by remember { mutableIntStateOf(0) }
    var weaponGuaranteed by remember { mutableStateOf(false) }

    // 第一页：目标配置算抽数
    var targetMindscape by remember { mutableIntStateOf(0) }
    var targetRefinement by remember { mutableIntStateOf(1) }

    // 第二页：现有抽数算配置
    var polychromeText by remember { mutableStateOf("16000") }
    var tapeText by remember { mutableStateOf("20") }
    var signalText by remember { mutableStateOf("0") }
    var pullSequence by remember { mutableStateOf(ZzzGachaCalculator.defaultPullSequence) }
    var showOutlierTails by remember { mutableStateOf(false) }

    val discountConfig = remember(discountEnabled, isARankFull, isStandardSFull) {
        DiscountConfig(
            enabled = discountEnabled,
            isARankFull = isARankFull,
            isStandardSFull = isStandardSFull,
        )
    }

    val polychromes = polychromeText.toIntOrNull() ?: 0
    val tapes = tapeText.toIntOrNull() ?: 0
    val signals = signalText.toIntOrNull() ?: 0
    val previewTotalPulls = tapes + (polychromes / 160) + (signals / 20)

    ZzzDrawingTheme {
        Scaffold(
            containerColor = ZzzColors.Bg,
            topBar = {
                AppTopBar(
                    currentPage = pagerState.currentPage,
                    targetMindscape = targetMindscape,
                    targetRefinement = targetRefinement,
                    totalPulls = previewTotalPulls,
                )
            },
            bottomBar = {
                AppBottomBar(
                    selectedPage = pagerState.currentPage,
                    onSelectPage = { page ->
                        scope.launch { pagerState.animateScrollToPage(page) }
                    },
                )
            },
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) { page ->
                when (page) {
                    0 -> PageTargetToPulls(
                        targetMindscape = targetMindscape,
                        targetRefinement = targetRefinement,
                        onSelectMindscape = { m ->
                            targetMindscape = m
                            if (m == -1 && targetRefinement == 0) targetRefinement = 1
                        },
                        onSelectRefinement = { r ->
                            targetRefinement = r
                            if (r == 0 && targetMindscape == -1) targetMindscape = 0
                        },
                        charPity = charPity,
                        charGuaranteed = charGuaranteed,
                        weaponPity = weaponPity,
                        weaponGuaranteed = weaponGuaranteed,
                        onCharPityChange = { charPity = it },
                        onCharGuaranteedChange = { charGuaranteed = it },
                        onWeaponPityChange = { weaponPity = it },
                        onWeaponGuaranteedChange = { weaponGuaranteed = it },
                        discountConfig = discountConfig,
                        onDiscountEnabledChange = { discountEnabled = it },
                        onARankFullChange = { isARankFull = it },
                        onStandardSFullChange = { isStandardSFull = it },
                    )
                    1 -> PagePullsToConfig(
                        polychromeText = polychromeText,
                        tapeText = tapeText,
                        signalText = signalText,
                        onPolychromeChange = { polychromeText = filterDigits(it, 7) },
                        onTapeChange = { tapeText = filterDigits(it, 5) },
                        onSignalChange = { signalText = filterDigits(it, 6) },
                        charPity = charPity,
                        charGuaranteed = charGuaranteed,
                        weaponPity = weaponPity,
                        weaponGuaranteed = weaponGuaranteed,
                        onCharPityChange = { charPity = it },
                        onCharGuaranteedChange = { charGuaranteed = it },
                        onWeaponPityChange = { weaponPity = it },
                        onWeaponGuaranteedChange = { weaponGuaranteed = it },
                        discountConfig = discountConfig,
                        onDiscountEnabledChange = { discountEnabled = it },
                        onARankFullChange = { isARankFull = it },
                        onStandardSFullChange = { isStandardSFull = it },
                        pullSequence = pullSequence,
                        onPullSequenceChange = { pullSequence = it },
                        showOutlierTails = showOutlierTails,
                        onShowOutlierTailsChange = { showOutlierTails = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppTopBar(
    currentPage: Int,
    targetMindscape: Int,
    targetRefinement: Int,
    totalPulls: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZzzColors.Surface)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(ZzzColors.Gold)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "ZZZ",
                        color = Color(0xFF14110A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "绝区零调频计算",
                    color = ZzzColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            val badgeText = if (currentPage == 0) {
                shortTargetLabel(targetMindscape, targetRefinement)
            } else {
                "$totalPulls 抽"
            }
            val badgeColor = if (currentPage == 0) ZzzColors.GoldBright else ZzzColors.Cyan
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeColor.copy(alpha = 0.14f))
                    .border(1.dp, badgeColor.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        HorizontalDivider(color = ZzzColors.Border, thickness = 1.dp)
    }
}

@Composable
private fun AppBottomBar(
    selectedPage: Int,
    onSelectPage: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZzzColors.Surface),
    ) {
        HorizontalDivider(color = ZzzColors.Border, thickness = 1.dp)
        NavigationBar(
            containerColor = ZzzColors.Surface,
            tonalElevation = 0.dp,
            modifier = Modifier.navigationBarsPadding(),
        ) {
            NavigationBarItem(
                selected = selectedPage == 0,
                onClick = { onSelectPage(0) },
                icon = { BellNavIcon() },
                label = {
                    Text(
                        text = "目标算抽数",
                        fontSize = 13.sp,
                        fontWeight = if (selectedPage == 0) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ZzzColors.GoldBright,
                    selectedTextColor = ZzzColors.GoldBright,
                    indicatorColor = ZzzColors.Gold.copy(alpha = 0.18f),
                    unselectedIconColor = ZzzColors.TextMuted,
                    unselectedTextColor = ZzzColors.TextSecondary,
                ),
            )
            NavigationBarItem(
                selected = selectedPage == 1,
                onClick = { onSelectPage(1) },
                icon = { TapeNavIcon() },
                label = {
                    Text(
                        text = "抽数算配置",
                        fontSize = 13.sp,
                        fontWeight = if (selectedPage == 1) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ZzzColors.Cyan,
                    selectedTextColor = ZzzColors.Cyan,
                    indicatorColor = ZzzColors.Cyan.copy(alpha = 0.16f),
                    unselectedIconColor = ZzzColors.TextMuted,
                    unselectedTextColor = ZzzColors.TextSecondary,
                ),
            )
        }
    }
}

@Composable
private fun BellNavIcon() {
    val tint = LocalContentColor.current
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.10f, h * 0.80f)
            cubicTo(w * 0.28f, h * 0.80f, w * 0.34f, h * 0.20f, w * 0.50f, h * 0.20f)
            cubicTo(w * 0.66f, h * 0.20f, w * 0.72f, h * 0.80f, w * 0.90f, h * 0.80f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.08f, h * 0.84f),
            end = Offset(w * 0.92f, h * 0.84f),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun TapeNavIcon() {
    val tint = LocalContentColor.current
    Canvas(modifier = Modifier.size(22.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val rOuter = size.minDimension * 0.42f
        val rInner = size.minDimension * 0.16f
        drawCircle(
            color = tint,
            radius = rOuter,
            center = c,
            style = Stroke(width = 2.0.dp.toPx()),
        )
        drawCircle(
            color = tint,
            radius = rInner,
            center = c,
            style = Stroke(width = 1.8.dp.toPx()),
        )
        drawLine(tint, Offset(c.x, c.y - rOuter), Offset(c.x, c.y - rInner), strokeWidth = 1.8.dp.toPx())
        drawLine(tint, Offset(c.x - rOuter * 0.86f, c.y + rOuter * 0.5f), Offset(c.x - rInner * 0.86f, c.y + rInner * 0.5f), strokeWidth = 1.8.dp.toPx())
        drawLine(tint, Offset(c.x + rOuter * 0.86f, c.y + rOuter * 0.5f), Offset(c.x + rInner * 0.86f, c.y + rInner * 0.5f), strokeWidth = 1.8.dp.toPx())
    }
}

// ============================================================================
// 第一页：目标配置算抽数
// ============================================================================

@Composable
private fun PageTargetToPulls(
    targetMindscape: Int,
    targetRefinement: Int,
    onSelectMindscape: (Int) -> Unit,
    onSelectRefinement: (Int) -> Unit,
    charPity: Int,
    charGuaranteed: Boolean,
    weaponPity: Int,
    weaponGuaranteed: Boolean,
    onCharPityChange: (Int) -> Unit,
    onCharGuaranteedChange: (Boolean) -> Unit,
    onWeaponPityChange: (Int) -> Unit,
    onWeaponGuaranteedChange: (Boolean) -> Unit,
    discountConfig: DiscountConfig,
    onDiscountEnabledChange: (Boolean) -> Unit,
    onARankFullChange: (Boolean) -> Unit,
    onStandardSFullChange: (Boolean) -> Unit,
) {
    var showPityPanel by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val resultState by produceState<Mode1Result?>(
        initialValue = null,
        targetMindscape,
        targetRefinement,
        charPity,
        charGuaranteed,
        weaponPity,
        weaponGuaranteed,
        discountConfig,
    ) {
        value = withContext(Dispatchers.Default) {
            ZzzGachaCalculator.calculateMode1(
                targetMindscape = targetMindscape,
                targetRefinement = targetRefinement,
                charPity = charPity,
                charGuaranteed = charGuaranteed,
                weaponPity = weaponPity,
                weaponGuaranteed = weaponGuaranteed,
                discountConfig = discountConfig,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 顶部核心看板
        resultState?.let { res ->
            ZzzCard(
                borderColor = ZzzColors.GoldDim,
                backgroundColor = ZzzColors.Surface,
                highlightGradient = true,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = shortTargetLabel(targetMindscape, targetRefinement),
                        color = ZzzColors.GoldBright,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (discountConfig.enabled) {
                        StatusChip(text = "余波打折中", color = ZzzColors.Lime)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f 抽", res.expectedPulls),
                        color = ZzzColors.GoldBright,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "期望 ≈ ${(res.expectedPulls * 160).roundToInt()} 菲林",
                        color = ZzzColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = ZzzColors.Border)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    HeroMiniStat("50% 概率", "${res.q50} 抽", ZzzColors.TextPrimary, Modifier.weight(1f))
                    HeroMiniStat("90% 稳妥", "${res.q90} 抽", ZzzColors.Cyan, Modifier.weight(1f))
                    HeroMiniStat("99% 极非", "${res.q99} 抽", ZzzColors.Lime, Modifier.weight(1f))
                }
            }
        }

        // 目标配置与设置卡
        ZzzCard {
            Text(
                text = "目标配置",
                color = ZzzColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "角色影画（0命含本体）",
                color = ZzzColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            listOf(-1, 0, 1, 2, 3, 4, 5, 6).chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { m ->
                        SelectTile(
                            text = if (m == -1) "不抽" else "${m}命",
                            selected = targetMindscape == m,
                            accent = ZzzColors.Gold,
                            onClick = { onSelectMindscape(m) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "专武精炼（1精起）",
                color = ZzzColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            listOf(0, 1, 2, 3, 4, 5).chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { r ->
                        SelectTile(
                            text = if (r == 0) "不抽" else "${r}精",
                            selected = targetRefinement == r,
                            accent = ZzzColors.Cyan,
                            onClick = { onSelectRefinement(r) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = ZzzColors.Border)
            Spacer(modifier = Modifier.height(10.dp))

            DiscountInlineBlock(
                discountConfig = discountConfig,
                onDiscountEnabledChange = onDiscountEnabledChange,
                onARankFullChange = onARankFullChange,
                onStandardSFullChange = onStandardSFullChange,
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = ZzzColors.Border)
            Spacer(modifier = Modifier.height(10.dp))

            // 垫抽折叠栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ZzzColors.SurfaceElevated)
                    .clickable { showPityPanel = !showPityPanel }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "垫抽与大小保底",
                    color = ZzzColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "角色${charPity}${if (charGuaranteed) "大" else "小"} · 音擎${weaponPity}${if (weaponGuaranteed) "大" else "小"}  ${if (showPityPanel) "▲" else "▼"}",
                    color = ZzzColors.GoldBright,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            AnimatedVisibility(visible = showPityPanel) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    PityControlsBlock(
                        charPity = charPity,
                        charGuaranteed = charGuaranteed,
                        weaponPity = weaponPity,
                        weaponGuaranteed = weaponGuaranteed,
                        onCharPityChange = onCharPityChange,
                        onCharGuaranteedChange = onCharGuaranteedChange,
                        onWeaponPityChange = onWeaponPityChange,
                        onWeaponGuaranteedChange = onWeaponGuaranteedChange,
                    )
                }
            }
        }

        // 产物 1 与产物 2
        resultState?.let { res ->
            PercentileTableCard(result = res)
            BellCurveChartCard(result = res)
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

// ============================================================================
// 第二页：现有抽数算配置 (3σ 区间)
// ============================================================================

@Composable
private fun PagePullsToConfig(
    polychromeText: String,
    tapeText: String,
    signalText: String,
    onPolychromeChange: (String) -> Unit,
    onTapeChange: (String) -> Unit,
    onSignalChange: (String) -> Unit,
    charPity: Int,
    charGuaranteed: Boolean,
    weaponPity: Int,
    weaponGuaranteed: Boolean,
    onCharPityChange: (Int) -> Unit,
    onCharGuaranteedChange: (Boolean) -> Unit,
    onWeaponPityChange: (Int) -> Unit,
    onWeaponGuaranteedChange: (Boolean) -> Unit,
    discountConfig: DiscountConfig,
    onDiscountEnabledChange: (Boolean) -> Unit,
    onARankFullChange: (Boolean) -> Unit,
    onStandardSFullChange: (Boolean) -> Unit,
    pullSequence: List<PullStepType>,
    onPullSequenceChange: (List<PullStepType>) -> Unit,
    showOutlierTails: Boolean,
    onShowOutlierTailsChange: (Boolean) -> Unit,
) {
    val polychromes = polychromeText.toIntOrNull() ?: 0
    val tapes = tapeText.toIntOrNull() ?: 0
    val signals = signalText.toIntOrNull() ?: 0

    var showSequenceDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val resultState by produceState<Mode2Result?>(
        initialValue = null,
        polychromes,
        tapes,
        signals,
        charPity,
        charGuaranteed,
        weaponPity,
        weaponGuaranteed,
        discountConfig,
        pullSequence,
    ) {
        value = withContext(Dispatchers.Default) {
            ZzzGachaCalculator.calculateMode2(
                polychromes = polychromes,
                encryptedTapes = tapes,
                residualSignals = signals,
                charPity = charPity,
                charGuaranteed = charGuaranteed,
                weaponPity = weaponPity,
                weaponGuaranteed = weaponGuaranteed,
                discountConfig = discountConfig,
                pullSequence = pullSequence,
            )
        }
    }

    if (showSequenceDialog) {
        SequenceEditorDialog(
            sequence = pullSequence,
            onSequenceChange = onPullSequenceChange,
            onDismiss = { showSequenceDialog = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. 资源看板 & 输入卡片
        ZzzCard(
            borderColor = ZzzColors.Cyan.copy(alpha = 0.45f),
            highlightGradient = true,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "可用总抽数",
                        color = ZzzColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                    resultState?.let { res ->
                        Text(
                            text = "${res.totalPulls} 抽",
                            color = ZzzColors.GoldBright,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                resultState?.let { res ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ZzzColors.SurfaceElevated)
                            .border(1.dp, ZzzColors.Border, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "菲林${res.pullsFromPolychromes} + 母带${res.pullsFromTapes}" +
                                if (res.pullsFromSignals > 0) " + 余波${res.pullsFromSignals}" else "",
                            color = ZzzColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NumericResourceField(
                    label = "菲林",
                    value = polychromeText,
                    onValueChange = onPolychromeChange,
                    modifier = Modifier.weight(1.15f),
                )
                NumericResourceField(
                    label = "加密母带",
                    value = tapeText,
                    onValueChange = onTapeChange,
                    modifier = Modifier.weight(0.95f),
                )
                NumericResourceField(
                    label = "余波",
                    value = signalText,
                    onValueChange = onSignalChange,
                    modifier = Modifier.weight(0.9f),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickAddChip("+1600", Modifier.weight(1f)) {
                    onPolychromeChange((polychromes + 1600).toString())
                }
                QuickAddChip("+10抽", Modifier.weight(1f)) {
                    onTapeChange((tapes + 10).toString())
                }
                QuickAddChip("+50抽", Modifier.weight(1f)) {
                    onTapeChange((tapes + 50).toString())
                }
                QuickAddChip("清零", Modifier.weight(0.85f), textColor = ZzzColors.RedAccent) {
                    onPolychromeChange("0")
                    onTapeChange("0")
                    onSignalChange("0")
                }
            }
        }

        // 2. 保底与余波打折卡片
        ZzzCard {
            Text(
                text = "垫抽与打折设置",
                color = ZzzColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            PityControlsBlock(
                charPity = charPity,
                charGuaranteed = charGuaranteed,
                weaponPity = weaponPity,
                weaponGuaranteed = weaponGuaranteed,
                onCharPityChange = onCharPityChange,
                onCharGuaranteedChange = onCharGuaranteedChange,
                onWeaponPityChange = onWeaponPityChange,
                onWeaponGuaranteedChange = onWeaponGuaranteedChange,
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = ZzzColors.Border)
            Spacer(modifier = Modifier.height(12.dp))

            DiscountInlineBlock(
                discountConfig = discountConfig,
                onDiscountEnabledChange = onDiscountEnabledChange,
                onARankFullChange = onARankFullChange,
                onStandardSFullChange = onStandardSFullChange,
            )
        }

        // 3. 抽卡顺序卡片（精简预览 + 弹窗自定义）
        ZzzCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "抽卡顺序",
                    color = ZzzColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ZzzColors.Cyan.copy(alpha = 0.16f))
                        .border(1.dp, ZzzColors.Cyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable { showSequenceDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "自定义顺序",
                        color = ZzzColors.Cyan,
                        fontSize = 13.sp,
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
                    text = "默认(0+1→满命→满精)",
                    selected = pullSequence == ZzzGachaCalculator.defaultPullSequence,
                    accent = ZzzColors.Gold,
                    onClick = { onPullSequenceChange(ZzzGachaCalculator.defaultPullSequence) },
                    modifier = Modifier.weight(1f),
                )
                ZzzTogglePill(
                    text = "先满命后专武",
                    selected = pullSequence == charsThenWeaponsSequence(),
                    accent = ZzzColors.Gold,
                    onClick = { onPullSequenceChange(charsThenWeaponsSequence()) },
                    modifier = Modifier.weight(0.85f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ZzzTogglePill(
                    text = "2+1→满精→满命",
                    selected = pullSequence == twoPlusOneFirstSequence(),
                    accent = ZzzColors.Cyan,
                    onClick = { onPullSequenceChange(twoPlusOneFirstSequence()) },
                    modifier = Modifier.weight(1f),
                )
                ZzzTogglePill(
                    text = "仅抽角色",
                    selected = pullSequence == onlyCharsSequence(),
                    accent = ZzzColors.Cyan,
                    onClick = { onPullSequenceChange(onlyCharsSequence()) },
                    modifier = Modifier.weight(0.68f),
                )
                ZzzTogglePill(
                    text = "仅抽专武",
                    selected = pullSequence == onlyWeaponsSequence(),
                    accent = ZzzColors.Cyan,
                    onClick = { onPullSequenceChange(onlyWeaponsSequence()) },
                    modifier = Modifier.weight(0.68f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 彩色胶囊流预览顺序
            SequenceBadgesRow(sequence = pullSequence)
        }

        // 4. 3σ 概率预测结果
        resultState?.let { res ->
            val displayed = if (showOutlierTails) res.allOutcomes else res.threeSigmaOutcomes
            val hiddenCount = res.allOutcomes.size - res.threeSigmaOutcomes.size

            ZzzCard(borderColor = ZzzColors.GoldDim) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "配置概率预测 (3σ 内)",
                        color = ZzzColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    StatusChip(
                        text = String.format(Locale.US, "覆盖 %.1f%%", res.threeSigmaCoveredProbability * 100.0),
                        color = ZzzColors.Lime,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                displayed.forEach { outcome ->
                    CleanOutcomeCard(
                        outcome = outcome,
                        isMode = outcome.completedSteps == res.mostLikelyOutcome?.completedSteps,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (hiddenCount > 0) {
                    ZzzTogglePill(
                        text = if (showOutlierTails) {
                            "收起 3σ 外极小概率"
                        } else {
                            "查看 3σ 外极小概率 ($hiddenCount 项)"
                        },
                        selected = showOutlierTails,
                        accent = ZzzColors.Cyan,
                        onClick = { onShowOutlierTailsChange(!showOutlierTails) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

// ============================================================================
// 复用交互模块
// ============================================================================

@Composable
private fun HeroMiniStat(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, color = ZzzColors.TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = color,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun PityControlsBlock(
    charPity: Int,
    charGuaranteed: Boolean,
    weaponPity: Int,
    weaponGuaranteed: Boolean,
    onCharPityChange: (Int) -> Unit,
    onCharGuaranteedChange: (Boolean) -> Unit,
    onWeaponPityChange: (Int) -> Unit,
    onWeaponGuaranteedChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 角色池
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ZzzColors.SurfaceElevated)
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "角色池垫抽：$charPity",
                    color = ZzzColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                GuaranteeSegmentedToggle(
                    isGuaranteed = charGuaranteed,
                    accent = ZzzColors.Gold,
                    onToggle = onCharGuaranteedChange,
                )
            }
            Slider(
                value = charPity.toFloat(),
                onValueChange = { onCharPityChange(it.roundToInt().coerceIn(0, 89)) },
                valueRange = 0f..89f,
                colors = SliderDefaults.colors(
                    thumbColor = ZzzColors.GoldBright,
                    activeTrackColor = ZzzColors.Gold,
                    inactiveTrackColor = ZzzColors.Border,
                ),
            )
        }

        // 音擎池
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ZzzColors.SurfaceElevated)
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "音擎池垫抽：$weaponPity",
                    color = ZzzColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                GuaranteeSegmentedToggle(
                    isGuaranteed = weaponGuaranteed,
                    accent = ZzzColors.Cyan,
                    onToggle = onWeaponGuaranteedChange,
                )
            }
            Slider(
                value = weaponPity.toFloat(),
                onValueChange = { onWeaponPityChange(it.roundToInt().coerceIn(0, 79)) },
                valueRange = 0f..79f,
                colors = SliderDefaults.colors(
                    thumbColor = ZzzColors.Cyan,
                    activeTrackColor = ZzzColors.Cyan,
                    inactiveTrackColor = ZzzColors.Border,
                ),
            )
        }
    }
}

@Composable
private fun GuaranteeSegmentedToggle(
    isGuaranteed: Boolean,
    accent: Color,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ZzzColors.Bg)
            .border(1.dp, ZzzColors.Border, RoundedCornerShape(8.dp))
            .padding(2.dp),
    ) {
        listOf(false to "小保底", true to "大保底").forEach { (value, label) ->
            val selected = isGuaranteed == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) accent else Color.Transparent)
                    .clickable { onToggle(value) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = label,
                    color = if (selected) Color(0xFF141108) else ZzzColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun DiscountInlineBlock(
    discountConfig: DiscountConfig,
    onDiscountEnabledChange: (Boolean) -> Unit,
    onARankFullChange: (Boolean) -> Unit,
    onStandardSFullChange: (Boolean) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "余波换抽打折",
                    color = ZzzColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (discountConfig.enabled) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val charRet = ZzzGachaCalculator.computeEffectiveReturnRate(false, discountConfig)
                    val discountFold = (1.0 - charRet) * 10.0
                    StatusChip(
                        text = String.format(Locale.US, "约%.1f折", discountFold),
                        color = ZzzColors.Lime,
                    )
                }
            }
            Switch(
                checked = discountConfig.enabled,
                onCheckedChange = onDiscountEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF141108),
                    checkedTrackColor = ZzzColors.Lime,
                ),
            )
        }

        AnimatedVisibility(visible = discountConfig.enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ZzzTogglePill(
                    text = if (discountConfig.isARankFull) "✓ 四星满命" else "四星满命",
                    selected = discountConfig.isARankFull,
                    accent = ZzzColors.Gold,
                    onClick = { onARankFullChange(!discountConfig.isARankFull) },
                    modifier = Modifier.weight(1f),
                )
                ZzzTogglePill(
                    text = if (discountConfig.isStandardSFull) "✓ 常驻S满命" else "常驻S满命",
                    selected = discountConfig.isStandardSFull,
                    accent = ZzzColors.Gold,
                    onClick = { onStandardSFullChange(!discountConfig.isStandardSFull) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SequenceBadgesRow(sequence: List<PullStepType>) {
    val scroll = rememberScrollState()
    var c = 0
    var w = 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        sequence.forEachIndexed { idx, step ->
            val isChar = step == PullStepType.CHARACTER
            val label = if (isChar) {
                val res = "${c}命"
                c++
                res
            } else {
                w++
                "${w}精"
            }
            val color = if (isChar) ZzzColors.Gold else ZzzColors.Cyan
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.16f))
                    .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = label,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (idx < sequence.lastIndex) {
                Text(text = "→", color = ZzzColors.TextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SequenceEditorDialog(
    sequence: List<PullStepType>,
    onSequenceChange: (List<PullStepType>) -> Unit,
    onDismiss: () -> Unit,
) {
    val charCount = sequence.count { it == PullStepType.CHARACTER }
    val weaponCount = sequence.count { it == PullStepType.WEAPON }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZzzColors.Surface,
        title = {
            Text(
                text = "自定义抽卡顺序",
                color = ZzzColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    var cSeen = 0
                    var wSeen = 0
                    sequence.forEachIndexed { idx, step ->
                        val stepLabel = if (step == PullStepType.CHARACTER) {
                            val m = cSeen
                            cSeen++
                            "限定角色 ${m}命"
                        } else {
                            wSeen++
                            "限定专武 ${wSeen}精"
                        }
                        val accent = if (step == PullStepType.CHARACTER) ZzzColors.Gold else ZzzColors.Cyan

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ZzzColors.SurfaceElevated)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${idx + 1}.",
                                color = ZzzColors.TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(26.dp),
                            )
                            Text(
                                text = stepLabel,
                                color = accent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            DialogIconBtn("↑", enabled = idx > 0) {
                                val copy = sequence.toMutableList()
                                val item = copy.removeAt(idx)
                                copy.add(idx - 1, item)
                                onSequenceChange(copy)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            DialogIconBtn("↓", enabled = idx < sequence.size - 1) {
                                val copy = sequence.toMutableList()
                                val item = copy.removeAt(idx)
                                copy.add(idx + 1, item)
                                onSequenceChange(copy)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            DialogIconBtn("✕", enabled = sequence.size > 1, tint = ZzzColors.RedAccent) {
                                val copy = sequence.toMutableList()
                                copy.removeAt(idx)
                                onSequenceChange(copy)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { if (charCount < 7) onSequenceChange(sequence + PullStepType.CHARACTER) },
                        enabled = charCount < 7,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZzzColors.Gold,
                            contentColor = Color(0xFF141108),
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (charCount < 7) "+ 角色 (${charCount}命)" else "角色已满",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Button(
                        onClick = { if (weaponCount < 5) onSequenceChange(sequence + PullStepType.WEAPON) },
                        enabled = weaponCount < 5,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZzzColors.Cyan,
                            contentColor = Color(0xFF041418),
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (weaponCount < 5) "+ 专武 (${weaponCount + 1}精)" else "专武已满",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = ZzzColors.GoldBright, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { onSequenceChange(ZzzGachaCalculator.defaultPullSequence) }) {
                Text("恢复默认", color = ZzzColors.TextSecondary, fontSize = 14.sp)
            }
        },
    )
}

@Composable
private fun CleanOutcomeCard(
    outcome: Mode2ConfigOutcome,
    isMode: Boolean,
) {
    val borderColor = if (isMode) ZzzColors.Gold else ZzzColors.Border
    val bgColor = if (isMode) Color(0xFF261E11) else ZzzColors.SurfaceElevated
    val title = when {
        outcome.charCount == 0 && outcome.weaponCount == 0 -> "未出限定 UP"
        outcome.charCount > 0 && outcome.weaponCount > 0 -> "${outcome.charCount - 1}命 + ${outcome.weaponCount}精"
        outcome.charCount > 0 -> "${outcome.charCount - 1}命 + 0精"
        else -> "无角色 + ${outcome.weaponCount}精"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = if (isMode) ZzzColors.GoldBright else ZzzColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (isMode) {
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusChip(text = "最可能", color = ZzzColors.GoldBright)
                } else if (!outcome.withinThreeSigma) {
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusChip(text = "3σ外", color = ZzzColors.TextMuted)
                }
            }
            Text(
                text = String.format(Locale.US, "%.2f%%", outcome.exactProbability * 100.0),
                color = if (isMode) ZzzColors.GoldBright else ZzzColors.Gold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { outcome.exactProbability.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (isMode) ZzzColors.GoldBright else ZzzColors.Gold,
            trackColor = Color(0xFF0D1017),
        )

        Spacer(modifier = Modifier.height(7.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "恰好停在该配置",
                color = ZzzColors.TextSecondary,
                fontSize = 12.sp,
            )
            Text(
                text = if (outcome.completedSteps == 0) {
                    "基线 100%"
                } else {
                    String.format(Locale.US, "至少达成 %.2f%%", outcome.atLeastProbability * 100.0)
                },
                color = ZzzColors.Cyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SelectTile(
    text: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else ZzzColors.SurfaceElevated)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else ZzzColors.Border,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) accent else ZzzColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NumericResourceField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ZzzColors.Gold,
            unfocusedBorderColor = ZzzColors.Border,
            focusedLabelColor = ZzzColors.GoldBright,
            unfocusedLabelColor = ZzzColors.TextSecondary,
            focusedTextColor = ZzzColors.TextPrimary,
            unfocusedTextColor = ZzzColors.TextPrimary,
            cursorColor = ZzzColors.GoldBright,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    )
}

@Composable
private fun QuickAddChip(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = ZzzColors.TextPrimary,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        border = BorderStroke(1.dp, ZzzColors.BorderBright),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.height(34.dp),
    ) {
        Text(text = text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DialogIconBtn(
    text: String,
    enabled: Boolean,
    tint: Color = ZzzColors.TextPrimary,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(ZzzColors.Surface)
            .border(1.dp, ZzzColors.Border, RoundedCornerShape(7.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) tint else ZzzColors.TextMuted.copy(alpha = 0.35f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun shortTargetLabel(mindscape: Int, refinement: Int): String = when {
    mindscape >= 0 && refinement > 0 -> "${mindscape}命 + ${refinement}精"
    mindscape >= 0 -> "${mindscape}命 + 0精"
    else -> "仅专武 ${refinement}精"
}

private fun filterDigits(input: String, maxLen: Int): String =
    input.filter { it.isDigit() }.take(maxLen)

private fun charsThenWeaponsSequence(): List<PullStepType> = buildList {
    repeat(7) { add(PullStepType.CHARACTER) }
    repeat(5) { add(PullStepType.WEAPON) }
}

private fun twoPlusOneFirstSequence(): List<PullStepType> = buildList {
    add(PullStepType.CHARACTER)
    add(PullStepType.WEAPON)
    add(PullStepType.CHARACTER)
    add(PullStepType.CHARACTER)
    repeat(4) { add(PullStepType.WEAPON) }
    repeat(4) { add(PullStepType.CHARACTER) }
}

private fun onlyCharsSequence(): List<PullStepType> = List(7) { PullStepType.CHARACTER }

private fun onlyWeaponsSequence(): List<PullStepType> = List(5) { PullStepType.WEAPON }
