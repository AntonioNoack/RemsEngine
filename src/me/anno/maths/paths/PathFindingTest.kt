package me.anno.maths.paths

import me.anno.image.raw.BIImage
import me.anno.maths.Maths
import me.anno.ui.base.DefaultRenderingHints.prepareGraphics
import me.anno.utils.OS
import me.anno.utils.types.AABBs.avgX
import me.anno.utils.types.AABBs.avgY
import me.anno.utils.types.AABBs.deltaX
import me.anno.utils.types.AABBs.deltaY
import org.joml.AABBf
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.random.Random

fun main() {

    data class TestNode(val x: Float, val y: Float, val i: Int, val j: Int, val id: Int)

    // test pathfinding
    // generate regular grid of nodes, and connect them
    // find the shortest path
    val sx = 20
    val sy = 20
    val cx = (sx - 1) * 0.5f
    val cy = (sy - 1) * 0.5f
    val random = Random(1234L)
    val randomness = 0f
    val nodes = Array(sx * sy) { id ->
        val i = id % sx
        val j = id / sx
        // change their positions slightly
        val dx = i - cx
        val dy = j - cy
        val f = sx * 0.3f / (1f + dx * dx + dy * dy)
        TestNode(
            i + dx * f + random.nextFloat() * randomness,
            j + dy * f + random.nextFloat() * randomness,
            i, j, id
        )
    }

    val indent = 5
    val start = nodes[(1+sx)*indent]
    val end = nodes[nodes.lastIndex-(1+sx)*indent]

    fun distance(a: TestNode, b: TestNode): Double {
        return Maths.length(b.x - a.x, b.y - a.y).toDouble()
    }

    val forward = { from: TestNode, callback: (TestNode, Double, Double) -> Unit ->

        val i = from.i
        val j = from.j
        val id = from.id

        fun callback1(otherId: Int) {
            val to = nodes[otherId]
            callback(to, distance(from, to), distance(to, end))
        }

        if (i > 0) callback1(id - 1)
        if (i + 1 < sx) callback1(id + 1)
        if (j > 0) callback1(id - sx)
        if (j + 1 < sy) callback1(id + sx)

        /*if (i > 0 && j > 0) callback1(id - 1 - sx)
        if (i + 1 < sx && j > 0) callback1(id + 1 - sx)
        if (i > 0 && j + 1 < sy) callback1(id - 1 + sx)
        if (i + 1 < sx && j + 1 < sy) callback1(id + 1 + sx)*/

    }

    val forward2 = { from: TestNode, callback: (TestNode, Double) -> Unit ->

        val i = from.i
        val j = from.j
        val id = from.id

        fun callback1(otherId: Int) {
            val to = nodes[otherId]
            callback(to, distance(from, to))
        }

        if (i > 0) callback1(id - 1)
        if (i + 1 < sx) callback1(id + 1)
        if (j > 0) callback1(id - sx)
        if (j + 1 < sy) callback1(id + sx)

        /*if (i > 0 && j > 0) callback1(id - 1 - sx)
        if (i + 1 < sx && j > 0) callback1(id + 1 - sx)
        if (i > 0 && j + 1 < sy) callback1(id - 1 + sx)
        if (i + 1 < sx && j + 1 < sy) callback1(id + 1 + sx)*/

    }

    // display nodes & result
    val w = 512
    val h = 512
    val padding = 10
    val image = BufferedImage(w, h, 1)
    val gfx = image.graphics as Graphics2D
    gfx.prepareGraphics(true)
    val bounds = AABBf()
    for (node in nodes) {
        bounds.union(node.x, node.y, 0f)
    }

    val border = 2 * padding
    val scale = Maths.min((w - border) / bounds.deltaX(), (h - border) / bounds.deltaY())
    var ox = w / 2 - bounds.avgX() * scale
    var oy = h / 2 - bounds.avgY() * scale

    // draw connections
    gfx.color = Color.GRAY
    for (from in nodes) {
        val x0 = (ox + from.x * scale).toInt()
        val y0 = (oy + from.y * scale).toInt()
        forward(from) { to, _, _ ->
            val x1 = (ox + to.x * scale).toInt()
            val y1 = (oy + to.y * scale).toInt()
            gfx.drawLine(x0, y0, x1, y1)
        }
    }

    // draw nodes
    gfx.color = Color.WHITE
    for (node in nodes) {
        val x = (ox + node.x * scale).toInt()
        val y = (oy + node.y * scale).toInt()
        gfx.fillOval(x - 2, y - 2, 5, 5)
    }

    fun drawPath(path0: List<TestNode>, color: Color) {
        val path = path0 + end
        var ni = start
        var x0 = (ox + ni.x * scale).toInt()
        var y0 = (oy + ni.y * scale).toInt()
        // draw path
        gfx.color = color
        var distance = 0.0
        for (j in 1 until path.size) {
            val nj = path[j]
            val x1 = (ox + nj.x * scale).toInt()
            val y1 = (oy + nj.y * scale).toInt()
            gfx.drawLine(x0, y0, x1, y1)
            distance += distance(ni, nj)
            x0 = x1
            y0 = y1
            ni = nj
        }
        println("distance: $distance")
    }

    val distStartEnd = distance(start, end)
    drawPath(PathFinding.aStar(start, end, distStartEnd, forward)!!, Color.GREEN)
    ox += 3
    oy -= 3
    drawPath(PathFinding.dijkstra(start, end, distStartEnd, forward2)!!, Color.CYAN)
    gfx.dispose()
    BIImage(image).write(OS.desktop.getChild("aStar.png"))

}