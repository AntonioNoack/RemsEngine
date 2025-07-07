package me.anno.tests.geometry

import me.anno.Time
import me.anno.engine.OfficialExtensions
import me.anno.maths.Maths
import me.anno.maths.Maths.length
import me.anno.maths.paths.PathFinding
import me.anno.jvm.fonts.DefaultRenderingHints.prepareGraphics
import me.anno.jvm.images.BIImage.write
import me.anno.tests.LOGGER
import me.anno.utils.OS
import me.anno.utils.structures.lists.Lists.createArrayList
import org.joml.AABBf
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.random.Random

/**
 * when true: each block has 9 neighbors in 2d, 27 in 3d;
 * when false: each block has 4 neighbors in 3d, 6 in 3d;
 * */
var enableCrossLinks = true

class TestNode(var x: Float, var y: Float, val i: Int, val j: Int, val id: Int) {
    var distToEnd = 0.0
    var links = emptyList<Link>()
    override fun toString() = "$id"
}

class Link(val to: TestNode, var dist: Double)

typealias TestGraph = List<TestNode>

fun distance(a: TestNode, b: TestNode): Double {
    // return (abs(b.x - a.x) + abs(b.y - a.y)).toDouble()
    return length(b.x - a.x, b.y - a.y).toDouble()
}

fun distance(start: TestNode, end: TestNode, path: List<TestNode>?): Double {
    if (path == null) return distance(start, end)
    var sum = distance(start, path[0])
    for (i in 1 until path.size) {
        sum += distance(path[i - 1], path[i])
    }
    sum += distance(path.last(), end)
    return sum
}

fun distance(path: List<TestNode>?): Double {
    if (path == null) return Double.POSITIVE_INFINITY
    var sum = 0.0
    for (i in 1 until path.size) {
        sum += distance(path[i - 1], path[i])
    }
    return sum
}

fun forward(graph: TestGraph, sx: Int, sy: Int, end: TestNode) =
    { from: TestNode, callback: (TestNode, Double, Double) -> Unit ->

        val i = from.i
        val j = from.j
        val id = from.id

        fun callback1(otherId: Int) {
            val to = graph[otherId]
            callback(to, distance(from, to), distance(to, end))
        }

        if (i > 0) callback1(id - 1)
        if (i + 1 < sx) callback1(id + 1)
        if (j > 0) callback1(id - sx)
        if (j + 1 < sy) callback1(id + sx)

        if (enableCrossLinks) {
            if (i > 0 && j > 0) callback1(id - 1 - sx)
            if (i + 1 < sx && j > 0) callback1(id + 1 - sx)
            if (i > 0 && j + 1 < sy) callback1(id - 1 + sx)
            if (i + 1 < sx && j + 1 < sy) callback1(id + 1 + sx)
        }

    }

fun forward(graph: TestGraph, sx: Int, sy: Int) =
    { from: TestNode, callback: (TestNode, Double) -> Unit ->

        val i = from.i
        val j = from.j
        val id = from.id

        fun callback1(otherId: Int) {
            val to = graph[otherId]
            callback(to, distance(from, to))
        }

        if (i > 0) callback1(id - 1)
        if (i + 1 < sx) callback1(id + 1)
        if (j > 0) callback1(id - sx)
        if (j + 1 < sy) callback1(id + sx)

        if (enableCrossLinks) {
            if (i > 0 && j > 0) callback1(id - 1 - sx)
            if (i + 1 < sx && j > 0) callback1(id + 1 - sx)
            if (i > 0 && j + 1 < sy) callback1(id - 1 + sx)
            if (i + 1 < sx && j + 1 < sy) callback1(id + 1 + sx)
        }

    }

fun forwardV2x1(from: TestNode, callback: (TestNode, Double, Double) -> Unit) {
    val links = from.links
    for (i in links.indices) {
        val link = links[i]
        val to = link.to
        callback(to, link.dist, to.distToEnd)
    }
}

fun forwardV2x2(from: TestNode, callback: (TestNode, Double) -> Unit) {
    val links = from.links
    for (i in links.indices) {
        val link = links[i]
        callback(link.to, link.dist)
    }
}

