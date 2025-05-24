package me.anno.tests.utils.structures.lists

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.callbacks.VtoD
import me.anno.utils.structures.lists.SegmentLists.segmentDistance
import me.anno.utils.structures.lists.SegmentLists.segmentStep
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.sign

class SegmentListTests {

    private val e = 1e-14
    private val denyRequest = { 0 }
    private val getSegmentLength = VtoD<Double> { it }

    private fun testSegments(
        endIndex: Double, index: Double, delta: Double, segments: List<Double>,
        requestStartSegments: () -> Int, requestEndSegments: () -> Int,
        clampOutput: Boolean
    ) {
        var offset = 0
        val stepped = segmentStep(
            index, delta, segments,
            getSegmentLength, {
                val added = requestStartSegments()
                offset += added
                added
            }, requestEndSegments, clampOutput
        )
        assertEquals(offset + endIndex, stepped, e)
        val distance = segmentDistance(
            offset + index, offset + endIndex, segments,
            getSegmentLength, requestStartSegments, requestEndSegments,
        )
        if (clampOutput) {
            assertTrue(abs(delta) >= abs(distance) && sign(delta) == sign(distance))
        } else {
            assertEquals(delta, distance, e)
        }
    }

    private fun testSegments(
        endIndex: Double, index: Double, delta: Double, segments: List<Double>,
        clamped: Boolean = false
    ) {
        testSegments(
            endIndex, index, delta, segments,
            denyRequest, denyRequest, clamped
        )
    }

    @Test
    fun testSimpleStepsForward() {
        testSegments(0.6, 0.1, 1.5, listOf(3.0))
        testSegments(1.6, 1.1, 1.5, listOf(Double.NaN, 3.0, Double.NaN))
    }

    @Test
    fun testSimpleStepsBackward() {
        testSegments(0.1, 0.6, -1.5, listOf(3.0))
        testSegments(1.1, 1.6, -1.5, listOf(Double.NaN, 3.0, Double.NaN))
    }

    private val start = 0.2
    private val end = 0.3
    private val len0 = 1.0
    private val len1 = 2.0
    private val len2 = 3.0
    private val lengths = listOf(len0, len1, len2)

    @Test
    fun testMultiStepForwardMiddle() {
        val delta = (1.0 - start) * len0 + len1 + end * len2
        testSegments(2.0 + end, start, delta, lengths)
    }

    @Test
    fun testMultiStepBackwardMiddle() {
        val delta = (1.0 - start) * len0 + len1 + end * len2
        testSegments(start, 2.0 + end, -delta, lengths)
    }

    @Test
    fun testMultiStepForwardClamping() {
        val extra = 20.0
        val delta = (1.0 - start) * len0 + len1 + len2 + extra
        testSegments(3.0, start, delta, lengths, true)
        testSegments(3.0 + extra / len2, start, delta, lengths, false)

        // start from beyond the start...
        val extra2 = 10.0
        val delta2 = extra2 + len0 + len1 + len2 + extra
        testSegments(3.0, -extra2, delta2, lengths, true)
        testSegments(3.0 + extra / len2, -extra2, delta2, lengths, false)
    }

    @Test
    fun testMultiStepBackwardClamping() {
        val extra = 20.0
        val delta = extra + len0 + len1 + len2 * end
        testSegments(0.0, 2.0 + end, -delta, lengths, true)
        testSegments(0.0 - extra / len0, 2.0 + end, -delta, lengths, false)

        // start from beyond the end...
        val extra2 = 10.0
        val delta2 = extra + len0 + len1 + len2 + extra2
        testSegments(0.0, 3.0 + extra2 / len2, -delta2, lengths, true)
        testSegments(0.0 - extra / len0, 3.0 + extra2 / len2, -delta2, lengths, false)
    }

    @Test
    fun testDynamicMultiStepForward() {
        val lengths = arrayListOf(1.0)
        testSegments(
            10.0 + 0.5 / 1024.0, 0.5, 1023.0, lengths,
            denyRequest, {
                lengths.add(lengths.last() * 2.0); 1
            }, false
        )
        assertEquals(11, lengths.size)
    }

    @Test
    fun testDynamicMultiStepBackward() {
        val lengths = arrayListOf(1.0)
        testSegments(
            -9.0 - 0.5 / 1024.0, 0.5, -1023.0, lengths, {
                lengths.add(0, lengths.first() * 2.0); 1
            }, denyRequest, false
        )
        assertEquals(11, lengths.size)
    }
}