package me.anno.tests.gfx

import me.anno.image.ImageCache
import me.anno.image.raw.IntImage
import me.anno.maths.Maths.mix
import me.anno.utils.OS
import me.anno.utils.OS.desktop
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import java.util.*

@Suppress("UNUSED_PARAMETER")
object ImageTracing {

    // test this with real letters
    // to do set the test size for meshes to 120 instead of 20-ish
    @JvmStatic
    fun main(args: Array<String>) {
        val image = ImageCache[OS.documents.getChild("test-text.png"), false]!!
        val pixels = (image as IntImage).data
        val black = -0x1000000
        var i = 0
        val l = pixels.size
        while (i < l) {
            pixels[i] = pixels[i] and black
            i++
        }
        computeOutline(image.width, image.height, pixels)
    }

    fun computeOutline(w: Int, h: Int, pixels: IntArray) {
        var i2 = 0
        val l = pixels.size
        while (i2 < l) {
            pixels[i2] = (pixels[i2] ushr 24) - 128
            i2++
        }
        val ops = IntArrayList(64)
        val data = FloatArrayList(256)
        val edge = IntArrayList(64)
        val p = FloatArray(2)
        val p2 = FloatArray(2)
        val done = BitSet(w * h * 2)
        val s = 16
        val ii = IntImage(w * s, h * s, false)
        val ij = IntImage(w, h, false)
        var ctr = 0
        var y = 0
        var i = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                if (isEdge(x, y, w, pixels, true) &&
                    !done[x + y * w] && !done[x + y * w + 1]
                ) {
                    traceEdge(x, y, w, h, pixels, done, edge)
                    if (edge.size <= 2) {
                        x++
                        i++
                        continue
                    }

                    // a full curve was found -> turn into points & lines
                    edgeToPoint(edge.getValue(0), w, pixels, p)
                    moveTo(p[0], p[1], ops, data)
                    run {
                        var k = 1
                        val l2 = edge.size
                        while (k < l2) {
                            edgeToPoint(edge.getValue(k), w, pixels, p)
                            lineTo(p[0], p[1], ops, data)
                            k++
                        }
                    }
                    close(ops)
                    for (k in pixels.indices) ii.setRGB(k % w, k / w, 0)
                    var k = 0
                    val l3 = edge.size - 1
                    while (k < l3) {
                        edgeToPoint(edge.getValue(k), w, pixels, p)
                        edgeToPoint(edge.getValue(k + 1), w, pixels, p2)
                        for (m in 0 until s * 2) {
                            val f = m / (s * 2 - 1f)
                            val px = (mix(p[0], p2[0], f) * s).toInt()
                            val py = (mix(p[1], p2[1], f) * s).toInt()
                            ii.setRGB(px, py, -1)
                        }
                        k++
                    }
                    ii.write(desktop.getChild("it/lines-" + ctr++ + ".png"))
                }
                x++
                i++
            }
            i++
            y++
        }
        for (k in 0 until w * h) ij.setRGB(k % w, k / w, if (done[k * 2] || done[k * 2 + 1]) 0 else -1)
        ij.write(desktop.getChild("it/done.png"))
    }

    private fun edgeToPoint(edge: Int, w: Int, pixels: IntArray, dst: FloatArray) {
        val i = edge shr 1
        val x = i % w
        val y = i / w
        dst[0] = x + 0.5f
        dst[1] = y + 0.5f
        if (isVEdge(edge)) {
            dst[0] += cut(pixels[i], pixels[i + 1])
        } else {
            dst[1] += cut(pixels[i], pixels[i + w])
        }
    }

    private fun traceEdge(x0: Int, y0: Int, w: Int, h: Int, pixels: IntArray, done: BitSet, dst: IntArrayList) {
        dst.clear()
        var x = x0
        var y = y0
        // our direction
        // 0: ->
        // 1: V
        // 2: <-
        // 3: A
        var edge: Int
        var dir = 1
        while (true) {
            var i3 = dir * 3
            var vEdge = dir and 1 == 1
            var i = 0
            while (i < 3) {
                val x2 = x + dx[i3]
                val y2 = y + dy[i3++]
                if (isEdge(x2, y2, w, pixels, vEdge)) {
                    // go right
                    x = x2
                    y = y2
                    dir = dir + nextDir[i] and 3
                    break
                }
                vEdge = dir and 1 == 0
                i++
            }
            if (i >= 3) return
            edge = (x + y * w) * 2 + (dir and 1)
            dst.plusAssign(edge)
            if (done[edge]) return
            done[edge] = true
        }
    }

    private fun isVEdge(edge: Int): Boolean {
        return edge and 1 == 1
    }

    private val dx = intArrayOf(
        +1, +0, +0,
        +0, +0, +1,
        -1, -1, -1,
        +0, +1, +0
    )
    private val dy = intArrayOf(
        +0, +1, +0,
        +1, +0, +0,
        +0, +0, +1,
        -1, -1, -1
    )
    private val nextDir = intArrayOf(0, 1, 3)
    private fun isEdge(x: Int, y: Int, w: Int, pixels: IntArray, vEdge: Boolean): Boolean {
        val i0 = x + y * w
        val i1: Int
        if (i0 < 0 || x >= w || i0 >= pixels.size) return false
        if (vEdge) {
            if (x + 1 >= w) return false
            i1 = i0 + 1
        } else {
            i1 = i0 + w
            if (i1 >= pixels.size) return false
        }
        return pixels[i0] > 0 != pixels[i1] > 0
    }

    private fun cut(ai: Int, bi: Int): Float {
        var a = ai
        var b = bi
        if (a < 0 == b < 0) return 0.5f
        if (a > b) a++ else b++
        return a / (a - b).toFloat()
    }

    private fun close(ops: IntArrayList) {
        println("close")
    }

    private fun moveTo(x: Float, y: Float, ops: IntArrayList, data: FloatArrayList) {
        println("move to $x, $y")
    }

    private fun lineTo(x: Float, y: Float, ops: IntArrayList, data: FloatArrayList) {
        println("line to $x, $y")
    }
}