package me.anno.experiments

import me.anno.utils.Clock
import me.anno.utils.assertions.assertEquals
import kotlin.math.min

fun main() {
    val clock = Clock("CoinChange")
    assertEquals(3, coinChange(intArrayOf(1, 2, 5), 11, clock))
    assertEquals(0, coinChange(intArrayOf(1), 0, clock))
    assertEquals(2, coinChange(intArrayOf(1, 5, 6), 11, clock))
    assertEquals(-1, coinChange(intArrayOf(2), 3, clock))
    assertEquals(20, coinChange(intArrayOf(1, 2, 5), 100, clock))
    assertEquals(20, coinChange(intArrayOf(186, 419, 83, 408), 6249, clock))
    assertEquals(2000, coinChange(intArrayOf(1, 2, 5), 10000, clock))
    assertEquals(24, coinChange(intArrayOf(411, 412, 413, 414), 9864, clock))
    assertEquals(24, coinChange(intArrayOf(411, 412, 413, 414, 416), 9864, clock))
    assertEquals(24, coinChange(intArrayOf(411, 412, 413, 414, 416, 417), 9864, clock))
    assertEquals(24, coinChange(intArrayOf(411, 412, 413, 414, 416, 417, 418), 9864, clock))
    assertEquals(-1, coinChange(intArrayOf(411, 412, 413, 414, 415, 416, 417, 418, 419, 420, 421, 422), 9864, clock))
}

fun coinChange(coins: IntArray, amount: Int, clock: Clock): Int {
    val result = coinChange(coins, amount)
    clock.stop("${coins.toList()}, $amount")
    return result
}

fun coinChange(coins: IntArray, amount: Int): Int {
    if (amount == 0) return 0
    coins.sortDescending()
    val times = IntArray(coins.size)
    val v = coinChange(coins, times, amount, HashSet(), Int.MAX_VALUE)
    return if (v == Int.MAX_VALUE) -1
    else v
}

fun coinChange(coins: IntArray, times: IntArray, amount: Int, done: HashSet<List<Int>>, bestSeen: Int): Int {
    if (amount == 0) return 0
    var best = bestSeen
    fun testSequence(i: Int, c: Int) {
        times[i] += c
        if (done.add(times.toList())) {
            val solution = coinChange(coins, times, amount - c * coins[i], done, best)
            if (solution < Int.MAX_VALUE) best = min(solution + c, best)
        }
        times[i] -= c
    }
    for (i in coins.indices) {
        if (coins[i] > amount) continue
        val ideal = amount / coins[i]
        if (ideal > best) continue // not good enough
        testSequence(i, ideal)
        if (ideal > 1000) testSequence(i, 1000)
        if (ideal > 100) testSequence(i, 100)
        if (ideal > 10) testSequence(i, 10)
        if (ideal > 1) testSequence(i, 1)
    }
    return best
}