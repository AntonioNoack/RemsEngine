package me.anno.tests.maths

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Floats.FP16_MAX_VALUE
import me.anno.utils.types.Floats.FP16_MIN_VALUE
import me.anno.utils.types.Floats.FP16_NEGATIVE_INFINITY
import me.anno.utils.types.Floats.FP16_POSITIVE_INFINITY
import me.anno.utils.types.Floats.float16ToFloat32
import me.anno.utils.types.Floats.float32ToFloat16
import org.junit.jupiter.api.Test

class HalfFloatTest {

    @Test
    fun testFP16ToFP32Conversions() {
        for (fp16Src in 0..0xffff) {
            val fp32 = float16ToFloat32(fp16Src)
            val fp16Dst = float32ToFloat16(fp32)
            assertEquals(fp16Src, fp16Dst)
        }
    }

    @Test
    fun testFP16Order() {
        for (i in 1..0x7c00) {
            val v0 = float16ToFloat32(i - 1)
            val v1 = float16ToFloat32(i)
            assertTrue(v0 < v1)
        }
    }

    @Test
    fun testSpecificValues() {
        assertEquals(0x3c00, float32ToFloat16(+1f))
        assertEquals(0xbc00, float32ToFloat16(-1f))
        assertEquals(0x0000, float32ToFloat16(+0f))
        assertEquals(0x8000, float32ToFloat16(-0f))

        val negative = 0x8000
        assertEquals(+5.9604645e-8f, float16ToFloat32(FP16_MIN_VALUE))
        assertEquals(-5.9604645e-8f, float16ToFloat32(FP16_MIN_VALUE or negative))
        assertEquals(+65504f, float16ToFloat32(FP16_MAX_VALUE))
        assertEquals(-65504f, float16ToFloat32(FP16_MAX_VALUE or negative))
        assertEquals(Float.POSITIVE_INFINITY, float16ToFloat32(FP16_POSITIVE_INFINITY))
        assertEquals(Float.NEGATIVE_INFINITY, float16ToFloat32(FP16_NEGATIVE_INFINITY))

        // check all NaNs
        for (i in 0x7c01 until 0x8000) {
            assertTrue(float16ToFloat32(i).isNaN())
            assertTrue(float16ToFloat32(i or 0x8000).isNaN())
        }
    }
}