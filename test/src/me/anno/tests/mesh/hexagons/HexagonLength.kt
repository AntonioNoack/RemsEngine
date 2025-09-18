package me.anno.tests.mesh.hexagons

import me.anno.maths.Maths.TAUf
import me.anno.maths.MinMax.min
import me.anno.maths.Maths.mix
import me.anno.maths.chunks.spherical.Hexagon
import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.utils.search.BinarySearch
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

val topPentagonIndex = 11
fun main() {
    // plot our I,F values on a log/?-plot
    // calculate the ideal F values by comparing the surface area of the pentagon and its neighbor

    val expectedRatio = getSurfaceAreaRegularPolygon(5) / getSurfaceAreaRegularPolygon(6)
    println("expected: $expectedRatio")

    var lastDelta = 0.99f
    var allGood = true
    val result = ArrayList<Float>()
    for ((i, n) in HexagonSphere.lengthI.withIndex()) {
        if (n == 0) continue
        val lengthF0 = HexagonSphere.lengthF[i]
        val minValue = (lengthF0 * lastDelta).toRawBits()
        val maxValue = (lengthF0 / lastDelta).toRawBits()
        var index = BinarySearch.binarySearch(minValue, maxValue) { idx ->
            val value = Float.fromBits(idx)
            HexagonSphere.lengthF[i] = value // manipulate value
            -getScore(n).compareTo(expectedRatio)
        }
        if (index < 0) {
            index = -1 - index
        }
        val lengthF1 = Float.fromBits(index)
        HexagonSphere.lengthF[i] = lengthF1
        val check = getScore(n) / expectedRatio
        println("$n: $lengthF0 -> $lengthF1, check: $check [$lastDelta]")
        allGood = allGood && abs(check - 1.0) < 0.01
        if(allGood) {
            lastDelta = mix(lastDelta, min(lengthF0, lengthF1) / max(lengthF0, lengthF1), 0.3f)
            result.add(lengthF1)
        }
    }
    println(result.map { "${it}f" })
}

fun getScore(n: Int): Double {
    val sphere = HexagonSphere(n, 1)
    val pentagon = sphere.pentagons[topPentagonIndex]
    return pentagon.neighbors.sumOf { getSurfaceArea(pentagon) / getSurfaceArea(it!!).toDouble() } / 5
}

fun getSurfaceAreaRegularPolygon(n: Int): Float {
    return 0.5f * n * sin(TAUf / n)
}

fun getSurfaceArea(hexagon: Hexagon): Float {
    val normal = Vector3f()
    val a = Vector3f()
    val b = Vector3f()
    val corners = hexagon.corners
    for (ci in 1 until corners.size) {
        val c0 = corners[ci - 1]
        val c1 = corners[ci]
        val c2 = corners[(ci + 1) % corners.size]
        c1.sub(c0, a)
        c2.sub(c1, b)
        normal.add(a.cross(b))
    }
    return normal.length() * 0.5f
}