package me.anno.tests.bench

import me.anno.utils.Color.convertABGR2ARGB
import me.anno.utils.Clock
import java.util.*

fun main() {
    // checking the speed, whether the indirect calls make it slower
    // -> no, they don't :3, each turn just takes 0.20 ns (in both cases) -> ~0.8 ticks @ 3.9 GHz
    val rnd = Random()
    val data = IntArray(1024 * 1024) { rnd.nextInt() }
    val clock = Clock()
    clock.benchmark(50, 1000, data.size, "Direct") {
        val agMask = 0xff00ff00.toInt()
        for (i in data.indices) {
            val v = data[i]
            data[i] = v.and(agMask) or v.shr(16).and(0xff) or v.and(0xff).shl(16)
        }
    }
    clock.benchmark(50, 1000, data.size, "Indirect") {
        for (i in data.indices) {
            data[i] = convertABGR2ARGB(data[i])
        }
    }
}