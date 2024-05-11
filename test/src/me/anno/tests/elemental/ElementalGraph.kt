package me.anno.tests.elemental

import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTexts
import me.anno.io.config.ConfigBasics.cacheFolder
import me.anno.io.files.Reference.getReference
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.pow
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.withAlpha
import org.joml.Matrix3x2f
import org.joml.Vector2f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Element(val name: String, val id: Int) {
    val inputs = HashSet<Element>()
    val outputs = HashSet<Element>()
    var rowY = -1
    var zoneMin = 0
    var zoneMax = 0
    var minColX = 0
    var maxColX = 0
    override fun toString() = name
}

val elements = arrayOfNulls<Element>(1000 * 80)

fun readCached(path: String, id: String): String {
    val cf = cacheFolder.getChild("${path.hashCode()}-$id.bin")
    if (cf.exists && abs(System.currentTimeMillis() - cf.lastModified) < 24 * 3600 * 1000L) {// reload once every 24 hours
        return cf.readTextSync()
    }
    val text = getReference(path)
        .readTextSync()
    cf.writeText(text)
    return text
}

fun main() {

    readCached("https://api.phychi.com/elemental/?l3", "list")
        .split('\n')
        .forEach {
            val c = it.split(':')
            if (c.size >= 3) { // c.size will be 4
                val id = c[0].toInt()
                if (id in elements.indices) {
                    // val group = c[1].toInt()
                    val name = c[2]
                    elements[id] = Element(name, id)
                } else println("Element index out of bounds: $it")
            }
        }

    for (groupId in 0 until 33) {
        readCached("https://api.phychi.com/elemental/?qgr=$groupId&sid=0", "$groupId")
            .split(';')
            .forEach {
                val c = it.split(',')
                if (c.size >= 3) { // size will be 3
                    val ai = c[0].toInt()
                    val bi = c[1].toInt()
                    val ri = c[2].toInt()
                    if (ai == ri || bi == ri ||
                        ai !in elements.indices ||
                        bi !in elements.indices ||
                        ri !in elements.indices
                    ) {
                        // we don't care
                    } else {
                        val a = elements[ai]
                        val b = elements[bi]
                        val r = elements[ri]
                        if (a != null && b != null && r != null) {
                            if (a != r) {
                                a.outputs.add(r)
                                r.inputs.add(a)
                            }
                            if (b != r && b != a) {
                                b.outputs.add(r)
                                r.inputs.add(b)
                            }
                        }
                    }
                }
            }
    }

    val nodes = ArrayList<Element>(elements.size)
    val canBeReached = HashSet<Element>(elements.size)
    val remaining = ArrayList<Element>(elements.size)

    for (i in 1..4) {
        val element = elements[i]!!
        canBeReached.add(element)
        remaining.add(element)
    }
    while (remaining.isNotEmpty()/* && nodes.size < 1000*/) {
        val element = remaining.removeAt(remaining.lastIndex)
        nodes.add(element)
        for (output in element.outputs) {
            if (canBeReached.add(output)) {
                remaining.add(output)
            }
        }
    }

    nodes.sortBy(Element::id)
    canBeReached.retainAll(nodes.toSet())

    println("${nodes.size} elements reachable out of ${elements.count { it != null }}")

    // simplify graph
    // view-source:http://www.biofabric.org/gallery/pages/SuperQuickBioFabric.html

    val compareNodes = Comparator<Element> { a, b ->
        val degree = (b.inputs.size + b.outputs.size).compareTo(a.inputs.size + a.outputs.size)
        if (degree != 0) degree // large nodes first
        else if (min(a.id, b.id) < 100) a.id.compareTo(b.id) // old nodes first, for the very first ones
        else b.name.compareTo(a.name) // why reverse?
    }

    // sort graph
    nodes.sortWith(compareNodes)

    fun orderKids(myNode: Element, currVal0: Int): Int {
        // consider inputs as well?
        var currVal = currVal0
        val neighbors = myNode.outputs
            .filter { it in canBeReached }
            .sortedWith(compareNodes)
        var toCheck = 0
        for (checkNode in neighbors) {
            if (checkNode.rowY == -1) {
                checkNode.rowY = currVal++
                toCheck++
            }
        }
        val endVal = currVal
        if (toCheck > 0) {
            for (checkNode in neighbors) {
                if (checkNode.rowY in currVal0 until endVal) {
                    currVal = orderKids(checkNode, currVal)
                }
            }
        }
        return currVal
    }

    val start = nodes.first()
    start.rowY = 0
    orderKids(start, 1)

    // sort links
    class Guy(val min: Int, val max: Int, val index: Int) : Comparable<Guy> {
        override fun compareTo(other: Guy): Int {
            val min1 = min.compareTo(other.min)
            val max1 = max.compareTo(other.max)
            return min1.shl(1) + max1
        }
    }

    class Link(val input: Element, val output: Element, var colX: Int)

    val links = nodes.flatMap { input ->
        input.outputs
            .filter { it in canBeReached }
            .map { Link(input, it, -1) }
    }
    val sortingHat = ArrayList<Guy>(links.size)
    for (link in links) {
        val a = link.input.rowY
        val b = link.output.rowY
        sortingHat.add(Guy(min(a, b), max(a, b), sortingHat.size))
    }

    sortingHat.sort()

    val nodeByRow = arrayOfNulls<Element>(nodes.size)
    for (i in nodes.indices.reversed()) {
        val node = nodes[i]
        if (node !in canBeReached) throw IllegalStateException("$node cannot be reached")
        if (node.rowY < 0) throw IllegalStateException("$node cannot be constructed")
        if (node.rowY > nodes.size) throw IllegalStateException("$node,${node.rowY} is out of bounds!, ${nodes.size}")
        nodeByRow[node.rowY] = node
    }

    var lastGuy: Guy? = null
    // var prevJ = 0
    for (i in sortingHat.indices) {
        val guy = sortingHat[i]
        val useLink = links[guy.index]
        useLink.colX = i
        if (lastGuy != null && guy.min > lastGuy.min) {
            got@ for (j in i - 1 downTo 0) {
                // find last entry, where hat.min < lastGuy.min
                // hat is sorted by min
                // this can be solved with binary search
                // at the same time, lastGuy is sorted as well ->
                // this can be computed in O(1)...
                // I think, this is already close to O(1) :)
                if (sortingHat[j].min < lastGuy.min || j == 0) {
                    val chkNode = nodeByRow[lastGuy.min] ?: continue
                    // if (prevJ != j) throw IllegalStateException("Assumption was wrong, $i -> $prevJ != $j")
                    chkNode.zoneMin = j
                    chkNode.zoneMax = i
                    break@got
                }
            }
        }
        /*if (lastGuy != null && guy.min > lastGuy.min) {
            prevJ = i // lastGuy.indexIn(sortedHat)
        }*/
        lastGuy = guy
    }

    // bound nodes
    for (node in nodes) {
        node.minColX = Int.MAX_VALUE
        node.maxColX = Int.MIN_VALUE
    }
    for (link in links) {
        link.input.minColX = min(link.input.minColX, link.colX)
        link.input.maxColX = max(link.input.maxColX, link.colX)
        link.output.minColX = min(link.output.minColX, link.colX)
        link.output.maxColX = max(link.output.maxColX, link.colX)
    }

    // draw results

    val transform = Matrix3x2f()
    val position = Vector2f()
    var scale = 25f
    val p0 = Vector2f()
    val p1 = Vector2f()

    val min = Vector2f()
    val max = Vector2f()

    val inverse = Matrix3x2f()

    val nodesByX = nodes.sortedBy { it.minColX }

    // helps with small graphs, but not with large ones
    /*val numColors = 16
    val colors = IntArray(numColors) {
        HSLuv.toRGB(Vector3f(it.toFloat() / numColors, 1f, 0.7f)).toRGB()
    }*/

    testDrawing("Elemental Graph") {

        it.allowLeft = true

        // controls
        position.add(it.mx, it.my)
        it.mx = 0f
        it.my = 0f

        val factor = pow(1.01f, it.mz)
        position.mul(factor)

        scale *= factor
        it.mz = 0f

        it.clear()

        transform.identity()
        transform.translate(position)
        transform.scale(scale)

        transform.invert(inverse)
        inverse.transformPosition(min.set(it.x.toFloat(), it.y.toFloat()))
        inverse.transformPosition(max.set((it.x + it.width).toFloat(), (it.y + it.height).toFloat()))

        // draw graph
        val size = scale * 0.5f
        val halfSize = size * 0.5f
        val halfSize2 = 0.25f
        val dotAlpha = clamp(size - 1f)
        val dotColor = (-1).withAlpha(dotAlpha)

        val lineColor = mixARGB(-1, it.backgroundColor, 0.5f)

        // draw vertical lines
        for (i in links.indices) {
            val link = links[i]

            val x = link.colX.toFloat()
            val y2 = link.input.rowY.toFloat()
            val y3 = link.output.rowY.toFloat()

            val y0 = min(y2, y3)
            val y1 = max(y2, y3)

            if (x < min.x || x > max.x) continue
            if (y1 < min.y || y0 > max.y) continue

            transform.transformPosition(p0.set(x, y0))
            transform.transformPosition(p1.set(x, y1))

            DrawRectangles.drawRect(
                p0.x.toInt(),
                p0.y.toInt(), 1,
                (p1.y - p0.y).toInt(),
                lineColor
            )
        }

        // draw horizontal lines
        for (i in nodes.indices) {
            val node = nodes[i]

            val y = node.rowY.toFloat()
            if (y < min.y || y >= max.y) continue

            val x0 = node.minColX.toFloat()
            val x1 = node.maxColX.toFloat()
            if (x0 > max.x || x1 < min.x && x1 >= x0) continue

            transform.transformPosition(p0.set(x0, y))
            transform.transformPosition(p1.set(x1, y))

            DrawRectangles.drawRect(
                p0.x.toInt(), p0.y.toInt(),
                (p1.x - p0.x).toInt(), 1,
                lineColor
            )
        }

        // var prevNode: Element? = null
        val nameLengthFactor = DrawTexts.monospaceFont.sampleWidth / scale
        for (node in nodesByX) {

            // val prevNode1 = prevNode
            // prevNode = node

            // x = col
            // y = row

            val y = node.rowY.toFloat()
            // 1.5f on the right was for zone labels
            if (y + 0.5f < min.y || y - 0.5f >= max.y) continue

            val x0 = node.minColX.toFloat()
            val x1 = node.maxColX.toFloat()
            val nameLength = node.name.length * nameLengthFactor + 1f // 1 is for offset
            // 0.5f is for box
            if (x0 - halfSize2 > max.x || x1 + nameLength < min.x) continue

            transform.transformPosition(p0.set(x0, y))
            transform.transformPosition(p1.set(x1, y))

            DrawRectangles.drawRect(p0.x - halfSize, p0.y - halfSize, size, size, dotColor)
            DrawRectangles.drawRect(p1.x - halfSize, p1.y - halfSize, size, size, dotColor)

            DrawTexts.drawSimpleTextCharByChar(
                (p1.x + 1.5f * size).toInt(), p0.y.toInt(), 1, node.name,
                AxisAlignment.MIN,
                AxisAlignment.CENTER
            )

            // zone labels
            /*val zoneMin = max((prevNode1?.zoneMax ?: -1) + 1, node.zoneMin)
            val zoneMax = node.zoneMax
            if (zoneMax >= zoneMin + nameLength) {

                val cx = (zoneMin + zoneMax) * 0.5f
                val cy = (node.rowY - 1f)

                transform.transformPosition(p0.set(cx, cy))

                DrawTexts.drawSimpleTextCharByChar(
                    p0.x.toInt(), p0.y.toInt(), 1, node.name,
                    AxisAlignment.MAX,
                    AxisAlignment.CENTER
                )
            }*/
        }


    }
}