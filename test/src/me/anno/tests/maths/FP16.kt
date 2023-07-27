package me.anno.tests.maths

import me.anno.utils.types.Floats.float16ToFloat32
import me.anno.utils.types.Floats.float32ToFloat16

fun main() {
    // works :)
    for (i in 0 until 65536) {
        val fp = float16ToFloat32(i)
        val j = float32ToFloat16(fp)
        if (i != j) throw IllegalStateException("$i != $j -> $fp")
    }
}