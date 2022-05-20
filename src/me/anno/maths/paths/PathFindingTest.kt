package me.anno.maths.paths

import me.anno.Engine
import me.anno.image.raw.BIImage
import me.anno.maths.Maths
import me.anno.maths.Maths.length
import me.anno.ui.base.DefaultRenderingHints.prepareGraphics
import me.anno.utils.LOGGER
import me.anno.utils.OS
import me.anno.utils.types.AABBs.avgX
import me.anno.utils.types.AABBs.avgY
import me.anno.utils.types.AABBs.deltaX
import me.anno.utils.types.AABBs.deltaY
import org.joml.AABBf
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.round
import kotlin.random.Random

val enableCrossLinks = false

class TestNode(var x: Float, var y: Float, val i: Int, val j: Int, val id: Int) {
    var distToEnd = 0.0
    var links = emptyList<Link>()
    override fun toString() = "$id"
}

class Link(val to: TestNode, var dist: Double)

class TestGraph(
    val nodes: Array<TestNode>,
    val disabled: Set<Int>,
    val extra: HashMap<Int, ArrayList<Int>>
) {
    fun connect(a: Int, b: Int) {
        extra[a] = extra[a] ?: arrayListOf()
        extra[b] = extra[b] ?: arrayListOf()
        extra[a]!! += b
        extra[b]!! += a
    }
}

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

