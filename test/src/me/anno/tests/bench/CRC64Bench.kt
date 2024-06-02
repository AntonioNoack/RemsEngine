package me.anno.tests.bench

import me.anno.utils.Clock
import net.boeckling.crc.CRC64
import kotlin.random.Random

fun main() {
    val random = Random(1234)
    val data = ByteArray(65536) { random.nextInt(256).toByte() }
    Clock("CRC64Bench").benchmark(10, 10000, data.size, "CRC64") {
        CRC64.update(data, 0, data.size, 0L)
    }
}