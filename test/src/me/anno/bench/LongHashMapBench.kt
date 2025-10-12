package me.anno.bench

import speiger.primitivecollections.small.SmallLongToLongMap
import me.anno.utils.Clock
import me.anno.utils.types.Floats.f2
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import speiger.primitivecollections.LongToLongHashMap
import java.util.function.BiConsumer
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow

object LongHashMapBench {

    private val LOGGER = LogManager.getLogger("LongHashMapBench")

    /**
     * Benchmark insertion, getting, etc, LongToLongHashMap vs HashMap<Long,Long>()
     * */
    @JvmStatic
    fun main(args: Array<String>) {
        var checksum = 0L
        runBenchmark(
            LOGGER, "put",
            {}, { n, _ -> insertNBoxing(n) },
            {}, { n, _ -> insertNNative(n) },
            {}, { n, _ -> insertNSmall(n) }, 1000 // 10x and 30x are faster
        )

        runBenchmark(
            LOGGER, "get",
            { n -> insertNBoxing(n) }, { n, map -> get(n, map) },
            { n -> insertNNative(n) }, { n, map -> get(n, map) },
            { n -> insertNSmall(n) }, { n, map -> get(n, map) }, 300 // 10x is faster
        )

        runBenchmark(
            // todo this includes clone,
            //  somehow remove that by adding an intermediate phase, which is not measured
            LOGGER, "remove",
            { n -> insertNBoxing(n) }, { n, map -> remove(n, map) },
            { n -> insertNNative(n) }, { n, map -> remove(n, map) },
            { n -> insertNSmallQuick(n) }, { n, map -> remove(n, map) }, 3000 // 10x, 30x are faster
        )

        runBenchmark(
            LOGGER, "Kotlin-forEach",
            { n -> insertNBoxing(n) }, { _, map -> checksum += kotlinForEach(map) },
            { n -> insertNNative(n) }, { _, map -> checksum += forEach(map) },
            { n -> insertNSmallQuick(n) }, { _, map -> checksum += forEach(map) }, ns.last()
        )

        runBenchmark(
            LOGGER, "Java-forEach",
            { n -> insertNBoxing(n) }, { _, map -> checksum += javaForEach(map) },
            { n -> insertNNative(n) }, { _, map -> checksum += forEach(map) },
            { n -> insertNSmallQuick(n) }, { _, map -> checksum += forEach(map) }, ns.last()
        )

        // required to prevent compiler-optimization
        //  the SmallLongLongMap.forEach was completely removed
        LOGGER.debug("CheckSum: $checksum")
    }

    val ns = listOf(10, 30, 100, 300, 1000, 3000, 10_000, 30_000, 100_000, 300_000)

    fun <V1, V2> runBenchmark(
        logger: Logger, name: String,
        init1: (n: Int) -> V1, run1: (n: Int, V1) -> Unit,
        init2: (n: Int) -> V2, run2: (n: Int, V2) -> Unit
    ) {
        val clock = Clock(logger)
        logger.info("\n------ ${name.uppercase()} ------")
        var total = 0.0
        for (n in ns) {
            logger.info("> n = $n")
            val runs = max(10, 1000_000 / n)
            val warmup = max(5, runs / 10)
            val boxing = init1(n)
            val t1 = clock.benchmark(warmup, runs, n, "HashMap.$name") {
                run1(n, boxing)
            }
            val native = init2(n)
            val t2 = clock.benchmark(warmup, runs, n, "LongToLongHashMap.$name") {
                run2(n, native)
            }
            logger.info("${(t1 / t2).f2()}x faster")
            total += log2(t1 / t2)
        }
        total /= ns.size
        logger.info("Total[${name}]: ${2.0.pow(total).f2()}x faster on geomean average")
    }

    fun <V1, V2, V3> runBenchmark(
        logger: Logger, name: String,
        init1: (n: Int) -> V1, run1: (n: Int, V1) -> Unit,
        init2: (n: Int) -> V2, run2: (n: Int, V2) -> Unit,
        init3: (n: Int) -> V3, run3: (n: Int, V3) -> Unit, maxNSmall: Int
    ) {
        val clock = Clock(logger)
        logger.info("\n------ ${name.uppercase()} ------")
        var total12 = 0.0
        var total13 = 0.0
        for (n in ns) {
            logger.info("> n = $n")
            val runs = max(10, 1000_000 / n)
            val warmup = max(5, runs / 10)
            val boxing = init1(n)
            val t1 = clock.benchmark(warmup, runs, n, "HashMap.$name") {
                run1(n, boxing)
            }
            val native = init2(n)
            val t2 = clock.benchmark(warmup, runs, n, "LongToLongHashMap.$name") {
                run2(n, native)
            }
            val smallEnough = n <= maxNSmall
            val t3 = if (smallEnough) {
                val small = init3(n)
                clock.benchmark(warmup, runs, n, "SmallLongToLongMap.$name") {
                    run3(n, small)
                }
            } else 0.0
            total12 += log2(t1 / t2)
            if (smallEnough) {
                total13 += log2(t1 / t3)
                logger.info("${(t1 / t2).f2()}x, ${(t1 / t3).f2()}x faster")
            } else {
                logger.info("${(t1 / t2).f2()}x faster")
            }
        }
        total12 /= ns.size
        total13 /= ns.count { n -> n <= maxNSmall }
        logger.info("Total[${name}]: ${2.0.pow(total12).f2()}x, ${2.0.pow(total13).f2()}x faster on geomean average")
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

    fun insertNSmall(n: Int): SmallLongToLongMap {
        val map = SmallLongToLongMap(-1, n)
        for (i in 0 until n) map[i * (i + 1L)] = i.toLong()
        return map
    }

    fun insertNSmallQuick(n: Int): SmallLongToLongMap {
        val map = SmallLongToLongMap(-1, n)
        map.keys = LongArray(n) { i -> i * (i + 1L) }
        map.values = LongArray(n) { i -> i.toLong() }
        map.size = n
        return map
    }

    fun get(n: Int, map: HashMap<Long, Long>) {
        for (i in 0 until n) map[i * (i + 1L)]
    }

    fun get(n: Int, map: LongToLongHashMap) {
        for (i in 0 until n) map[i * (i + 1L)]
    }

    fun get(n: Int, map: SmallLongToLongMap) {
        for (i in 0 until n) map[i * (i + 1L)]
    }

    fun remove(n: Int, map: HashMap<Long, Long>) {
        val clone = HashMap(map)
        for (i in 0 until n) clone.remove(i * (i + 1L))
    }

    fun remove(n: Int, map: LongToLongHashMap) {
        val clone = map.clone()
        for (i in 0 until n) clone.remove(i * (i + 1L))
    }

    fun remove(n: Int, map: SmallLongToLongMap) {
        val clone = map.clone()
        for (i in 0 until n) clone.remove(i * (i + 1L))
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

    fun forEach(map: SmallLongToLongMap): Long {
        var sum = 0L
        map.forEach { k, v -> sum += k * v }
        return sum
    }
}