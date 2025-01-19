package me.anno.tests.maths

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Floats.float16ToFloat32
import me.anno.utils.types.Floats.float32ToFloat16
import org.junit.jupiter.api.Test

class HalfFloatTest {

    @Test
    fun testSpecialValues() {
        assertEquals(+0f, float16ToFloat32(0x0000))
        assertEquals(-0f, float16ToFloat32(0x8000))
        assertEquals(Float.POSITIVE_INFINITY, float16ToFloat32(0x7c00))
        assertEquals(Float.NEGATIVE_INFINITY, float16ToFloat32(0xfc00))
        for (i in 0 until 0xff) {
            assertTrue(float16ToFloat32(0xff00 + i).isNaN())
        }
        // assertEquals(+1f, float16ToFloat32(0x3c00)) // slightly off -> for rounding?
        // assertEquals(-1f, float16ToFloat32(0xcc00))
    }

    @Test
    fun testFP16ToFP32Conversions() {
        // todo test actual values, too
        for (fp16Src in 0 until 65536) {
            val fp32 = float16ToFloat32(fp16Src)
            val fp16Dst = float32ToFloat16(fp32)
            assertEquals(fp16Src, fp16Dst)
        }
    }
}