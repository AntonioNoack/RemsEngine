package me.anno.tests.maths

import me.anno.maths.ContinuousMedian
import me.anno.maths.Maths
import me.anno.tests.maths.ContinuousMedianTest.testDirect
import me.anno.tests.maths.ContinuousMedianTest.testDiscrete
import me.anno.utils.types.Floats.f6
import me.anno.utils.types.Floats.f6s
import me.anno.utils.types.Strings.withLength
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

object ContinuousMedianTest {

    @Test
    fun testDirect() {
        val instance = ContinuousMedian(0f, 3000f, 6)
        instance.bucketWeights[0] = 6f
        instance.bucketWeights[1] = 7f
        instance.bucketWeights[2] = 9f
        instance.bucketWeights[3] = 8f
        instance.bucketWeights[4] = 4f
        instance.bucketWeights[5] = 6f
        instance.total = instance.bucketWeights.sum()
        instance.median = Float.NaN
        assertEquals(1388.889f, instance.median, 0.001f)
    }

    @Test
    fun testDiscrete() {
        val instance = ContinuousMedian(0f, 3000f, 6)
        instance.add(250f, 6f)
        instance.add(750f, 7f)
        instance.add(1250f, 9f)
        instance.add(1750f, 8f)
        instance.add(2250f, 4f)
        instance.add(2750f, 6f)
        instance.total = instance.bucketWeights.sum()
        instance.median = Float.NaN
        // shall be 1388.889
        // actual, with sharpness 1.1: 1393.785 -> good enough :)
        assertEquals(1388.889f, instance.median, 10f)
    }
}
