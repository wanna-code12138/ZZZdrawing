package com.zzz.drawing.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ZzzGachaCalculatorTest {

    @Test
    fun mode1_matches_OneBST_reference_table_exactly() {
        val expectedRows = listOf(
            // (mindscape, q10, q25, q50, q75, q90, q95, q99, expected)
            Triple(0, listOf(85, 117, 150, 197, 225, 240, 291), 155.81),
            Triple(1, listOf(160, 201, 247, 299, 341, 370, 408), 249.26),
            Triple(2, listOf(237, 287, 342, 397, 451, 477, 531), 342.70),
            Triple(3, listOf(317, 373, 436, 499, 555, 591, 651), 436.15),
            Triple(4, listOf(398, 460, 529, 600, 662, 698, 765), 529.60),
            Triple(5, listOf(480, 547, 623, 698, 766, 806, 879), 623.04),
            Triple(6, listOf(563, 635, 716, 797, 870, 913, 992), 716.49),
        )

        val result = ZzzGachaCalculator.calculateMode1(
            targetMindscape = 0,
            targetRefinement = 1,
        )

        assertEquals(1.0, result.pmf.sum(), 1e-6)
        assertEquals(7, result.comparisonTableRows.size)

        for ((index, expected) in expectedRows.withIndex()) {
            val (mindscape, quantiles, expPulls) = expected
            val row = result.comparisonTableRows[index]
            assertEquals("Row $mindscape mindscape", mindscape, row.mindscape)
            assertEquals("Row $mindscape q10", quantiles[0], row.q10)
            assertEquals("Row $mindscape q25", quantiles[1], row.q25)
            assertEquals("Row $mindscape q50", quantiles[2], row.q50)
            assertEquals("Row $mindscape q75", quantiles[3], row.q75)
            assertEquals("Row $mindscape q90", quantiles[4], row.q90)
            assertEquals("Row $mindscape q95", quantiles[5], row.q95)
            assertEquals("Row $mindscape q99", quantiles[6], row.q99)
            assertEquals("Row $mindscape expected", expPulls, row.expectedPulls, 0.015)
        }
    }

    @Test
    fun mode1_single_char_and_single_weapon_expectations_match_theory() {
        val c0Only = ZzzGachaCalculator.calculateMode1(targetMindscape = 0, targetRefinement = 0)
        assertEquals(93.45, c0Only.expectedPulls, 0.02)

        val w1Only = ZzzGachaCalculator.calculateMode1(targetMindscape = -1, targetRefinement = 1)
        assertEquals(62.37, w1Only.expectedPulls, 0.02)
        assertEquals(5, w1Only.comparisonTableRows.size)
    }

    @Test
    fun mode1_pity_and_guarantee_reduce_required_pulls_accurately() {
        val guaranteedAt89 = ZzzGachaCalculator.calculateMode1(
            targetMindscape = 0,
            targetRefinement = 0,
            charPity = 89,
            charGuaranteed = true,
        )
        assertEquals(1.0, guaranteedAt89.expectedPulls, 1e-6)
        assertEquals(1, guaranteedAt89.q99)
        assertEquals(1.0, guaranteedAt89.probabilityAtPull(1), 1e-6)

        val weaponGuaranteedAt79 = ZzzGachaCalculator.calculateMode1(
            targetMindscape = -1,
            targetRefinement = 1,
            weaponPity = 79,
            weaponGuaranteed = true,
        )
        assertEquals(1.0, weaponGuaranteedAt79.expectedPulls, 1e-6)
        assertEquals(1, weaponGuaranteedAt79.q99)
    }

    @Test
    fun mode1_discount_reduces_expected_pulls_in_valid_order() {
        val noDisc = ZzzGachaCalculator.calculateMode1(
            targetMindscape = 2,
            targetRefinement = 1,
            discountConfig = DiscountConfig(enabled = false),
        )
        val partialDisc = ZzzGachaCalculator.calculateMode1(
            targetMindscape = 2,
            targetRefinement = 1,
            discountConfig = DiscountConfig(enabled = true, isARankFull = false, isStandardSFull = false),
        )
        val aFullDisc = ZzzGachaCalculator.calculateMode1(
            targetMindscape = 2,
            targetRefinement = 1,
            discountConfig = DiscountConfig(enabled = true, isARankFull = true, isStandardSFull = false),
        )
        val allFullDisc = ZzzGachaCalculator.calculateMode1(
            targetMindscape = 2,
            targetRefinement = 1,
            discountConfig = DiscountConfig(enabled = true, isARankFull = true, isStandardSFull = true),
        )

        assertEquals(1.0, partialDisc.pmf.sum(), 1e-6)
        assertEquals(1.0, aFullDisc.pmf.sum(), 1e-6)
        assertEquals(1.0, allFullDisc.pmf.sum(), 1e-6)

        assertTrue(partialDisc.expectedPulls < noDisc.expectedPulls)
        assertTrue(aFullDisc.expectedPulls < partialDisc.expectedPulls)
        assertTrue(allFullDisc.expectedPulls < aFullDisc.expectedPulls)

        // Check analytical C0 discount expectation: 93.446*(1 - 0.13056) - 0.5*2 = 80.246
        val c0FullA = ZzzGachaCalculator.calculateMode1(
            targetMindscape = 0,
            targetRefinement = 0,
            discountConfig = DiscountConfig(enabled = true, isARankFull = true, isStandardSFull = false),
        )
        assertTrue(abs(c0FullA.expectedPulls - 80.25) < 0.1)
    }

    @Test
    fun mode2_converts_polychromes_tapes_and_signals_and_filters_three_sigma() {
        val result = ZzzGachaCalculator.calculateMode2(
            polychromes = 16050,
            encryptedTapes = 20,
            residualSignals = 45,
        )

        // 16050 / 160 = 100 rem 50; 45 / 20 = 2 rem 5; total = 100 + 20 + 2 = 122
        assertEquals(100, result.pullsFromPolychromes)
        assertEquals(50, result.remainingPolychromes)
        assertEquals(20, result.pullsFromTapes)
        assertEquals(2, result.pullsFromSignals)
        assertEquals(5, result.remainingSignals)
        assertEquals(122, result.totalPulls)

        val totalProb = result.allOutcomes.sumOf { it.exactProbability }
        assertEquals(1.0, totalProb, 1e-6)

        for (item in result.threeSigmaOutcomes) {
            assertTrue(item.withinThreeSigma)
            assertTrue(item.exactProbability >= ZzzGachaCalculator.THREE_SIGMA_TAIL_THRESHOLD)
        }
        assertTrue(result.threeSigmaCoveredProbability >= 0.99)

        // Ensure at-least probability is monotonically non-increasing
        for (i in 1 until result.allOutcomes.size) {
            assertTrue(result.allOutcomes[i].atLeastProbability <= result.allOutcomes[i - 1].atLeastProbability + 1e-12)
        }
    }

    @Test
    fun mode2_custom_sequence_is_respected() {
        val weaponFirstSeq = listOf(PullStepType.WEAPON, PullStepType.CHARACTER, PullStepType.CHARACTER)
        val result = ZzzGachaCalculator.calculateMode2(
            polychromes = 0,
            encryptedTapes = 80,
            weaponPity = 79,
            weaponGuaranteed = true,
            pullSequence = weaponFirstSeq,
        )

        // Step 1 (weapon guaranteed at 79 pity) takes 1 pull -> completed >= 1 step is 100%
        assertEquals(0.0, result.allOutcomes[0].exactProbability, 1e-9)
        assertEquals(1.0, result.allOutcomes[1].atLeastProbability, 1e-9)
        assertEquals(0, result.allOutcomes[1].charCount)
        assertEquals(1, result.allOutcomes[1].weaponCount)
    }
}