fun main() {

    OfficialExtensions.initForTests()

    // 1) testing the implementation
    // 2) benchmarking it
    // 3) I had a bug, and searched for it; I found it with this code :)

    // A* vs Dijkstra
    //  4 x  4 -> 1.2x faster
    // 16 x 16 -> 1.04x faster
    // 20 x 20 with crosses -> up to 14x faster
    // 50 x 50 with crosses -> up to 7.5x faster
    val sx = 20
    val sy = 20

    val w = (sx + sy) * 50
    val padding = 20
    val startNode = 0
    val endNode = sx * sy - 1

    val nodes = createArrayList(sx * sy) { id ->
        val i = id % sx
        val j = id / sx
        // change their positions slightly
        TestNode(
            0f, 0f,
            i, j, id
        )
    }

    val start = nodes[startNode]
    val end = nodes[endNode]

    val fw1 = forward(nodes, sx, sy)
    for (node in nodes) {
        val links = ArrayList<Link>(16)
        fw1(node) { other, dist ->
            links.add(Link(other, dist))
        }
        node.links = links
    }

    fun generate(seed: Long) {
        // test pathfinding
        // generate regular grid of nodes, and connect them
        // find the shortest path
        val cx = (sx - 1) * 0.5f
        val cy = (sy - 1) * 0.5f
        val random = Random(seed)
        val randomness = 0f
        for (id in nodes.indices) {
            val i = id % sx
            val j = id / sx
            // change their positions slightly
            val dx = i - cx
            val dy = j - cy
            val f = sx * 0.3f / (1f + dx * dx + dy * dy)
            val node = nodes[id]
            node.x = i + dx * f + random.nextFloat() * randomness
            node.y = j + dy * f + random.nextFloat() * randomness
        }
    }

    var seed: Long = 1
    var lastSeed = seed
    var lastTime = Time.nanoTime
    var path0: List<TestNode>
    var path1: List<TestNode>
    var cost0 = 0L
    var cost1 = 0L
    val maxDistance = Double.POSITIVE_INFINITY
    val includeStart = true
    val includeEnd = true
    do {
        generate(seed)
        for (id in nodes.indices) {
            val node = nodes[id]
            node.distToEnd = distance(node, end)
            for (link in node.links) {
                link.dist = distance(node, link.to)
            }
        }
        // functional interfaces
        // 1700ns/seed
        // path0 = PathFinding.aStar(start, end, distance(start, end), sx * sy, ForwardV2x1)!!
        // path1 = PathFinding.dijkstra(start, end, distance(start, end), sx * sy, ForwardV2x2)!!
        // with and without inlining, we get the same performance of 1500ns/seed
        // 1500ns/seed | 32k ns/seed
        val t0 = Time.nanoTime
        path0 = PathFinding.aStar(
            start, end, distance(start, end), maxDistance,
            sx * sy, includeStart, includeEnd, ::forwardV2x1
        )!!
        val t1 = Time.nanoTime
        path1 = PathFinding.dijkstra(
            start, end, distance(start, end), maxDistance,
            sx * sy, includeStart, includeEnd, ::forwardV2x2
        )!!
        val t2 = Time.nanoTime
        cost0 += (t1 - t0)
        cost1 += (t2 - t1)
        // 2100ns/seed | 70k ns/seed
        // path0 = PathFinding.aStar(start, end, distance(start, end), sx * sy, forward(nodes, sx, sy, end))!!
        // path1 = PathFinding.dijkstra(start, end, distance(start, end), sx * sy, forward(nodes, sx, sy))!!
        val distance1 = distance(path0)
        val distance2 = distance(path1)
        val time = Time.nanoTime
        if (time - lastTime > 1e9) {
            LOGGER.info("Checking seed $seed, ${(time - lastTime) / (seed - lastSeed)} ns/seed, A* is ${cost1.toFloat() / cost0}x faster")
            lastSeed = seed
            lastTime = time
            cost0 = 0L
            cost1 = 0L
        }
        // this has been fixed; fill no longer occur
        if (distance1 < distance2) {
            LOGGER.debug("Found invalid sample: $seed, $distance1 < $distance2")
            break
        }
        // this will be found a few times
        if (distance1 > distance2 * 1.0001f) {
            LOGGER.debug("Found sample, where Dijkstra is better")
            break
        }
        seed++
    } while (true)

    val bounds = AABBf()
    for (id in nodes.indices) {
        val node = nodes[id]
        bounds.union(node.x, node.y, 0f)
    }

    // move everything left
    /*for (id in nodes.indices) {
        val node = nodes[id]
        node.x -= bounds.minX
        node.y -= bounds.minY
    }

    bounds.maxX -= bounds.minX
    bounds.maxY -= bounds.minY
    bounds.minX = 0f
    bounds.minY = 0f*/

    /*val pointsByPosition = HashMap<Int, List<Int>>()
    for (id in nodes.indices) {
        val node = nodes[id]
        val index = node.x.toInt() + 256 * node.y.toInt()
        pointsByPosition[index] = (pointsByPosition[index] ?: emptyList()) + id
    }

    for ((index, ids) in pointsByPosition) {
        println("${index.shr(8)},${index.and(255)}: $ids")
    }*/

    val h = ((w - padding) * bounds.deltaY / bounds.deltaX + padding).toInt()

    // display nodes & result
    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val gfx = image.graphics as Graphics2D
    gfx.prepareGraphics(true)
    gfx.font = Font.getFont("Roboto")?.run { deriveFont(h * 0.2f) }

    val border = 2 * padding
    val scale = Maths.min((w - border) / bounds.deltaX, (h - border) / bounds.deltaY)
    var ox = w / 2 - bounds.centerX * scale
    var oy = h / 2 - bounds.centerY * scale

    // draw connections
    gfx.color = Color.GRAY
    val forward1 = forward(nodes, sx, sy, end)
    for (from in nodes) {
        val x0 = (ox + from.x * scale).toInt()
        val y0 = (oy + from.y * scale).toInt()
        forward1(from) { to, distance, _ ->
            val x1 = (ox + to.x * scale).toInt()
            val y1 = (oy + to.y * scale).toInt()
            gfx.drawLine(x0, y0, x1, y1)
            gfx.drawString("${distance.toInt()}", (x0 + x1) / 2, (y0 + y1) / 2 - 8)
        }
    }

    // draw nodes
    gfx.color = Color.WHITE
    for (node in nodes) {
        val x = (ox + node.x * scale).toInt()
        val y = (oy + node.y * scale).toInt()
        gfx.fillOval(x - 2, y - 2, 5, 5)
        gfx.drawString("${node.id}", x, y - 8)
    }

    fun drawPath(path: List<TestNode>, name: String, index: Int, color: Color) {
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
            LOGGER.debug("[{} -> {}] {} += {}", ni, nj, distance, distance(ni, nj))
            distance += distance(ni, nj)
            x0 = x1
            y0 = y1
            ni = nj
        }
        LOGGER.info("Distance: {}", distance)
        gfx.drawString(name, w / 2, h / 2 + index * gfx.font.size)
    }

    drawPath(path0, "A-Star", 0, Color.GREEN)
    ox += 3; oy -= 3
    drawPath(path1, "Dijkstra", 1, Color.CYAN)
    gfx.dispose()
    image.write(OS.desktop.getChild("aStar.png"))
}

