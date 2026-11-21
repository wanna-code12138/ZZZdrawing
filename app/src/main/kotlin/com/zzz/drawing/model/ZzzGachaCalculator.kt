package com.zzz.drawing.model

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 抽取步骤类型：限定S级代理人（角色）或限定S级音擎（武器）。
 */
enum class PullStepType {
    CHARACTER,
    WEAPON,
}

/**
 * 副产物（信号余波）换抽（打折抽卡）配置。
 *
 * @property enabled 是否开启把抽卡副产物信号余波兑换成加密母带继续抽卡
 * @property isARankFull 四星（A级）代理人是否已满命（影响角色/武器池A级产出余波均值）
 * @property isStandardSFull 常驻五星（S级）代理人是否已满命（影响角色池歪常驻时返还余波：未满命40余波=2抽，已满命100余波=5抽）
 */
data class DiscountConfig(
    val enabled: Boolean = false,
    val isARankFull: Boolean = false,
    val isStandardSFull: Boolean = false,
)

/**
 * 分位表中的单行数据（对应截图中的一行）。
 */
data class QuantileTableRow(
    val label: String,
    val mindscape: Int?,
    val refinement: Int,
    val q10: Int,
    val q25: Int,
    val q50: Int,
    val q75: Int,
    val q90: Int,
    val q95: Int,
    val q99: Int,
    val expectedPulls: Double,
    val isSelected: Boolean,
)

/**
 * 模式一（指定配置算所需抽数与分布）计算结果。
 */
data class Mode1Result(
    val targetMindscape: Int,
    val targetRefinement: Int,
    val targetLabel: String,
    val pmf: DoubleArray,
    val cdf: DoubleArray,
    val expectedPulls: Double,
    val stdDev: Double,
    val modePull: Int,
    val peakProb: Double,
    val maxHardPityPulls: Int,
    val q10: Int,
    val q25: Int,
    val q50: Int,
    val q75: Int,
    val q90: Int,
    val q95: Int,
    val q99: Int,
    val discountConfig: DiscountConfig,
    val charEffectiveReturnRate: Double,
    val weaponEffectiveReturnRate: Double,
    val comparisonTableRows: List<QuantileTableRow>,
) {
    fun probabilityAtPull(pull: Int): Double =
        if (pull in pmf.indices) pmf[pull] else 0.0

    fun cumulativeAtPull(pull: Int): Double =
        when {
            pull < 0 -> 0.0
            pull >= cdf.size -> 1.0
            else -> cdf[pull]
        }
}

/**
 * 模式二（给定抽数算能抽出的配置可能性）单项结果。
 */
data class Mode2ConfigOutcome(
    val completedSteps: Int,
    val charCount: Int,
    val weaponCount: Int,
    val label: String,
    val shortLabel: String,
    val exactProbability: Double,
    val atLeastProbability: Double,
    val withinThreeSigma: Boolean,
)

/**
 * 模式二计算结果。
 */
data class Mode2Result(
    val totalPulls: Int,
    val pullsFromPolychromes: Int,
    val remainingPolychromes: Int,
    val pullsFromTapes: Int,
    val pullsFromSignals: Int,
    val remainingSignals: Int,
    val allOutcomes: List<Mode2ConfigOutcome>,
    val threeSigmaOutcomes: List<Mode2ConfigOutcome>,
    val threeSigmaCoveredProbability: Double,
    val expectedCompletedSteps: Double,
    val mostLikelyOutcome: Mode2ConfigOutcome?,
)