fun forward(graph: TestGraph, sx: Int, sy: Int, end: TestNode) =
    { from: TestNode, callback: (TestNode, Double, Double) -> Unit ->

        val i = from.i
        val j = from.j
        val id = from.id

        fun callback1(otherId: Int) {
            val to = graph.nodes[otherId]
            if (to.id !in graph.disabled)
                callback(to, distance(from, to), distance(to, end))
        }

        for (extra in graph.extra[id] ?: emptyList()) {
            callback1(extra)
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
            val to = graph.nodes[otherId]
            if (to.id !in graph.disabled) callback(to, distance(from, to))
        }

        for (extra in graph.extra[id] ?: emptyList()) {
            callback1(extra)
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

    // 1) testing the implementation
    // 2) benchmarking it
    // 3) I had a bug, and searched for it; I found it with this code :)

    val sx = 4
    val sy = 4

    val w = (sx + sy) * 50
    val padding = 20
    val startNode = 0
    val endNode = sx * sy - 1

    val disabled = emptySet<Int>()

    val nodes = Array(sx * sy) { id ->
        val i = id % sx
        val j = id / sx
        // change their positions slightly
        TestNode(
            0f, 0f,
            i, j, id
        )
    }

    val graph = TestGraph(nodes, disabled, hashMapOf())

    val start = graph.nodes[startNode]
    val end = graph.nodes[endNode]

    val fw1 = forward(graph, sx, sy)
    for (node in graph.nodes) {
        if (node.id !in graph.disabled) {
            val links = ArrayList<Link>(16)
            fw1(node) { other, dist ->
                links.add(Link(other, dist))
            }
            node.links = links
        }
    }

    fun generate(seed: Long) {
        // test pathfinding
        // generate regular grid of nodes, and connect them
        // find the shortest path
        val cx = (sx - 1) * 0.5f
        val cy = (sy - 1) * 0.5f
        val random = Random(seed)
        val randomness = 5f
        for (id in nodes.indices) {
            val i = id % sx
            val j = id / sx
            // change their positions slightly
            val dx = i - cx
            val dy = j - cy
            val f = sx * 0.3f / (1f + dx * dx + dy * dy)
            val node = nodes[id]
            node.x = round((i + dx * f + random.nextFloat() * randomness) * 3f)
            node.y = round((j + dy * f + random.nextFloat() * randomness) * 3f)
        }
    }

    var seed: Long = 1
    var lastSeed = seed
    var lastTime = Engine.nanoTime
    var path0: List<TestNode>
    var path1: List<TestNode>
    do {
        generate(seed)
        for (id in graph.nodes.indices) {
            val node = graph.nodes[id]
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
        // 1500ns/seed
        path0 = PathFinding.aStar(start, end, distance(start, end), sx * sy, ::forwardV2x1)!!
        path1 = PathFinding.dijkstra(start, end, distance(start, end), sx * sy, ::forwardV2x2)!!
        // 2100ns/seed
        // path0 = PathFinding.aStar(start, end, distance(start, end), sx * sy, forward(graph, sx, sy, end))!!
        // path1 = PathFinding.dijkstra(start, end, distance(start, end), sx * sy, forward(graph, sx, sy))!!
        val distance1 = distance(start, end, path0)
        val distance2 = distance(start, end, path1)
        val time = Engine.nanoTime
        if (time - lastTime > 1e9) {
            LOGGER.info("Checking seed $seed, ${(time - lastTime) / (seed - lastSeed)} ns/seed")
            lastSeed = seed
            lastTime = time
        }
        // this has been fixed; fill no longer occur
        if (distance1 < distance2) {
            LOGGER.debug("Found invalid sample: $seed, $distance1 < $distance2")
            break
        }
        // this will be found a few times
        if (distance1 > distance2) {
            LOGGER.debug("Found sample, where Dijkstra is better")
            break
        }
        seed++
    } while (true)

    val bounds = AABBf()
    for (id in graph.nodes.indices) {
        if (id !in graph.disabled) {
            val node = graph.nodes[id]
            bounds.union(node.x, node.y, 0f)
        }
    }

    // move everything left
    for (id in graph.nodes.indices) {
        val node = graph.nodes[id]
        node.x -= bounds.minX
        node.y -= bounds.minY
    }

    bounds.maxX -= bounds.minX
    bounds.maxY -= bounds.minY
    bounds.minX = 0f
    bounds.minY = 0f

    val pointsByPosition = HashMap<Int, List<Int>>()
    for (id in graph.nodes.indices) {
        if (id !in graph.disabled) {
            val node = graph.nodes[id]
            val index = node.x.toInt() + 256 * node.y.toInt()
            pointsByPosition[index] = (pointsByPosition[index] ?: emptyList()) + id
        }
    }

    for ((index, ids) in pointsByPosition) {
        println("${index.shr(8)},${index.and(255)}: $ids")
    }

    val h = ((w - padding) * bounds.deltaY() / bounds.deltaX() + padding).toInt()

    // display nodes & result
    val image = BufferedImage(w, h, 1)
    val gfx = image.graphics as Graphics2D
    gfx.prepareGraphics(true)
    gfx.font = Font.getFont("Roboto")?.run { deriveFont(h * 0.2f) }

    val border = 2 * padding
    val scale = Maths.min((w - border) / bounds.deltaX(), (h - border) / bounds.deltaY())
    var ox = w / 2 - bounds.avgX() * scale
    var oy = h / 2 - bounds.avgY() * scale

    // draw connections
    gfx.color = Color.GRAY
    val forward1 = forward(graph, sx, sy, end)
    for (from in nodes) {
        if (from.id in disabled) continue
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
        if (node.id in disabled) continue
        val x = (ox + node.x * scale).toInt()
        val y = (oy + node.y * scale).toInt()
        gfx.fillOval(x - 2, y - 2, 5, 5)
        gfx.drawString("${node.id}", x, y - 8)
    }

    fun drawPath(path0: List<TestNode>, name: String, index: Int, color: Color) {
        val path = path0 + end
        var ni = start
        var x0 = (ox + ni.x * scale).toInt()
        var y0 = (oy + ni.y * scale).toInt()
        // draw path
        gfx.color = color
        var distance = 0.0
        for (j in path.indices) {
            val nj = path[j]
            val x1 = (ox + nj.x * scale).toInt()
            val y1 = (oy + nj.y * scale).toInt()
            gfx.drawLine(x0, y0, x1, y1)
            LOGGER.debug("[$ni -> $nj] $distance += ${distance(ni, nj)}")
            distance += distance(ni, nj)
            x0 = x1
            y0 = y1
            ni = nj
        }
        LOGGER.info("Distance: $distance")
        gfx.drawString(name, w / 2, h / 2 + index * gfx.font.size)
    }

    drawPath(path0, "A-Star", 0, Color.GREEN)
    ox += 3; oy -= 3
    drawPath(path1, "Dijkstra", 1, Color.CYAN)
    gfx.dispose()
    BIImage(image).write(OS.desktop.getChild("aStar.png"))
}

