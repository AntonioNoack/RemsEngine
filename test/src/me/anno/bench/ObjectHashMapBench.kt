package me.anno.bench

import me.anno.bench.LongHashMapBench.ns
import me.anno.bench.LongHashMapBench.runBenchmark
import org.apache.logging.log4j.LogManager
import speiger.primitivecollections.ObjectToObjectHashMap
import java.util.function.BiConsumer

object ObjectHashMapBench {

    private val LOGGER = LogManager.getLogger("ObjectHashMapBench")

    /**
     * Benchmark insertion, getting, etc, ObjectToObjectHashMap vs HashMap<String,String>()
     * */
    @JvmStatic
    fun main(args: Array<String>) {
        val values = Array(ns.last()) { it.toString(36) }
        runBenchmark(
            LOGGER, "put",
            {}, { n, _ -> insertNNested(n, values) },
            {}, { n, _ -> insertNLinear(n, values) })

        runBenchmark(
            LOGGER, "get",
            { n -> insertNNested(n, values) }, { n, map -> get(n, map, values) },
            { n -> insertNLinear(n, values) }, { n, map -> get(n, map, values) })

        runBenchmark(
            LOGGER, "remove",
            { n -> insertNNested(n, values) }, { n, map -> remove(n, map, values) },
            { n -> insertNLinear(n, values) }, { n, map -> remove(n, map, values) })

        runBenchmark(
            LOGGER, "Kotlin-forEach",
            { n -> insertNNested(n, values) }, { _, map -> kotlinForEach(map) },
            { n -> insertNLinear(n, values) }, { _, map -> forEach(map) })

        runBenchmark(
            LOGGER, "Java-forEach",
            { n -> insertNNested(n, values) }, { _, map -> javaForEach(map) },
            { n -> insertNLinear(n, values) }, { _, map -> forEach(map) })
    }

    fun insertNNested(n: Int, values: Array<String>): HashMap<String, String> {
        val map = HashMap<String, String>(n)
        for (i in 0 until n) {
            val value = values[i]
            map[value] = value
        }
        return map
    }

    fun insertNLinear(n: Int, values: Array<String>): ObjectToObjectHashMap<String, String?> {
        val map = ObjectToObjectHashMap<String, String?>(null, n)
        for (i in 0 until n) {
            val value = values[i]
            map[value] = value
        }
        return map
    }

    fun get(n: Int, map: HashMap<String, String>, values: Array<String>) {
        for (i in 0 until n) map[values[i]]
    }

    fun get(n: Int, map: ObjectToObjectHashMap<String, String?>, values: Array<String>) {
        for (i in 0 until n) map[values[i]]
    }

    fun remove(n: Int, map: HashMap<String, String>, values: Array<String>) {
        for (i in 0 until n) map.remove(values[i])
    }

    fun remove(n: Int, map: ObjectToObjectHashMap<String, String?>, values: Array<String>) {
        for (i in 0 until n) map.remove(values[i])
    }

    fun kotlinForEach(map: HashMap<String, String>): Int {
        var sum = 0
        map.forEach { (k, v) -> sum += k.length * v.length }
        return sum
    }

    fun javaForEach(map: HashMap<String, String>): Int {
        var sum = 0
        map.forEach(BiConsumer<String, String> { k, v -> sum += k.length * v.length })
        return sum
    }

    // the compiler saw what I was doing without this sum, and completely removed my call :D
    fun forEach(map: ObjectToObjectHashMap<String, String?>): Int {
        var sum = 0
        map.forEach { k, v -> sum += k.length * v!!.length }
        return sum
    }
}