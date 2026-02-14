package me.anno.bench

import me.anno.maths.Maths
import me.anno.maths.noise.RandomBySeed
import me.anno.maths.noise.RandomUtils.nextGaussian
import me.anno.maths.noise.RandomUtils.nextGaussianF
import me.anno.utils.Clock
import java.util.Random

fun main() {
    val n = 100_000_000
    val w = 100_000
    val clock = Clock("Random")
    val seed = 1234L
    val java = Random(seed)
    val kotlin = kotlin.random.Random(seed)

    // our RandomBySeed.nextInt is somehow much slower than Java and Kotlin :(
    clock.benchmark(w, n, "Java.nextInt()") { java.nextInt() }
    clock.benchmark(w, n, "Kotlin.nextInt()") { kotlin.nextInt() }
    clock.benchmark(w, n, "RandomBySeed.nextInt()") { RandomBySeed.getRandomInt(seed + it) }
    clock.benchmark(w, n, "Maths.nextInt()") { Maths.randomInt() }
    println()

    // but our RandomBySeed.nextLong is much faster... weird...
    clock.benchmark(w, n, "Java.nextLong()") { java.nextLong() }
    clock.benchmark(w, n, "Kotlin.nextLong()") { kotlin.nextLong() }
    clock.benchmark(w, n, "RandomBySeed.nextLong()") { RandomBySeed.getRandomLong(seed + it) }
    clock.benchmark(w, n, "Maths.nextLong()") { Maths.randomLong() }
    println()

    clock.benchmark(w, n, "Java.nextInt(17)") { java.nextInt(17) }
    clock.benchmark(w, n, "Kotlin.nextInt(17)") { kotlin.nextInt(17) }
    clock.benchmark(w, n, "RandomBySeed.nextInt(17)") { RandomBySeed.getRandomInt(seed + it, 0, 17) }
    clock.benchmark(w, n, "Maths.nextInt(17)") { Maths.randomInt(0, 17) }
    println()

    clock.benchmark(w, n, "Kotlin.nextLong(17)") { kotlin.nextLong(17) }
    clock.benchmark(w, n, "RandomBySeed.nextLong(17)") { RandomBySeed.getRandomLong(seed + it, 0, 17) }
    clock.benchmark(w, n, "Maths.nextLong(17)") { Maths.randomLong(0, 17) }
    println()

    clock.benchmark(w, n, "Java.nextFloat()") { java.nextFloat() }
    clock.benchmark(w, n, "Kotlin.nextFloat()") { kotlin.nextFloat() }
    clock.benchmark(w, n, "RandomBySeed.nextFloat()") { RandomBySeed.getRandomFloat(seed + it) }
    clock.benchmark(w, n, "Maths.nextFloat()") { Maths.random().toFloat() }
    println()

    clock.benchmark(w, n, "Java.nextDouble()") { java.nextDouble() }
    clock.benchmark(w, n, "Kotlin.nextDouble()") { kotlin.nextDouble() }
    clock.benchmark(w, n, "RandomBySeed.nextDouble()") { RandomBySeed.getRandomDouble(seed + it) }
    clock.benchmark(w, n, "Maths.nextDouble()") { Maths.random() }
    println()

    clock.benchmark(w, n, "Java.nextGaussianF()") { java.nextGaussian().toFloat() }
    clock.benchmark(w, n, "Kotlin.nextGaussianF()") { kotlin.nextGaussianF() }
    clock.benchmark(w, n, "RandomBySeed.nextGaussianF()") { RandomBySeed.getRandomGaussianF(seed + it) }
    println()

    clock.benchmark(w, n, "Java.nextGaussian()") { java.nextGaussian() }
    clock.benchmark(w, n, "Kotlin.nextGaussian()") { kotlin.nextGaussian() }
    clock.benchmark(w, n, "RandomBySeed.nextGaussian()") { RandomBySeed.getRandomGaussian(seed + it) }
}