/**
 * 《绝区零》抽卡概率与期望核心计算引擎。
 *
 * 严格遵循游戏公示规则及社区精确软保底模型（对标 GGanalysis / 一棵平衡树）：
 * - 限定角色池（独家频段）：基础 0.6%，第74抽起每抽+6%，90抽硬保底；小保底50%概率UP，歪则下次必UP。
 * - 限定武器池（音擎频段）：基础 1.0%，第65抽起每抽+6%，80抽硬保底；小保底75%概率UP，歪则下次必UP。
 * - 副产物信号余波（20余波 = 1加密母带）：
 *   - 角色池A级综合14.4%（A代理人12.16% + A音擎2.24%）：四星未满命每抽返5.76%，满命每抽返13.056%。
 *   - 武器池A级综合18.0%（A音擎16.5% + A代理人1.5%）：四星未满命每抽返7.20%，满命每抽返8.10%。
 *   - S级返还：UP角色0命解锁返0抽，1~6命返2抽；歪常驻角色未满命返2抽，满命返5抽；S级音擎固定返2抽。
 */
object ZzzGachaCalculator {

    const val POLYCHROME_PER_PULL = 160
    const val SIGNAL_PER_PULL = 20

    const val CHAR_BASE_RATE = 0.006
    const val CHAR_SOFT_PITY_START = 74
    const val CHAR_SOFT_PITY_STEP = 0.06
    const val CHAR_HARD_PITY = 90
    const val CHAR_UP_RATE = 0.5

    const val WEAPON_BASE_RATE = 0.01
    const val WEAPON_SOFT_PITY_START = 65
    const val WEAPON_SOFT_PITY_STEP = 0.06
    const val WEAPON_HARD_PITY = 80
    const val WEAPON_UP_RATE = 0.75

    // A级含10连保底综合概率下的每抽信号余波返还折算抽数
    const val CHAR_A_RETURN_NOT_FULL = 0.144 * 0.4 // 0.0576
    const val CHAR_A_RETURN_FULL = 0.1216 * 1.0 + 0.0224 * 0.4 // 0.13056
    const val WEAPON_A_RETURN_NOT_FULL = 0.180 * 0.4 // 0.0720
    const val WEAPON_A_RETURN_FULL = 0.165 * 0.4 + 0.015 * 1.0 // 0.0810

    const val STANDARD_S_RETURN_NOT_FULL = 2.0 // 40余波
    const val STANDARD_S_RETURN_FULL = 5.0 // 100余波
    const val UP_CHAR_C0_RETURN = 0.0
    const val UP_CHAR_DUPE_RETURN = 2.0
    const val WEAPON_S_RETURN = 2.0

    // 3σ（正态分布 ±3σ 覆盖 99.73002%，单侧尾端各 0.13499%）
    const val THREE_SIGMA_TAIL_THRESHOLD = 0.00135

    private val freshCharBase: DoubleArray = buildBaseSDistribution(isWeapon = false, initialPity = 0)
    private val freshWeaponBase: DoubleArray = buildBaseSDistribution(isWeapon = true, initialPity = 0)

    /**
     * 默认模式二抽卡顺序：先抽 0 命 -> 专武(1精) -> 1~6命 -> 2~5精。
     */
    val defaultPullSequence: List<PullStepType> = buildList {
        add(PullStepType.CHARACTER) // 0命
        add(PullStepType.WEAPON)    // 1精
        repeat(6) { add(PullStepType.CHARACTER) } // 1~6命
        repeat(4) { add(PullStepType.WEAPON) }    // 2~5精
    }

    /**
     * 生成单个 S 级物品在指定已垫抽数下的条件出货抽数分布 P(next S on k-th pull)。
     */
    fun buildBaseSDistribution(isWeapon: Boolean, initialPity: Int): DoubleArray {
        val baseRate = if (isWeapon) WEAPON_BASE_RATE else CHAR_BASE_RATE
        val softStart = if (isWeapon) WEAPON_SOFT_PITY_START else CHAR_SOFT_PITY_START
        val stepRate = if (isWeapon) WEAPON_SOFT_PITY_STEP else CHAR_SOFT_PITY_STEP
        val hardPity = if (isWeapon) WEAPON_HARD_PITY else CHAR_HARD_PITY

        val clampedPity = initialPity.coerceIn(0, hardPity - 1)
        val remainingMax = hardPity - clampedPity
        val dist = DoubleArray(remainingMax + 1)
        var survival = 1.0

        for (k in 1..remainingMax) {
            val pityIndex = clampedPity + k
            val p = when {
                pityIndex < softStart -> baseRate
                pityIndex < hardPity -> min(1.0, baseRate + (pityIndex - softStart + 1) * stepRate)
                else -> 1.0
            }
            dist[k] = survival * p
            survival *= (1.0 - p)
        }
        return dist
    }

