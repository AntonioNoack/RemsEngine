package me.anno.tests.mesh.hexagons

import me.anno.image.ImageDrawing.drawLine
import me.anno.image.ImageDrawing.mixRGB
import me.anno.image.raw.IntImage
import me.anno.maths.Maths.TAUf
import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.utils.OS.desktop
import org.joml.Vector3f
import org.joml.Vector3i
import java.util.Random
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun main() {
    val sphere = HexagonSphere(100, 5)
    testFindingChunks(sphere)
}

/**
 * visualize how the engine decides chunks, and how we do the inverse transform
 * */
@Suppress("unused")
fun testFindingChunks(sphere: HexagonSphere) {

    val sijToColor = HashMap<Triple<Int, Int, Int>, Int>()
    val rand = Random(1234L)

    val size = 2048
    val hs = size / 2
    val image = IntImage(size * 3, size, false)

    var ok = 0L
    var w1 = 0L
    var w2 = 0L
    for (triIndex in 0 until 20) {
        for (si in 0 until sphere.chunkCount) {
            for (sj in 0 until sphere.chunkCount - si) {
                val hexagons = sphere.queryChunk(triIndex, si, sj)
                val color0 = sijToColor.getOrPut(Triple(triIndex, si, sj)) { rand.nextInt() }
                for (hex in hexagons) {
                    val q = sphere.findClosestChunk(hex.center)
                    val color1 = sijToColor.getOrPut(Triple(q.tri, q.si, q.sj)) { rand.nextInt() }
                    if (q.tri != triIndex) w1++// println("wrong triangle for $triIndex/$si/$sj (${hex.center}, ${hex.index})")
                    else if (q.si != si || q.sj != sj) w2++ // println("wrong chunk for $triIndex/$si/$sj (${hex.center}, ${hex.index})")
                    else ok++
                    if (hex.center.y > 0f) {
                        val x = ((hex.center.x + 1) * hs)
                        val y = ((hex.center.z + 1) * hs)
                        image.mixRGB(x, y, color0, 1f)
                        image.mixRGB(x + size, y, color1, 1f)
                        if (color0 != color1) image.mixRGB(x + size * 2, y, color1, 1f)
                    }
                }
                val cx = hexagons.map { it.center.x }.average().toFloat()
                val cy = hexagons.map { it.center.y }.average().toFloat()
                val cz = hexagons.map { it.center.z }.average().toFloat()
                val center = sphere.triangles[triIndex].getChunkCenter(si, sj)
                if (cy > 0f && center.y > 0f) {
                    val x0 = ((cx + 1) * hs)
                    val y0 = ((cz + 1) * hs)
                    val x1 = ((center.x + 1) * hs)
                    val y1 = ((center.z + 1) * hs)
                    image.drawLine(x0, y0, x1, y1, 0xff00ff, 1f)
                }
            }
        }
    }

    image.write(desktop.getChild("chunk.png"))

    val total = sphere.numHexagons.toFloat()
    println("${ok / total}/${w1 / total}/${w2 / total} ok/wrong-tri/wrong-chunk")
}

@Suppress("unused")
fun testFindingChunks2(sphere: HexagonSphere) {

    // visualize how the engine decides chunks, and how we do the inverse transform

    val maxDistance = sphere.hexagonsPerSide / 2 * sphere.len

    val size = 2048
    val hs = size / 2f
    val image = IntImage(size, size, false)

    val queried = HashSet<Vector3i>()
    sphere.queryChunks(Vector3f(0f, 1f, 0f), maxDistance) { sc ->
        queried.add(Vector3i(sc.tri, sc.si, sc.sj))
        false
    }

    val rand = Random(1234L)
    for (tri in 0 until 20) {
        for (si in 0 until sphere.chunkCount) {
            for (sj in 0 until sphere.chunkCount - si) {
                val hexagons = sphere.queryChunk(tri, si, sj)
                val color0 = rand.nextInt().and(0x777777) or (if (Vector3i(tri, si, sj) in queried) 0x808080 else 0)
                for (hex in hexagons) {
                    if (hex.center.y > 0f) {
                        val x = ((hex.center.x + 1) * hs)
                        val y = ((hex.center.z + 1) * hs)
                        image.mixRGB(x, y, color0, 1f)
                    }
                }
            }
        }
    }

    // draw circle :)
    val r = sin(maxDistance) * hs
    val rs = (r * TAUf).roundToInt()
    var x0 = hs + r
    var y0 = hs
    for (i in 1..rs) {
        val a = i * TAUf / rs
        val x1 = hs + cos(a) * r
        val y1 = hs + sin(a) * r
        image.drawLine(x0, y0, x1, y1, -1)
        x0 = x1
        y0 = y1
    }

    image.write(desktop.getChild("chunk2.png"))
}
