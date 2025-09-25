package me.anno.bench

import me.anno.utils.Clock
import me.anno.utils.types.Floats.f2
import org.apache.logging.log4j.LogManager
import speiger.primitivecollections.LongToLongHashMap
import java.util.function.BiConsumer
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow

private val LOGGER = LogManager.getLogger("HashMapBench")
private val clock = Clock(LOGGER)

/**
 * Benchmark insertion, getting, etc, LongToLongHashMap vs HashMap<Long,Long>()
 * */
fun main() {
    runBenchmark(
        "put",
        {}, { n, _ -> insertNBoxing(n) },
        {}, { n, _ -> insertNNative(n) })

    runBenchmark(
        "get",
        { n -> insertNBoxing(n) }, { n, map -> get(n, map) },
        { n -> insertNNative(n) }, { n, map -> get(n, map) })

    runBenchmark(
        "remove",
        { n -> insertNBoxing(n) }, { n, map -> remove(n, map) },
        { n -> insertNNative(n) }, { n, map -> remove(n, map) })

    runBenchmark(
        "Kotlin-forEach",
        { n -> insertNBoxing(n) }, { _, map -> kotlinForEach(map) },
        { n -> insertNNative(n) }, { _, map -> forEach(map) })

    runBenchmark(
        "Java-forEach",
        { n -> insertNBoxing(n) }, { _, map -> javaForEach(map) },
        { n -> insertNNative(n) }, { _, map -> forEach(map) })
}

val ns = listOf(10, 30, 100, 300, 1000, 3000, 10_000, 30_000, 100_000, 300_000)

fun <V1, V2> runBenchmark(
    name: String,
    init1: (n: Int) -> V1, run1: (n: Int, V1) -> Unit,
    init2: (n: Int) -> V2, run2: (n: Int, V2) -> Unit
) {
    LOGGER.info("\n------ ${name.uppercase()} ------")
    var total = 0.0
    for (n in ns) {
        LOGGER.info("> n = $n")
        val runs = max(10, 1000_000 / n)
        val warmup = max(5, runs / 10)
        val boxing = init1(n)
        val t0 = clock.benchmark(warmup, runs, n, "HashMap.$name") {
            run1(n, boxing)
        }
        val native = init2(n)
        val t1 = clock.benchmark(warmup, runs, n, "LongToLongHashMap.$name") {
            run2(n, native)
        }
        LOGGER.info("${(t0 / t1).f2()}x faster")
        total += log2(t0 / t1)
    }
    total /= ns.size
    LOGGER.info("Total[${name}]: ${2.0.pow(total).f2()}x faster on geomean average")
}

fun insertNBoxing(n: Int): HashMap<Long, Long> {
    val map = HashMap<Long, Long>(n)
    for (i in 0 until n) map[i * (i + 1L)] = i.toLong()
    return map
}

fun insertNNative(n: Int): LongToLongHashMap {
    val map = LongToLongHashMap(-1, n)
    for (i in 0 until n) map[i * (i + 1L)] = i.toLong()
    return map
}

fun get(n: Int, map: HashMap<Long, Long>) {
    for (i in 0 until n) map[i * (i + 1L)]
}

fun get(n: Int, map: LongToLongHashMap) {
    for (i in 0 until n) map[i * (i + 1L)]
}

fun remove(n: Int, map: HashMap<Long, Long>) {
    for (i in 0 until n) map.remove(i * (i + 1L))
}

fun remove(n: Int, map: LongToLongHashMap) {
    for (i in 0 until n) map.remove(i * (i + 1L))
}

fun kotlinForEach(map: HashMap<Long, Long>): Long {
    var sum = 0L
    map.forEach { (k, v) -> sum += k * v }
    return sum
}

fun javaForEach(map: HashMap<Long, Long>): Long {
    var sum = 0L
    map.forEach(BiConsumer<Long, Long> { k, v -> sum += k * v })
    return sum
}

// the compiler saw what I was doing without this sum, and completely removed my call :D
fun forEach(map: LongToLongHashMap): Long {
    var sum = 0L
    map.forEach { k, v -> sum += k * v }
    return sum
}