    /**
     * 生成单个限定 UP 目标（角色或专武）所需净抽数的概率质量分布。
     */
    fun buildUpStepDistribution(
        stepType: PullStepType,
        initialPity: Int,
        isGuaranteed: Boolean,
        isFirstCharacterCopy: Boolean,
        discountConfig: DiscountConfig,
    ): DoubleArray {
        val isWeapon = stepType == PullStepType.WEAPON
        val upRate = if (isWeapon) WEAPON_UP_RATE else CHAR_UP_RATE
        val firstBase = buildBaseSDistribution(isWeapon = isWeapon, initialPity = initialPity)
        val freshBase = if (isWeapon) freshWeaponBase else freshCharBase

        val winRaw = DoubleArray(firstBase.size)
        val loseRaw = if (isGuaranteed) DoubleArray(0) else convolve(firstBase, freshBase)

        val winWeight = if (isGuaranteed) 1.0 else upRate
        val loseWeight = if (isGuaranteed) 0.0 else (1.0 - upRate)

        for (i in firstBase.indices) {
            winRaw[i] = firstBase[i] * winWeight
        }
        for (i in loseRaw.indices) {
            loseRaw[i] *= loseWeight
        }

        if (!discountConfig.enabled) {
            return addDistributions(winRaw, loseRaw)
        }

        val aReturnRate = when {
            isWeapon && discountConfig.isARankFull -> WEAPON_A_RETURN_FULL
            isWeapon -> WEAPON_A_RETURN_NOT_FULL
            discountConfig.isARankFull -> CHAR_A_RETURN_FULL
            else -> CHAR_A_RETURN_NOT_FULL
        }
        val scale = 1.0 - aReturnRate

        val upRefund = when {
            isWeapon -> WEAPON_S_RETURN
            isFirstCharacterCopy -> UP_CHAR_C0_RETURN
            else -> UP_CHAR_DUPE_RETURN
        }
        val lossRefund = when {
            isWeapon -> WEAPON_S_RETURN
            discountConfig.isStandardSFull -> STANDARD_S_RETURN_FULL
            else -> STANDARD_S_RETURN_NOT_FULL
        }

        val winNet = applySmoothDiscount(winRaw, scale = scale, fixedRefund = upRefund)
        val loseNet = if (isGuaranteed) {
            DoubleArray(0)
        } else {
            applySmoothDiscount(loseRaw, scale = scale, fixedRefund = upRefund + lossRefund)
        }
        return addDistributions(winNet, loseNet)
    }

    /**
     * 计算指定角色或武器池在给定配置下的综合净返还比例（仅用于展示参考折扣率）。
     */
    fun computeEffectiveReturnRate(isWeapon: Boolean, discountConfig: DiscountConfig): Double {
        if (!discountConfig.enabled) return 0.0
        val aRate = when {
            isWeapon && discountConfig.isARankFull -> WEAPON_A_RETURN_FULL
            isWeapon -> WEAPON_A_RETURN_NOT_FULL
            discountConfig.isARankFull -> CHAR_A_RETURN_FULL
            else -> CHAR_A_RETURN_NOT_FULL
        }
        val rawUpExpected = if (isWeapon) 62.3666 else 93.4460
        val sRefundExpected = if (isWeapon) {
            WEAPON_S_RETURN * 1.25
        } else {
            val stdRefund = if (discountConfig.isStandardSFull) STANDARD_S_RETURN_FULL else STANDARD_S_RETURN_NOT_FULL
            0.5 * stdRefund + UP_CHAR_DUPE_RETURN * 0.5 // 仅作综合参考（0命不返、命座返2）
        }
        return (aRate + sRefundExpected / rawUpExpected).coerceIn(0.0, 0.5)
    }

