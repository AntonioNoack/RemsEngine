package me.anno.tests.image.svg

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.fonts.signeddistfields.edges.EdgeSegment
import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.fonts.signeddistfields.edges.QuadraticSegment
import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.image.ImageCache
import me.anno.image.raw.IntImage
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.utils.Color.black
import me.anno.utils.Color.toHexColor
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.OS.desktop
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.lists.Lists.all2
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

val maxColorDiffSq = sq(10f / 255f)

// covert an easy image from PNG to SVG
//  - find blobs of the same color
//  - separate/segment that area
//  - define a distance/error metric
//  -> dual contouring / marching squares
//  -> simplify the shape by combining linear sections into splines as much as possible to reduce the size
fun main() {
    OfficialExtensions.initForTests()
    val src = desktop.getChild("gofluent-for-svg.png")
    val png = ImageCache[src, false]!!
    val seg = segmentation(png.asIntImage())
    mapRandomly(seg.indices).write(desktop.getChild("segments.png"))
    val segmentBySize = seg.weights.withIndex().sortedByDescending { it.value }
    val builder = StringBuilder()
    val bg = seg.colors[segmentBySize.first().index].toHexColor()
    builder.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ${png.width} ${png.height}\" style=\"background-color:$bg\">\n")
    for (j in 1 until segmentBySize.size) {
        val i = segmentBySize[j].index
        val ci = seg.indices.data[i]
        if (seg.weights[i] < 3f) continue
        if (seg.maxXs[ci] - seg.minXs[ci] < 2 || seg.maxYs[ci] - seg.minYs[ci] < 2) continue
        val line = extractSegment(seg, ci)
        // todo results:
        //  p1 is broken in some places,
        //  p0 is fine, but unoptimized,
        //  p2 is a little crooked (off by one error, probably)
        appendPolygon2(builder, seg.colors[ci], line)
        // appendPolygon0(builder, Vector4f(seg.colors[ci]).apply { w *= 0.3f }, line)
        // val line1 = simplifyLine(line)
        //appendPolygon1(builder, seg.colors[ci], line1)
    }
    builder.append("</svg>")
    desktop.getChild("png2svg.svg")
        .writeText(builder.toString())
    Engine.requestShutdown()
}

fun appendPolygon0(builder: StringBuilder, color: Vector4f, line: List<Vector2f>) {
    builder.append("  <polygon fill=\"${color.toHexColor()}\" points=\"")
    for (vertex in line) {
        builder
            .append1(vertex.x).append(',')
            .append1(vertex.y).append(' ')
    }
    builder.setLength(builder.length - 1)
    builder.append("\"/>\n")
}

fun appendPolygon2(builder: StringBuilder, color: Vector4f, line: List<Vector2f>) {
    builder.append("  <path fill=\"${color.toHexColor()}\" d=\"")
    var last = line.first()
    builder.append('M').append1(last.x).append(',').append1(last.y)
    var i0 = 0
    loop@ while (i0 < line.size) {
        fun next(i: Int) {
            // to do optimize V/H? rare
            val vertex = line[i]
            builder
                .append('l')
                .append1(vertex.x - last.x).append(',')
                .append1(vertex.y - last.y)
            last = vertex
        }

        var i1 = i0 + 2
        while (i1 <= line.size) {
            if (isValid(subList(line, i0, i1), LinearSegment(line[i0], line[i1 - 1]), 0.9f)) {
                i1++
            } else {
                next(i1 - 1)
                i0 = i1 - 1
                continue@loop
            }
        }
        next(line.lastIndex)
        break
    }
    builder.append('z')
    builder.append("\"/>\n")
}

fun StringBuilder.append1(v: Float): StringBuilder {
    append(v)
    if (endsWith(".0")) {
        setLength(length - 2)
    }
    return this
}

