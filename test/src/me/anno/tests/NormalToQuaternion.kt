package me.anno.tests

import me.anno.maths.Maths.length
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.types.Vectors.normalToQuaternion
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*

fun main() {
    // half the time is being used for correctness checks and random number generation ->
    // correctness and performance shouldn't be tested at the same time
    val testCorrectness = false
    val rand = Random()
    val dir1 = Vector3f()
    val dir2 = Vector3f()
    val rot = Quaternionf()
    var t0 = System.nanoTime()
    var li = 0
    val runtime = Runtime.getRuntime()
    for (i in 0 until 1_000_000) {
        rand.nextFloat()
        rand.nextFloat()
        rand.nextFloat()
    }
    val t2 = System.nanoTime()
    println("for random: ${(t2 - t0) / 1e6} ns/e")
    val random = FloatArray(4096) { rand.nextFloat() * 2f - 1f }
    for (i in 0 until random.size - 2 step 3) {
        val f = 1f / length(random[i], random[i + 1], random[i + 2])
        random[i] *= f
        random[i + 1] *= f
        random[i + 2] *= f
    }
    var j = 0
    for (i in 0 until 1_000_000_003) {
        if (i and 0xffff == 0) {
            val t1 = System.nanoTime()
            if (t1 - t0 >= 1_000_000_000) {
                val used = runtime.totalMemory() - runtime.freeMemory()
                println("-- $i, ${(t1 - t0) / (i - li)} ns/e, ${used.formatFileSize()}")
                // runtime.gc()
                t0 = System.nanoTime()
                li = i
            }
        }
        // dir1.set(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).me.anno.tests.normalize()
        if (testCorrectness) {
            dir1.set(
                rand.nextFloat() * 2f - 1f,
                rand.nextFloat() * 2f - 1f,
                rand.nextFloat() * 2f - 1f
            ).normalize()
        } else {
            if (j + 3 > random.size) j = 0
            dir1.set(random[j++], random[j++], random[j++])
        }
        dir1.normalToQuaternion(rot)
        if (testCorrectness) {
            dir2.set(0f, 1f, 0f).rotate(rot)
            if (dir2.dot(dir1) < 0.99f) {
                throw RuntimeException("Error[$i]: $dir1 -> $rot -> $dir2")
            }// else println(" ok ")
        }
    }
}