    /**
     * 平滑线性插值折算副产物母带返还：
     * 原始消耗 k 抽时，经 A 级返还率缩减与 S 级固定余波返还后，净消耗初始抽数 x = max(1.0, k * scale - fixedRefund)。
     * 将概率质量按比例分配到相邻整数抽数，既保持数学期望精确一致，又消除离散取整锯齿。
     */
    internal fun applySmoothDiscount(
        rawDist: DoubleArray,
        scale: Double,
        fixedRefund: Double,
    ): DoubleArray {
        if (rawDist.isEmpty()) return DoubleArray(0)
        val maxRaw = rawDist.size - 1
        val maxNet = max(2, ceil(maxRaw * scale).toInt() + 2)
        val net = DoubleArray(maxNet)
        for (k in 1 until rawDist.size) {
            val prob = rawDist[k]
            if (prob <= 0.0) continue
            val x = max(1.0, k * scale - fixedRefund)
            val lo = floor(x).toInt().coerceIn(1, maxNet - 1)
            val hi = lo + 1
            val frac = x - lo
            net[lo] += prob * (1.0 - frac)
            if (hi < maxNet) {
                net[hi] += prob * frac
            } else {
                net[lo] += prob * frac
            }
        }
        return net
    }

    /**
     * 模式一计算：输入目标配置 (x 命 y 精炼，其中 x = -1 表示不抽角色，y = 0 表示不抽武器)，
     * 返回完整概率分布、分位数值、期望与标准差，以及对比分位表。
     */
    fun calculateMode1(
        targetMindscape: Int,
        targetRefinement: Int,
        charPity: Int = 0,
        charGuaranteed: Boolean = false,
        weaponPity: Int = 0,
        weaponGuaranteed: Boolean = false,
        discountConfig: DiscountConfig = DiscountConfig(),
    ): Mode1Result {
        val clampedMindscape = targetMindscape.coerceIn(-1, 6)
        val clampedRefinement = targetRefinement.coerceIn(0, 5)
        val charCopies = if (clampedMindscape >= 0) clampedMindscape + 1 else 0
        val weaponCopies = clampedRefinement

        val pmf = computeComboPmf(
            charCopies = charCopies,
            weaponCopies = weaponCopies,
            charPity = charPity,
            charGuaranteed = charGuaranteed,
            weaponPity = weaponPity,
            weaponGuaranteed = weaponGuaranteed,
            discountConfig = discountConfig,
        )

        val cdf = buildCdf(pmf)
        val stats = computeStats(pmf)
        val maxHardPity = computeHardPityUpperBound(
            charCopies = charCopies,
            weaponCopies = weaponCopies,
            charPity = charPity,
            charGuaranteed = charGuaranteed,
            weaponPity = weaponPity,
            weaponGuaranteed = weaponGuaranteed,
        )

        val comparisonRows = if (clampedMindscape >= 0) {
            (0..6).map { m ->
                val rowPmf = if (m == clampedMindscape) {
                    pmf
                } else {
                    computeComboPmf(
                        charCopies = m + 1,
                        weaponCopies = weaponCopies,
                        charPity = charPity,
                        charGuaranteed = charGuaranteed,
                        weaponPity = weaponPity,
                        weaponGuaranteed = weaponGuaranteed,
                        discountConfig = discountConfig,
                    )
                }
                val rowCdf = buildCdf(rowPmf)
                val rowExp = computeExpected(rowPmf)
                QuantileTableRow(
                    label = "影画$m ${clampedRefinement}星",
                    mindscape = m,
                    refinement = clampedRefinement,
                    q10 = quantile(rowCdf, 0.10),
                    q25 = quantile(rowCdf, 0.25),
                    q50 = quantile(rowCdf, 0.50),
                    q75 = quantile(rowCdf, 0.75),
                    q90 = quantile(rowCdf, 0.90),
                    q95 = quantile(rowCdf, 0.95),
                    q99 = quantile(rowCdf, 0.99),
                    expectedPulls = rowExp,
                    isSelected = (m == clampedMindscape),
                )
            }
        } else {
            val activeRange = if (clampedRefinement > 0) (1..5) else (1..5)
            activeRange.map { r ->
                val rowPmf = if (r == clampedRefinement) {
                    pmf
                } else {
                    computeComboPmf(
                        charCopies = 0,
                        weaponCopies = r,
                        charPity = charPity,
                        charGuaranteed = charGuaranteed,
                        weaponPity = weaponPity,
                        weaponGuaranteed = weaponGuaranteed,
                        discountConfig = discountConfig,
                    )
                }
                val rowCdf = buildCdf(rowPmf)
                val rowExp = computeExpected(rowPmf)
                QuantileTableRow(
                    label = "仅音擎 ${r}星",
                    mindscape = null,
                    refinement = r,
                    q10 = quantile(rowCdf, 0.10),
                    q25 = quantile(rowCdf, 0.25),
                    q50 = quantile(rowCdf, 0.50),
                    q75 = quantile(rowCdf, 0.75),
                    q90 = quantile(rowCdf, 0.90),
                    q95 = quantile(rowCdf, 0.95),
                    q99 = quantile(rowCdf, 0.99),
                    expectedPulls = rowExp,
                    isSelected = (r == clampedRefinement),
                )
            }
        }

        return Mode1Result(
            targetMindscape = clampedMindscape,
            targetRefinement = clampedRefinement,
            targetLabel = formatTargetLabel(clampedMindscape, clampedRefinement),
            pmf = pmf,
            cdf = cdf,
            expectedPulls = stats.mean,
            stdDev = stats.stdDev,
            modePull = stats.modePull,
            peakProb = stats.peakProb,
            maxHardPityPulls = maxHardPity,
            q10 = quantile(cdf, 0.10),
            q25 = quantile(cdf, 0.25),
            q50 = quantile(cdf, 0.50),
            q75 = quantile(cdf, 0.75),
            q90 = quantile(cdf, 0.90),
            q95 = quantile(cdf, 0.95),
            q99 = quantile(cdf, 0.99),
            discountConfig = discountConfig,
            charEffectiveReturnRate = computeEffectiveReturnRate(isWeapon = false, discountConfig = discountConfig),
            weaponEffectiveReturnRate = computeEffectiveReturnRate(isWeapon = true, discountConfig = discountConfig),
            comparisonTableRows = comparisonRows,
        )
    }

