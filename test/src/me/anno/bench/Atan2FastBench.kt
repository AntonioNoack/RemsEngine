package me.anno.bench

import com.bulletphysics.linearmath.ScalarUtil.atan2Fast
import me.anno.maths.Maths.posMod
import me.anno.maths.Maths.sq
import me.anno.utils.Clock
import org.apache.logging.log4j.LogManager
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.random.Random

private val LOGGER = LogManager.getLogger("Atan2Fast-Bench")

/**
 * check how fast and accurate ScalarUtil.atan2Fast actually is
 * -> pretty inaccurate, but it's also 15x faster
 * */
fun main() {
    val rnd = Random(1324)
    val data = FloatArray(512) { rnd.nextFloat() * 2f - 1f }
    var err = 0.0
    val validations = 10000
    for (i in 0 until validations) {
        val idx = (i % (data.size shr 1)) * 2
        val x = data[idx]
        val y = data[idx + 1]
        err += sq(atan2(y, x) - atan2Fast(y, x))
    }
    err = sqrt(err / validations)
    LOGGER.info("Err: $err") // 0.05 :/, ok-ish

    val clock = Clock(LOGGER)
    clock.benchmark(100_000, 10_000_000, "Standard") { i -> // 45 ns/e
        val idx = posMod(i, (data.size shr 1)) * 2
        val x = data[idx]
        val y = data[idx + 1]
        atan2(y, x)
    }
    clock.benchmark(100_000, 50_000_000, "Fast") { i -> // 3 ns/e
        val idx = posMod(i, (data.size shr 1)) * 2
        val x = data[idx]
        val y = data[idx + 1]
        atan2Fast(y, x)
    }
}