fun appendPolygon1(builder: StringBuilder, color: Vector4f, line: List<EdgeSegment>) {
    builder.append("  <path fill=\"${color.toHexColor()}\" d=\"")
    val s0 = line[0]
    val p0 = s0.point(0f, Vector2f())
    builder.append('M').append1(p0.x).append(' ')
        .append1(p0.y).append(' ')
    var last = p0
    val tmp = Vector2f()
    for (i in line.indices) {
        assertEquals(last, line[i].point(0f, tmp))
        last = when (val segment = line[i]) {
            is LinearSegment -> {
                when {
                    segment.p0.x == segment.p1.x -> {
                        builder.append('h')
                            .append1(segment.p1.y - segment.p0.y)
                    }
                    segment.p0.y == segment.p1.y -> {
                        builder.append('v')
                            .append1(segment.p1.x - segment.p0.x)
                    }
                    else -> {
                        builder.append('l')
                            .append1(segment.p1.x - segment.p0.x).append(' ')
                            .append1(segment.p1.y - segment.p0.y)
                    }
                }
                segment.p1
            }
            is QuadraticSegment -> {
                builder.append('q')
                    .append1(segment.p1.x - segment.p0.x).append(' ')
                    .append1(segment.p1.y - segment.p0.y).append(',')
                    .append1(segment.p2.x - segment.p0.x).append(' ')
                    .append1(segment.p2.y - segment.p0.y)
                segment.p2
            }
            else -> throw NotImplementedError()
        }
    }
    builder.append('z') // close path
    builder.append("\"/>\n")
}

fun getSimplificationOptions(list: List<Vector2f>): List<EdgeSegment> {
    if (list.size < 2) return emptyList()
    val p0 = list.first()
    val pn = list.last()
    val pc = list[list.size / 2]
    val pq = Vector2f(
        2f * pc.x - (p0.x + pn.x),
        2f * pc.y - (p0.y + pn.y)
    )
    return listOf(
        LinearSegment(p0, pn),
        //   QuadraticSegment(p0, pc, pn),
        //   QuadraticSegment(p0, pq, pn)
    )
}

fun subList(line: List<Vector2f>, i: Int, j: Int): List<Vector2f> {
    val dst = ArrayList<Vector2f>(j - i)
    dst.addAll(line.subList(i, min(j, line.size)))
    dst.addAll(line.subList(0, max(j - line.size, 0)))
    return dst
}

fun isValid(line: List<Vector2f>, segment: EdgeSegment, err: Float): Boolean {
    val tmp1 = FloatPtr()
    val tmp2 = FloatArray(3)
    val tmp3 = SignedDistance()
    return line.all2 { abs(segment.signedDistance(it, tmp1, tmp2, tmp3).distance) < err }
}

fun getSimplification(line: List<Vector2f>): EdgeSegment? {
    return getSimplificationOptions(line).firstOrNull { option ->
        isValid(line, option, 0.9f)
    }
}

fun simplifyLine(line: List<Vector2f>): List<EdgeSegment> {
    val dst = ArrayList<EdgeSegment>()
    var i = 0
    while (i < line.size) {
        // check whether these points can be simplified
        var j = min(i + 2, line.size)
        var best = getSimplification(subList(line, i, j + 1))!!
        while (j + 1 < line.size) {
            best = getSimplification(subList(line, i, j + 2)) ?: break
            j++
        }
        dst.add(best)
        i = j
    }
    return dst
}

fun mapRandomly(src: IntImage): IntImage {
    val dst = IntImage(src.width, src.height, false)
    val srcI = src.data
    val dstI = dst.data
    for (i in srcI.indices) {
        dstI[i] = (srcI[i] * 92681).hashCode() or black
    }
    return dst
}

val dir = listOf(
    Vector2i(-1, 0),
    Vector2i(+1, 0),
    Vector2i(0, -1),
    Vector2i(0, +1),
)

class Segmentation(
    val indices: IntImage,
    val colors: Array<Vector4f>,
    val weights: FloatArray,
    val minXs: IntArray, val minYs: IntArray,
    val maxXs: IntArray, val maxYs: IntArray,
)