    /**
     * 模式二计算：给定现有菲林、加密母带、现有余波、垫抽、保底、打折设置与抽取顺序，
     * 返回各阶段配置达成概率，并按 3 西格玛 (99.73%) 区间过滤极小概率长尾项。
     */
    fun calculateMode2(
        polychromes: Int,
        encryptedTapes: Int,
        residualSignals: Int = 0,
        charPity: Int = 0,
        charGuaranteed: Boolean = false,
        weaponPity: Int = 0,
        weaponGuaranteed: Boolean = false,
        discountConfig: DiscountConfig = DiscountConfig(),
        pullSequence: List<PullStepType> = defaultPullSequence,
    ): Mode2Result {
        val safePolychromes = max(0, polychromes)
        val safeTapes = max(0, encryptedTapes)
        val safeSignals = max(0, residualSignals)

        val pullsFromPoly = safePolychromes / POLYCHROME_PER_PULL
        val remPoly = safePolychromes % POLYCHROME_PER_PULL
        val pullsFromSig = safeSignals / SIGNAL_PER_PULL
        val remSig = safeSignals % SIGNAL_PER_PULL
        val totalPulls = safeTapes + pullsFromPoly + pullsFromSig

        val effectiveSequence = pullSequence.ifEmpty { defaultPullSequence }
        var currentPmf = doubleArrayOf(1.0)
        var charsSeen = 0
        var weaponsSeen = 0

        val stepCount = effectiveSequence.size
        val atLeastProbs = DoubleArray(stepCount + 1)
        atLeastProbs[0] = 1.0

        val stepCountsSnapshot = ArrayList<Pair<Int, Int>>(stepCount + 1)
        stepCountsSnapshot.add(0 to 0)

        for ((index, step) in effectiveSequence.withIndex()) {
            val stepDist = when (step) {
                PullStepType.CHARACTER -> {
                    val isFirst = (charsSeen == 0)
                    charsSeen++
                    buildUpStepDistribution(
                        stepType = PullStepType.CHARACTER,
                        initialPity = if (isFirst) charPity else 0,
                        isGuaranteed = if (isFirst) charGuaranteed else false,
                        isFirstCharacterCopy = isFirst,
                        discountConfig = discountConfig,
                    )
                }
                PullStepType.WEAPON -> {
                    val isFirst = (weaponsSeen == 0)
                    weaponsSeen++
                    buildUpStepDistribution(
                        stepType = PullStepType.WEAPON,
                        initialPity = if (isFirst) weaponPity else 0,
                        isGuaranteed = if (isFirst) weaponGuaranteed else false,
                        isFirstCharacterCopy = false,
                        discountConfig = discountConfig,
                    )
                }
            }
            currentPmf = convolve(currentPmf, stepDist)
            stepCountsSnapshot.add(charsSeen to weaponsSeen)
            atLeastProbs[index + 1] = sumPrefix(currentPmf, totalPulls).coerceIn(0.0, atLeastProbs[index])
        }

        val exactProbs = DoubleArray(stepCount + 1)
        for (m in 0..stepCount) {
            val next = if (m + 1 <= stepCount) atLeastProbs[m + 1] else 0.0
            exactProbs[m] = max(0.0, atLeastProbs[m] - next)
        }

        var cumulativeStart = 0.0
        val rawOutcomes = ArrayList<Mode2ConfigOutcome>(stepCount + 1)
        var expectedSteps = 0.0

        for (m in 0..stepCount) {
            val pExact = exactProbs[m]
            val pAtLeast = atLeastProbs[m]
            val cumEnd = cumulativeStart + pExact
            val (cCount, wCount) = stepCountsSnapshot[m]
            val (fullLabel, shortLabel) = formatStepSnapshotLabel(cCount, wCount)

            val inThreeSigma = pExact >= THREE_SIGMA_TAIL_THRESHOLD &&
                cumEnd > THREE_SIGMA_TAIL_THRESHOLD &&
                cumulativeStart < (1.0 - THREE_SIGMA_TAIL_THRESHOLD)

            expectedSteps += m * pExact
            rawOutcomes.add(
                Mode2ConfigOutcome(
                    completedSteps = m,
                    charCount = cCount,
                    weaponCount = wCount,
                    label = fullLabel,
                    shortLabel = shortLabel,
                    exactProbability = pExact,
                    atLeastProbability = pAtLeast,
                    withinThreeSigma = inThreeSigma,
                )
            )
            cumulativeStart = cumEnd
        }

        val mostLikely = rawOutcomes.maxByOrNull { it.exactProbability }
        val filtered = rawOutcomes.filter { it.withinThreeSigma }.ifEmpty {
            listOfNotNull(mostLikely)
        }
        val coveredProb = filtered.sumOf { it.exactProbability }.coerceIn(0.0, 1.0)

        return Mode2Result(
            totalPulls = totalPulls,
            pullsFromPolychromes = pullsFromPoly,
            remainingPolychromes = remPoly,
            pullsFromTapes = safeTapes,
            pullsFromSignals = pullsFromSig,
            remainingSignals = remSig,
            allOutcomes = rawOutcomes,
            threeSigmaOutcomes = filtered,
            threeSigmaCoveredProbability = coveredProb,
            expectedCompletedSteps = expectedSteps,
            mostLikelyOutcome = mostLikely,
        )
    }

