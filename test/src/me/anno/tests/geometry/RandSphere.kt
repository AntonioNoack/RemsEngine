package me.anno.tests.geometry

import me.anno.Time
import me.anno.image.ImageWriter
import me.anno.maths.Maths.PIf
import me.anno.utils.types.Floats.f1
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

fun main() {

    val rand = Random(1234L)
    val size = 200
    val analytic = IntArray(size)
    val perfect = IntArray(size)

    val v = Vector3f()

    fun genPerfect() {
        while (true) {
            val x = rand.nextFloat() * 2f - 1f
            val y = rand.nextFloat() * 2f - 1f
            val z = rand.nextFloat() * 2f - 1f
            if (x * x + y * y + z * z < 1f) {
                v.set(x, y, z)
                return
            }
        }
    }

    val c1 = 2f / PIf
    val c2 = 2f * PIf

    // I thought this may be faster, because it has no branches,
    // and no loop, but actually it is 13.3x slower 🤔
    fun genAnalytic() {
        val f = rand.nextFloat() * 2f - 1f
        val y = asin(f) * c1
        val cosY = sqrt(1f - y * y)
        val a = rand.nextFloat() * c2
        val x = cos(a) * cosY
        val z = sin(a) * cosY
        v.set(x, y, z)
    }

    val total = 1e7.toInt()
    var lastTime = Time.nanoTime
    var lastI = 0
    for (i in 0 until total) {// 400ns/e
        genAnalytic()
        analytic[((v.y * .5f + .5f) * size).toInt()]++
        if (i.and(1023) == 0) {
            val time = Time.nanoTime
            if (time - lastTime > 1e9) {
                println("$i/$total, ${(i * 100f / total).f1()}%, ${(time - lastTime) / (i - lastI)}ns/e")
                lastTime = time
                lastI = i
            }
        }
    }
    println("$total/$total, 100%, ${(Time.nanoTime - lastTime) / (total - lastI)}ns/e")
    lastTime = Time.nanoTime
    lastI = 0
    for (i in 0 until total) {// 30ns/e
        genPerfect()
        perfect[((v.y * .5f + .5f) * size).toInt()]++
        if (i.and(1023) == 0) {
            val time = Time.nanoTime
            if (time - lastTime > 1e9) {
                println("$i/$total, ${(i * 100f / total).f1()}%, ${(time - lastTime) / (i - lastI)}ns/e")
                lastTime = time
                lastI = i
            }
        }
    }
    println("$total/$total, 100%, ${(Time.nanoTime - lastTime) / (total - lastI)}ns/e")

    ImageWriter.writeImageCurve(
        512, 512, true, 0, -1,
        3, analytic.mapIndexed { index, i -> Vector2f(i.toFloat(), perfect[index].toFloat()) },
        "spherical.png"
    )

    println(perfect.joinToString())
    println(analytic.joinToString())

}