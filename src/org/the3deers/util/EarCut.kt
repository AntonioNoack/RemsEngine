package org.the3deers.util

import me.anno.utils.structures.arrays.IntArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * from the3deers.org, a derivative work from https://github.com/mapbox/earcut
 * converted to Kotlin by Antonio Noack
 * (copyright (https://github.com/mapbox/earcut/blob/main/LICENSE) removed, because it's like spam for programmers)
 */
object EarCut {

    @JvmStatic
    fun earcut(data: FloatArray, dim: Int): IntArrayList? {

        val outerLen = data.size
        val outerNode = linkedList(data, 0, outerLen, dim, true)
        if (outerNode == null || outerNode.next === outerNode.prev) return null
        val triangles = IntArrayList(max(data.size, 16))
        var minX = 0f
        var minY = 0f
        var maxX: Float
        var maxY: Float
        var x: Float
        var y: Float
        var invSize = 0f

        // if the shape is not too simple, we'll use z-order curve hash later; calculate polygon bbox
        if (data.size > 80 * dim) {
            maxX = data[0]
            minX = maxX
            maxY = data[1]
            minY = maxY
            var i = dim
            while (i < outerLen) {
                x = data[i]
                y = data[i + 1]
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
                i += dim
            }

            // minX, minY and invSize are later used to transform coords into integers for z-order calculation
            invSize = max(maxX - minX, maxY - minY)
            invSize = if (invSize != 0f) 1f / invSize else 0f
        }
        earcutLinked(outerNode, triangles, dim, minX, minY, invSize, 0)
        return triangles
    }

    @JvmStatic
    @Suppress("unused")
    fun earcut(data: FloatArray, holeIndices: IntArray?, dim: Int): IntArrayList? {
        val hasHoles = holeIndices != null && holeIndices.isNotEmpty()
        val outerLen = if (hasHoles) holeIndices!![0] * dim else data.size
        var outerNode = linkedList(data, 0, outerLen, dim, true)
        if (outerNode == null || outerNode.next === outerNode.prev) return null
        val triangles = IntArrayList(max(data.size, 16))
        var minX = 0f
        var minY = 0f
        var maxX: Float
        var maxY: Float
        var x: Float
        var y: Float
        var invSize = 0f
        if (hasHoles) outerNode = eliminateHoles(data, holeIndices, outerNode, dim)

        // if the shape is not too simple, we'll use z-order curve hash later; calculate polygon bbox
        if (data.size > 80 * dim) {
            maxX = data[0]
            minX = maxX
            maxY = data[1]
            minY = maxY
            var i = dim
            while (i < outerLen) {
                x = data[i]
                y = data[i + 1]
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
                i += dim
            }

            // minX, minY and invSize are later used to transform coords into integers for z-order calculation
            invSize = max(maxX - minX, maxY - minY)
            invSize = if (invSize != 0f) 1f / invSize else 0f
        }
        earcutLinked(outerNode, triangles, dim, minX, minY, invSize, 0)
        return triangles
    }

    /**
     * create a circular doubly linked list from polygon points in the specified winding order
     * */
    @JvmStatic
    private fun linkedList(data: FloatArray, start: Int, end: Int, dim: Int, clockwise: Boolean): EarCutNode? {
        var i: Int
        var last: EarCutNode? = null
        if (clockwise == signedArea(data, start, end, dim) > 0) {
            i = start
            while (i < end) {
                last = insertNode(i, data[i], data[i + 1], last)
                i += dim
            }
        } else {
            i = end - dim
            while (i >= start) {
                last = insertNode(i, data[i], data[i + 1], last)
                i -= dim
            }
        }
        if (last != null && last == last.next) {
            removeNode(last)
            last = last.next
        }
        return last
    }

    /**
     * eliminate collinear or duplicate points
     * */
    @JvmStatic
    private fun filterPoints(start: EarCutNode?, end0: EarCutNode?): EarCutNode? {
        var end = end0
        if (start == null) return null
        if (end == null) end = start
        var p = start
        var again: Boolean
        do {
            again = false
            if (!p!!.steiner && (p == p.next || signedTriangleArea(p.prev!!, p, p.next!!) == 0f)) {
                removeNode(p)
                end = p.prev
                p = end
                if (p === p!!.next) break
                again = true
            } else {
                p = p.next
            }
        } while (again || p !== end)
        return end
    }

    /**
     * main ear slicing loop, which triangulates a polygon (given as a list)
     * */
    @JvmStatic
    private fun earcutLinked(
        ear0: EarCutNode?,
        triangles: IntArrayList,
        dim: Int,
        minX: Float,
        minY: Float,
        invSize: Float,
        pass: Int
    ) {
        var ear: EarCutNode? = ear0 ?: return

        // interlink polygon nodes in z-order
        if (pass == 0 && invSize != 0f) indexCurve(ear!!, minX, minY, invSize)
        var stop = ear
        var prev: EarCutNode?
        var next: EarCutNode?

        // iterate through ears, slicing them one by one
        while (ear!!.prev !== ear!!.next) {
            prev = ear!!.prev
            next = ear.next
            if (if (invSize != 0f) isEarHashed(ear, minX, minY, invSize) else isEar(ear)) {
                // cut off the triangle
                triangles.add(prev!!.i / dim)
                triangles.add(ear.i / dim)
                triangles.add(next!!.i / dim)
                removeNode(ear)

                // skipping the next vertex leads to less sliver triangles
                ear = next.next
                stop = next.next
                continue
            }
            ear = next

            // if we looped through the whole remaining polygon and can't find any more ears
            if (ear === stop) {
                // try filtering points and slicing again
                when (pass) {
                    0 -> earcutLinked(filterPoints(ear, null), triangles, dim, minX, minY, invSize, 1)
                    // if this didn't work, try curing all small self-intersections locally
                    1 -> {
                        ear = cureLocalIntersections(filterPoints(ear, null), triangles, dim)
                        earcutLinked(ear, triangles, dim, minX, minY, invSize, 2)
                        // as a last resort, try splitting the remaining polygon into two
                    }
                    2 -> splitEarcut(ear!!, triangles, dim, minX, minY, invSize)
                }
                break
            }
        }
    }

    /**
     * check whether a polygon node forms a valid ear with adjacent nodes
     * */
    @JvmStatic
    private fun isEar(ear: EarCutNode): Boolean {
        val a = ear.prev!!
        val c = ear.next!!
        if (signedTriangleArea(a, ear, c) >= 0) return false // reflex, can't be an ear
        // now make sure we don't have other points inside the potential ear
        var p = ear.next!!.next
        while (p !== ear.prev) {
            if (pointInTriangle(a.x, a.y, ear.x, ear.y, c.x, c.y, p!!.x, p.y) &&
                signedTriangleArea(p.prev!!, p, p.next!!) >= 0
            ) return false
            p = p.next
        }
        return true
    }

    @JvmStatic
    private fun isEarHashed(ear: EarCutNode, minX: Float, minY: Float, invSize: Float): Boolean {

        val a = ear.prev!!
        val c = ear.next!!
        if (signedTriangleArea(a, ear, c) >= 0) return false // reflex, can't be an ear

        // triangle bounding box; min & max are calculated like this for speed
        val minTX = if (a.x < ear.x) if (a.x < c.x) a.x else c.x else if (ear.x < c.x) ear.x else c.x
        val minTY = if (a.y < ear.y) if (a.y < c.y) a.y else c.y else if (ear.y < c.y) ear.y else c.y
        val maxTX = if (a.x > ear.x) if (a.x > c.x) a.x else c.x else if (ear.x > c.x) ear.x else c.x
        val maxTY = if (a.y > ear.y) if (a.y > c.y) a.y else c.y else if (ear.y > c.y) ear.y else c.y

        // z-order range for the current triangle bounding box;
        val minZ = zOrder(minTX, minTY, minX, minY, invSize)
        val maxZ = zOrder(maxTX, maxTY, minX, minY, invSize)
        var p = ear.prevZ
        var n = ear.nextZ

        // look for points inside the triangle in both directions
        while (p != null && p.z >= minZ && n != null && n.z <= maxZ) {
            if (p !== ear.prev && p !== ear.next &&
                pointInTriangle(a.x, a.y, ear.x, ear.y, c.x, c.y, p.x, p.y) &&
                signedTriangleArea(p.prev!!, p, p.next!!) >= 0
            ) return false
            p = p.prevZ
            if (n !== ear.prev && n !== ear.next &&
                pointInTriangle(a.x, a.y, ear.x, ear.y, c.x, c.y, n.x, n.y) &&
                signedTriangleArea(n.prev!!, n, n.next!!) >= 0
            ) return false
            n = n.nextZ
        }

        // look for remaining points in decreasing z-order
        while (p != null && p.z >= minZ) {
            if (p !== ear.prev && p !== ear.next &&
                pointInTriangle(a.x, a.y, ear.x, ear.y, c.x, c.y, p.x, p.y) &&
                signedTriangleArea(p.prev!!, p, p.next!!) >= 0
            ) return false
            p = p.prevZ
        }

        // look for remaining points in increasing z-order
        while (n != null && n.z <= maxZ) {
            if (n !== ear.prev && n !== ear.next &&
                pointInTriangle(a.x, a.y, ear.x, ear.y, c.x, c.y, n.x, n.y) &&
                signedTriangleArea(n.prev!!, n, n.next!!) >= 0
            ) return false
            n = n.nextZ
        }
        return true
    }

    /**
     * go through all polygon nodes and cure small local self-intersections
     * */
    @JvmStatic
    private fun cureLocalIntersections(start0: EarCutNode?, triangles: IntArrayList, dim: Int): EarCutNode? {
        var start = start0
        var p = start
        do {
            val a = p!!.prev!!
            val b = p.next!!.next!!
            if (a != b && intersects(a, p, p.next!!, b) && locallyInside(a, b) && locallyInside(b, a)) {
                triangles.add(a.i / dim)
                triangles.add(p.i / dim)
                triangles.add(b.i / dim)

                // remove two nodes involved
                removeNode(p)
                removeNode(p.next!!)
                start = b
                p = start
            }
            p = p.next
        } while (p !== start)
        return filterPoints(p, null)
    }

    /**
     * try splitting polygon into two and triangulate them independently
     * */
    @JvmStatic
    private fun splitEarcut(
        start: EarCutNode,
        triangles: IntArrayList,
        dim: Int,
        minX: Float,
        minY: Float,
        invSize: Float
    ) {
        // look for a valid diagonal, that divides the polygon into two
        var an: EarCutNode? = start
        do {
            val a = an!!
            var b = a.next!!.next
            while (b !== a.prev) {
                if (a.i != b!!.i && isValidDiagonal(a, b)) {
                    // split the polygon in two by the diagonal
                    val c = splitPolygon(a, b)

                    // filter collinear points around the cuts
                    val a2 = filterPoints(a, a.next)
                    val c2 = filterPoints(c, c.next)

                    // run earcut on each half
                    earcutLinked(a2, triangles, dim, minX, minY, invSize, 0)
                    earcutLinked(c2, triangles, dim, minX, minY, invSize, 0)
                    return
                }
                b = b.next
            }
            an = a.next
        } while (an !== start)
    }

    /**
     * link every hole into the outer loop, producing a single-ring polygon without holes
     * */
    @JvmStatic
    private fun eliminateHoles(data: FloatArray, holeIndices: IntArray?, outerNode0: EarCutNode, dim: Int): EarCutNode {
        var outerNode: EarCutNode? = outerNode0
        val queue: MutableList<EarCutNode> = ArrayList()
        var start: Int
        var end: Int
        var list: EarCutNode?
        var i = 0
        val len = holeIndices!!.size
        while (i < len) {
            start = holeIndices[i] * dim
            end = if (i < len - 1) holeIndices[i + 1] * dim else data.size
            list = linkedList(data, start, end, dim, false)!!
            if (list === list.next) list.steiner = true
            queue.add(getLeftmost(list))
            i++
        }
        queue.sortWith { a: EarCutNode, b: EarCutNode -> a.x.compareTo(b.x) }

        // process holes from left to right
        i = 0
        while (i < queue.size) {
            eliminateHole(queue[i], outerNode!!)
            outerNode = filterPoints(outerNode, outerNode.next)
            i++
        }
        return outerNode!!
    }

    /**
     * find a bridge between vertices that connects hole with an outer ring and link it
     * */
    @JvmStatic
    private fun eliminateHole(hole: EarCutNode, outerNode0: EarCutNode) {
        val outerNode = findHoleBridge(hole, outerNode0)
        if (outerNode != null) {
            val b = splitPolygon(outerNode, hole)
            // filter collinear points around the cuts
            filterPoints(outerNode, outerNode.next)
            filterPoints(b, b.next)
        }
    }

    /**
     * David Eberly's algorithm for finding a bridge between hole and outer polygon
     * */
    @JvmStatic
    private fun findHoleBridge(hole: EarCutNode, outerNode: EarCutNode): EarCutNode? {
        var p = outerNode
        val hx = hole.x
        val hy = hole.y
        var qx = -Float.MAX_VALUE
        var m: EarCutNode? = null

        // find a segment intersected by a ray from the hole's leftmost point to the left;
        // segment's endpoint with lesser x will be potential connection point
        do {
            if (hy <= p.y && hy >= p.next!!.y && p.next!!.y != p.y) {
                val x = p.x + (hy - p.y) * (p.next!!.x - p.x) / (p.next!!.y - p.y)
                if (x <= hx && x > qx) {
                    qx = x
                    if (x == hx) {
                        if (hy == p.y) return p
                        if (hy == p.next!!.y) return p.next
                    }
                    m = if (p.x < p.next!!.x) p else p.next
                }
            }
            p = p.next!!
        } while (p !== outerNode)
        if (m == null) return null
        if (hx == qx) return m // hole touches outer segment; pick leftmost endpoint

        // look for points inside the triangle of hole point, segment intersection and endpoint;
        // if there are no points found, we have a valid connection;
        // otherwise choose the point of the minimum angle with the ray as connection point
        val stop: EarCutNode = m
        val mx = m.x
        val my = m.y
        var tanMin = Float.MAX_VALUE
        var tan: Float
        p = m
        do {
            if (p.x in mx..hx && hx != p.x &&
                pointInTriangle(if (hy < my) hx else qx, hy, mx, my, if (hy < my) qx else hx, hy, p.x, p.y)
            ) {
                tan = abs(hy - p.y) / (hx - p.x) // tangential
                if (locallyInside(p, hole) &&
                    (tan < tanMin || tan == tanMin && (p.x > m!!.x || p.x == m.x && sectorContainsSector(m, p)))
                ) {
                    m = p
                    tanMin = tan
                }
            }
            p = p.next!!
        } while (p !== stop)
        return m
    }

    /**
     * whether sector in vertex m contains sector in vertex p in the same coordinates
     * */
    @JvmStatic
    private fun sectorContainsSector(m: EarCutNode, p: EarCutNode): Boolean {
        return signedTriangleArea(m.prev!!, m, p.prev!!) < 0 && signedTriangleArea(p.next!!, m, m.next!!) < 0
    }

    /**
     * interlink polygon nodes in z-order
     * */
    @JvmStatic
    private fun indexCurve(start: EarCutNode, minX: Float, minY: Float, invSize: Float) {
        var p: EarCutNode? = start
        do {
            if (p!!.z == -1f) p.z = zOrder(p.x, p.y, minX, minY, invSize)
            p.prevZ = p.prev
            p.nextZ = p.next
            p = p.next
        } while (p !== start)
        p.prevZ!!.nextZ = null
        p.prevZ = null
        sortLinked(p)
    }

    /**
     * Simon Tatham's linked list merge sort algorithm;
     * http://www.chiark.greenend.org.uk/~sgtatham/algorithms/listsort.html
     * */
    @JvmStatic
    private fun sortLinked(list0: EarCutNode?) {
        var list = list0
        var i: Int
        var p: EarCutNode?
        var q: EarCutNode?
        var e: EarCutNode?
        var tail: EarCutNode?
        var numMerges: Int
        var pSize: Int
        var qSize: Int
        var inSize = 1
        do {
            p = list
            list = null
            tail = null
            numMerges = 0
            while (p != null) {
                numMerges++
                q = p
                pSize = 0
                i = 0
                while (i < inSize) {
                    pSize++
                    q = q!!.nextZ
                    if (q == null) break
                    i++
                }
                qSize = inSize
                while (pSize > 0 || qSize > 0 && q != null) {
                    if (pSize != 0 && (qSize == 0 || q == null || p!!.z <= q.z)) {
                        e = p
                        p = p!!.nextZ
                        pSize--
                    } else {
                        e = q
                        q = q!!.nextZ
                        qSize--
                    }
                    if (tail != null) tail.nextZ = e else list = e
                    e!!.prevZ = tail
                    tail = e
                }
                p = q
            }
            tail!!.nextZ = null
            inSize *= 2
        } while (numMerges > 1)
    }

    /**
     * z-order of a point given coords and inverse of the longer side of data bounding box
     * */
    @JvmStatic
    fun zOrder(x0: Float, y0: Float, minX: Float, minY: Float, invSize: Float): Float {
        // coords are transformed into non-negative 15-bit integer range
        var x = (32767 * (x0 - minX) * invSize).toInt()
        var y = (32767 * (y0 - minY) * invSize).toInt()
        x = x or (x shl 8) and 0x00FF00FF
        x = x or (x shl 4) and 0x0F0F0F0F
        x = x or (x shl 2) and 0x33333333
        x = x or (x shl 1) and 0x55555555
        y = y or (y shl 8) and 0x00FF00FF
        y = y or (y shl 4) and 0x0F0F0F0F
        y = y or (y shl 2) and 0x33333333
        y = y or (y shl 1) and 0x55555555
        return (x or (y shl 1)).toFloat()
    }

    /**
     * find the leftmost node of a polygon ring
     * */
    @JvmStatic
    private fun getLeftmost(start: EarCutNode): EarCutNode {
        var p = start
        var leftmost = start
        do {
            if (p.x < leftmost.x || p.x == leftmost.x && p.y < leftmost.y) leftmost = p
            p = p.next!!
        } while (p !== start)
        return leftmost
    }

    /**
     * check if a point lies within a triangle
     * */
    @JvmStatic
    fun pointInTriangle(
        ax: Float, ay: Float,
        bx: Float, by: Float,
        cx: Float, cy: Float,
        px: Float, py: Float
    ): Boolean {
        return (cx - px) * (ay - py) - (ax - px) * (cy - py) >= 0f &&
                (ax - px) * (by - py) - (bx - px) * (ay - py) >= 0f &&
                (bx - px) * (cy - py) - (cx - px) * (by - py) >= 0f
    }

    /**
     * check if a diagonal between two polygon nodes is valid (lies in polygon interior)
     * */
    @JvmStatic
    private fun isValidDiagonal(a: EarCutNode, b: EarCutNode): Boolean {
        return a.next!!.i != b.i && a.prev!!.i != b.i &&
                !intersectsPolygon(a, b) && // doesn't intersect other edges
                (locallyInside(a, b) && locallyInside(b, a) && middleInside(a, b) && // locally visible
                        // does not create opposite-facing sectors
                        (signedTriangleArea(a.prev!!, a, b.prev!!) != 0f || signedTriangleArea(a, b.prev!!, b) != 0f) ||
                        a == b && signedTriangleArea(a.prev!!, a, a.next!!) > 0 &&
                        signedTriangleArea(b.prev!!, b, b.next!!) > 0) // special zero-length case
    }

    @JvmStatic
    private fun signedTriangleArea(p: EarCutNode, q: EarCutNode, r: EarCutNode): Float {
        return (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y)
    }

    /**
     * check if two segments intersect
     * */
    @JvmStatic
    private fun intersects(p1: EarCutNode, q1: EarCutNode, p2: EarCutNode, q2: EarCutNode): Boolean {
        val o1 = sign(signedTriangleArea(p1, q1, p2))
        val o2 = sign(signedTriangleArea(p1, q1, q2))
        val o3 = sign(signedTriangleArea(p2, q2, p1))
        val o4 = sign(signedTriangleArea(p2, q2, q1))
        if (o1 != o2 && o3 != o4) return true // general case
        if (o1 == 0 && onSegment(p1, p2, q1)) return true // p1, q1 and p2 are collinear and p2 lies on p1q1
        if (o2 == 0 && onSegment(p1, q2, q1)) return true // p1, q1 and q2 are collinear and q2 lies on p1q1
        if (o3 == 0 && onSegment(p2, p1, q2)) return true // p2, q2 and p1 are collinear and p1 lies on p2q2
        return o4 == 0 && onSegment(p2, q1, q2) // p2, q2 and q1 are collinear and q1 lies on p2q2
    }

    /**
     * for collinear points p, q, r, check if point q lies on segment pr
     * */
    @JvmStatic
    private fun onSegment(p: EarCutNode, q: EarCutNode, r: EarCutNode): Boolean {
        return q.x <= max(p.x, r.x) &&
                q.x >= min(p.x, r.x) &&
                q.y <= max(p.y, r.y) &&
                q.y >= min(p.y, r.y)
    }

    @JvmStatic
    private fun sign(num: Float): Int {
        return if (num > 0) 1 else if (num < 0) -1 else 0
    }

    /**
     * check if a polygon diagonal intersects any polygon segments
     * */
    @JvmStatic
    private fun intersectsPolygon(a: EarCutNode, b: EarCutNode): Boolean {
        var p = a
        do {
            if (p.i != a.i && p.next!!.i != a.i && p.i != b.i && p.next!!.i != b.i &&
                intersects(p, p.next!!, a, b)
            ) return true
            p = p.next!!
        } while (p !== a)
        return false
    }

    /**
     * check if a polygon diagonal is locally inside the polygon
     * */
    @JvmStatic
    private fun locallyInside(a: EarCutNode, b: EarCutNode): Boolean {
        return if (signedTriangleArea(a.prev!!, a, a.next!!) < 0)
            signedTriangleArea(a, b, a.next!!) >= 0f && signedTriangleArea(a, a.prev!!, b) >= 0f
        else
            signedTriangleArea(a, b, a.prev!!) < 0f || signedTriangleArea(a, a.next!!, b) < 0f
    }

    /**
     * check if the middle point of a polygon diagonal is inside the polygon
     * */
    @JvmStatic
    private fun middleInside(a: EarCutNode, b: EarCutNode): Boolean {
        var p = a
        var inside = false
        val px = (a.x + b.x) * 0.5f
        val py = (a.y + b.y) * 0.5f
        do {
            if (p.y > py != p.next!!.y > py && p.next!!.y != p.y &&
                px < (p.next!!.x - p.x) * (py - p.y) / (p.next!!.y - p.y) + p.x
            ) inside = !inside
            p = p.next!!
        } while (p !== a)
        return inside
    }

    // link two polygon vertices with a bridge; if the vertices belong to the same ring, it splits polygon into two;
    // if one belongs to the outer ring and another to a hole, it merges it into a single ring
    @JvmStatic
    private fun splitPolygon(a: EarCutNode, b: EarCutNode): EarCutNode {
        val a2 = EarCutNode(a.i, a.x, a.y)
        val b2 = EarCutNode(b.i, b.x, b.y)
        val an = a.next!!
        val bp = b.prev!!
        a.next = b
        b.prev = a
        a2.next = an
        an.prev = a2
        b2.next = a2
        a2.prev = b2
        bp.next = b2
        b2.prev = bp
        return b2
    }

    /**
     * create a node and optionally link it with previous one (in a circular doubly linked list)
     * */
    @JvmStatic
    private fun insertNode(i: Int, x: Float, y: Float, last: EarCutNode?): EarCutNode {
        val p = EarCutNode(i, x, y)
        if (last == null) {
            p.prev = p
            p.next = p
        } else {
            p.next = last.next
            p.prev = last
            last.next!!.prev = p
            last.next = p
        }
        return p
    }

    @JvmStatic
    private fun removeNode(p: EarCutNode) {
        p.next!!.prev = p.prev
        p.prev!!.next = p.next
        if (p.prevZ != null) p.prevZ!!.nextZ = p.nextZ
        if (p.nextZ != null) p.nextZ!!.prevZ = p.prevZ
    }

    @JvmStatic
    private fun signedArea(data: FloatArray, start: Int, end: Int, dim: Int): Float {
        var sum = 0f
        var i = start
        var j = end - dim
        while (i < end) {
            sum += (data[j] - data[i]) * (data[i + 1] + data[j + 1])
            j = i
            i += dim
        }
        return sum
    }

    class EarCutNode(
        // vertex index in coordinates array
        var i: Int,
        // vertex coordinates
        var x: Float, var y: Float
    ) {

        // z-order curve value
        var z = -1f

        // indicates whether this is a steiner point
        var steiner: Boolean = false

        // previous and next vertex nodes in a polygon ring
        var prev: EarCutNode? = null
        var next: EarCutNode? = null

        // previous and next nodes in z-order
        var nextZ: EarCutNode? = null
        var prevZ: EarCutNode? = null

        override fun equals(other: Any?): Boolean {
            return other === this || (other is EarCutNode && other.x == x && other.y == y)
        }

        override fun hashCode() = x.hashCode() * 31 + y.hashCode()

    }
}