    /**
     * 格式化目标配置文案（如 `0命 + 1精 (需1角色+1专武)`）。
     */
    fun formatTargetLabel(mindscape: Int, refinement: Int): String {
        val charPart = if (mindscape >= 0) "${mindscape}命" else "不抽角色"
        val weapPart = if (refinement > 0) "${refinement}精" else "不抽专武"
        return when {
            mindscape >= 0 && refinement > 0 -> "${charPart} ${weapPart}（${mindscape + 1}角色+${refinement}专武）"
            mindscape >= 0 -> "${charPart}（${mindscape + 1}角色，无专武）"
            refinement > 0 -> "仅专武 ${weapPart}（${refinement}把音擎）"
            else -> "未选择抽取目标"
        }
    }

    private fun formatStepSnapshotLabel(charCount: Int, weaponCount: Int): Pair<String, String> {
        if (charCount == 0 && weaponCount == 0) {
            return "未抽到任何限定UP" to "未出UP"
        }
        val charText = if (charCount > 0) "${charCount - 1}命" else "无角色"
        val weaponText = "${weaponCount}精"
        val full = when {
            charCount > 0 && weaponCount > 0 -> "$charText + $weaponText（${charCount}金角色+${weaponCount}金音擎）"
            charCount > 0 -> "$charText + 0精（仅${charCount}金角色）"
            else -> "无角色 + $weaponText（仅${weaponCount}金音擎）"
        }
        val short = if (charCount > 0) "${charCount - 1}+${weaponCount}" else "0角色+${weaponCount}精"
        return full to short
    }

