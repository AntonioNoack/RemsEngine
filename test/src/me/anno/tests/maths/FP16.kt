package me.anno.tests.maths

import me.anno.utils.types.Floats.float16ToFloat32
import me.anno.utils.types.Floats.float32ToFloat16
import kotlin.test.assertEquals

fun main() {
    // works :)
    for (i in 0 until 65536) {
        val fp = float16ToFloat32(i)
        val j = float32ToFloat16(fp)
        assertEquals(i, j)
    }
}