fun extractSegment(seg: Segmentation, i: Int): List<Vector2f> {
    val src = seg.indices
    val w = src.width
    val srcI = src.data
    val ci = srcI[i]
    val x0 = seg.minXs[ci]
    val x1 = seg.maxXs[ci]
    val y0 = seg.minYs[ci]
    val y1 = seg.maxYs[ci]
    val line = ArrayList<Vector2f>()
    val startY = y0 + 0
    val startX = (x0..x1).first { x -> srcI[x + startY * w] == ci }
    line.add(Vector2f(startX - 0.5f, startY - 0.5f))
    line.add(Vector2f(startX + 0.5f, startY - 0.5f))
    while (true) {
        fun isSame(x: Float, y: Float): Boolean {
            val xi = round(x).toInt()
            val yi = round(y).toInt()
            return xi in x0..x1 && yi in y0..y1 &&
                    srcI[xi + yi * w] == ci
        }
        // find next point:
        //  - straight / left turn / right turn
        val p0 = line[line.size - 2]
        val p1 = line[line.size - 1]
        val nx = mix(p0.x, p1.x, 1.5f)
        val ny = mix(p0.y, p1.y, 1.5f)
        val cx = p1.y - p0.y
        val cy = p0.x - p1.x
        val a = isSame(nx + cx * 0.5f, ny + cy * 0.5f)
        val b = isSame(nx - cx * 0.5f, ny - cy * 0.5f)
        when {
            a && b -> {
                // left
                line.add(Vector2f(p1).add(cx, cy))
            }
            !a && b -> {
                // straight
                line.add(p0.lerp(p1, 2f, Vector2f()))
            }
            else -> {
                // right
                line.add(Vector2f(p1).sub(cx, cy))
            }
        }
        if (line.last().distanceSquared(line.first()) < 0.5f) {
            line.removeLast()
            for (v in line) {
                v.add(0.5f, 0.5f)
            }
            return line
        }
    }
}

fun segmentation(src: IntImage): Segmentation {
    // spread seeds every 1x1 color
    // combine similar colors / weights
    // calculate bounds for every group
    val w = src.width
    val h = src.height
    val dst = IntImage(w, h, false)
    val srcI = src.data
    val dstI = dst.data
    for (i in dstI.indices) dstI[i] = i
    val colors = Array(dstI.size) { Vector4f() }
    val weights = FloatArray(dstI.size)
    weights.fill(1f)
    for (i in colors.indices) {
        srcI[i].toVecRGBA(colors[i])
    }
    val remap = IntArray(dstI.size)
    for (i in dstI.indices) remap[i] = i
    val minXs = IntArray(dstI.size)
    val minYs = IntArray(dstI.size)
    val maxXs = IntArray(dstI.size)
    val maxYs = IntArray(dstI.size)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val i = x + y * w
            minXs[i] = x
            maxXs[i] = x
            minYs[i] = y
            maxYs[i] = y
        }
    }

    val seg = Segmentation(
        dst, colors, weights,
        minXs, minYs, maxXs, maxYs
    )

    var ctr = 0
    for (k in 0 until 3) {
        fun process(j: Int) {

            if (++ctr % 100000 == 0) println("$ctr/${dstI.size}")

            val x = j % w
            val y = j / w
            if (x < 1 || x >= w - 1 ||
                y < 1 || y >= h - 1
            ) return

            val ci = dstI[j]
            // combine while close enough
            val colorI = colors[ci]
            // check if any neighbor is interesting
            for (d in dir) {
                val j1 = j + d.x + d.y * w
                val cj = dstI[j1]

                fun remap(x: Int, y: Int, d: Int): Int {
                    return if (x in 0 until w && y in 0 until h) {
                        val i = x + y * w
                        if (dstI[i] == cj) {
                            dstI[i] = ci
                            minXs[ci] = min(minXs[ci], x)
                            minYs[ci] = min(minYs[ci], y)
                            maxXs[ci] = max(maxXs[ci], x)
                            maxYs[ci] = max(maxYs[ci], y)
                            if (d < 500) {
                                val di = d + 1
                                1 + // self
                                        remap(x - 1, y, di) +
                                        remap(x + 1, y, di) +
                                        remap(x, y - 1, di) +
                                        remap(x, y + 1, di)
                            } else 1
                        } else 0
                    } else 0
                }

                val colorJ = colors[cj]
                if (ci != cj && colorJ.distanceSquared(colorI) < maxColorDiffSq) {
                    if (weights[ci] + 16 >= weights[cj]) {
                        val w0 = weights[ci]
                        val w1 = weights[cj]
                        // we need to remap all its members
                        val wx = remap(x + d.x, y + d.y, 0)
                        val t = wx / (w0 + w1)
                        colorI.lerp(colors[cj], t)
                        weights[ci] = w0 + wx
                        weights[cj] = w1 - wx
                    }
                }
            }
        }
        for (j in dstI.indices) {
            process(j)
        }
        println("Non-zero values: ${weights.count { it > 0f }}/${weights.size}")
    }
    return seg
}