    private fun computeComboPmf(
        charCopies: Int,
        weaponCopies: Int,
        charPity: Int,
        charGuaranteed: Boolean,
        weaponPity: Int,
        weaponGuaranteed: Boolean,
        discountConfig: DiscountConfig,
    ): DoubleArray {
        if (charCopies == 0 && weaponCopies == 0) {
            return doubleArrayOf(1.0)
        }
        var combined = doubleArrayOf(1.0)

        if (charCopies > 0) {
            val firstChar = buildUpStepDistribution(
                stepType = PullStepType.CHARACTER,
                initialPity = charPity,
                isGuaranteed = charGuaranteed,
                isFirstCharacterCopy = true,
                discountConfig = discountConfig,
            )
            combined = convolve(combined, firstChar)

            if (charCopies > 1) {
                val dupeChar = buildUpStepDistribution(
                    stepType = PullStepType.CHARACTER,
                    initialPity = 0,
                    isGuaranteed = false,
                    isFirstCharacterCopy = false,
                    discountConfig = discountConfig,
                )
                repeat(charCopies - 1) {
                    combined = convolve(combined, dupeChar)
                }
            }
        }

        if (weaponCopies > 0) {
            val firstWeapon = buildUpStepDistribution(
                stepType = PullStepType.WEAPON,
                initialPity = weaponPity,
                isGuaranteed = weaponGuaranteed,
                isFirstCharacterCopy = false,
                discountConfig = discountConfig,
            )
            combined = convolve(combined, firstWeapon)

            if (weaponCopies > 1) {
                val dupeWeapon = buildUpStepDistribution(
                    stepType = PullStepType.WEAPON,
                    initialPity = 0,
                    isGuaranteed = false,
                    isFirstCharacterCopy = false,
                    discountConfig = discountConfig,
                )
                repeat(weaponCopies - 1) {
                    combined = convolve(combined, dupeWeapon)
                }
            }
        }

        return normalizeTail(combined)
    }

    private fun computeHardPityUpperBound(
        charCopies: Int,
        weaponCopies: Int,
        charPity: Int,
        charGuaranteed: Boolean,
        weaponPity: Int,
        weaponGuaranteed: Boolean,
    ): Int {
        var total = 0
        if (charCopies > 0) {
            val firstMax = (CHAR_HARD_PITY - charPity.coerceIn(0, CHAR_HARD_PITY - 1)) +
                if (charGuaranteed) 0 else CHAR_HARD_PITY
            total += firstMax + (charCopies - 1) * (2 * CHAR_HARD_PITY)
        }
        if (weaponCopies > 0) {
            val firstMax = (WEAPON_HARD_PITY - weaponPity.coerceIn(0, WEAPON_HARD_PITY - 1)) +
                if (weaponGuaranteed) 0 else WEAPON_HARD_PITY
            total += firstMax + (weaponCopies - 1) * (2 * WEAPON_HARD_PITY)
        }
        return total
    }

    private fun convolve(a: DoubleArray, b: DoubleArray): DoubleArray {
        if (a.isEmpty() || b.isEmpty()) return DoubleArray(0)
        val out = DoubleArray(a.size + b.size - 1)
        for (i in a.indices) {
            val ai = a[i]
            if (ai == 0.0) continue
            for (j in b.indices) {
                out[i + j] += ai * b[j]
            }
        }
        return trimTrailingZeros(out)
    }

    private fun addDistributions(a: DoubleArray, b: DoubleArray): DoubleArray {
        val len = max(a.size, b.size)
        val out = DoubleArray(len)
        for (i in a.indices) out[i] += a[i]
        for (i in b.indices) out[i] += b[i]
        return trimTrailingZeros(out)
    }

    private fun trimTrailingZeros(arr: DoubleArray): DoubleArray {
        var last = arr.size - 1
        while (last > 0 && arr[last] < 1e-15) {
            last--
        }
        return if (last == arr.size - 1) arr else arr.copyOf(last + 1)
    }

    private fun normalizeTail(arr: DoubleArray): DoubleArray {
        val sum = arr.sum()
        if (sum <= 0.0) return arr
        val out = arr.copyOf()
        if (kotlin.math.abs(sum - 1.0) > 1e-12) {
            for (i in out.indices) {
                out[i] /= sum
            }
        }
        return out
    }

    private fun buildCdf(pmf: DoubleArray): DoubleArray {
        val cdf = DoubleArray(pmf.size)
        var acc = 0.0
        for (i in pmf.indices) {
            acc += pmf[i]
            cdf[i] = min(1.0, acc)
        }
        return cdf
    }

    private fun sumPrefix(pmf: DoubleArray, maxIndex: Int): Double {
        if (maxIndex < 0) return 0.0
        val end = min(maxIndex, pmf.size - 1)
        var sum = 0.0
        for (i in 0..end) {
            sum += pmf[i]
        }
        return min(1.0, sum)
    }

    private fun quantile(cdf: DoubleArray, q: Double): Int {
        for (i in cdf.indices) {
            if (cdf[i] >= q - 1e-12) return i
        }
        return cdf.size - 1
    }

    private fun computeExpected(pmf: DoubleArray): Double {
        var sum = 0.0
        for (i in pmf.indices) {
            sum += i * pmf[i]
        }
        return sum
    }

    private data class PmfStats(
        val mean: Double,
        val stdDev: Double,
        val modePull: Int,
        val peakProb: Double,
    )

    private fun computeStats(pmf: DoubleArray): PmfStats {
        val mean = computeExpected(pmf)
        var variance = 0.0
        var modeIdx = 0
        var peak = -1.0
        for (i in pmf.indices) {
            val p = pmf[i]
            val diff = i - mean
            variance += diff * diff * p
            if (p > peak) {
                peak = p
                modeIdx = i
            }
        }
        return PmfStats(
            mean = mean,
            stdDev = sqrt(max(0.0, variance)),
            modePull = modeIdx,
            peakProb = max(0.0, peak),
        )
    }
}
