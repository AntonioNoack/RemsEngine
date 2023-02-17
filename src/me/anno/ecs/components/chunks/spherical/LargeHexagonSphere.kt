package me.anno.ecs.components.chunks.spherical

import me.anno.ecs.components.chunks.spherical.HexagonSphere.indices
import me.anno.ecs.components.chunks.spherical.HexagonSphere.lineCount
import me.anno.ecs.components.chunks.spherical.HexagonSphere.lineIndices
import me.anno.ecs.components.chunks.spherical.HexagonSphere.pentagonCount
import me.anno.ecs.components.chunks.spherical.HexagonSphere.vertices
import me.anno.ecs.components.chunks.spherical.HexagonSpherePartitioner.pentagonTris
import me.anno.ecs.components.chunks.spherical.HexagonSpherePartitioner.triangleLines
import me.anno.maths.Maths.clamp
import me.anno.utils.types.Vectors.normalToQuaternion
import org.joml.AABBf
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

class LargeHexagonSphere(
    val n: Int, val s: Int,
    val creator: HexagonCreator = HexagonCreator.PooledHexagonCreator
) {

    val t = n / s

    val special0 = lineCount * (n + 1L)
    val special = special0 + pentagonCount
    val perSide = (n * (n + 1L)) shr 1

    val i0 = (n - 1) / 3f
    val j0 = (n - 1.5f) * 0.5f - n / 6f + 0.4f // why 0.4???
    val j0l = n * 0.5f

    // todo findLength() is experimental for n > 500, add data points
    val len = HexagonSphere.findLength(n)

    init {
        if (t * s != n) throw IllegalArgumentException()
        println(len * (n + 1))
    }

    class Triangle(
        val self: LargeHexagonSphere, val index: Int,
        val center: Vector3f, val ab: Vector3f, val ac: Vector3f,
    ) {
        lateinit var abLine: Line
        lateinit var acLine: Line
        lateinit var baLine: Line
        lateinit var caLine: Line
        lateinit var bcLine: Line
        val aabb = AABBf()
        val idx0 = self.special + index * (self.n * (self.n + 1)).shr(1)
        operator fun get(i: Int, j: Int): Hexagon {
            return self.create(center, ab, ac, self.triIdx(idx0, i, j), i - self.i0, j - self.j0)
        }

        operator fun get(i: Int, j: Int, id: Long): Hexagon {
            return self.create(center, ab, ac, id, i - self.i0, j - self.j0)
        }
    }

    class Line(
        val self: LargeHexagonSphere,
        val left: TRef, val right: TRef,
        val center: Vector3f, val ab: Vector3f, val ac: Vector3f,
        val first: Long, val last: Long,
        val step: Long
    ) {
        var firstH: Hexagon? = null
        var lastH: Hexagon? = null
        fun getIndex(index: Int): Long = first + index * step
        operator fun get(index: Int): Hexagon {
            val idx = first + index * step
            if (idx == first && firstH != null) return firstH!!
            else if (idx == last && lastH != null) return lastH!!
            if ((step > 0 && idx > last)) throw IndexOutOfBoundsException()
            if ((step < 0 && idx < last)) throw IndexOutOfBoundsException()
            val pos = self.calcHexPos(center, ab, ac, index - self.j0l, 0f)
            return self.createLineHexagon(pos.normalize(), left, right, index, idx)
        }
    }

    fun isLine(i: Long): Boolean {
        return i < special0
    }

    fun isPentagon(i: Long): Boolean {
        return i in special0 until special
    }

    fun connect(a: Hexagon, bi: Long) {
        val idx = a.neighborIds.indexOf(bi)
        if (idx < 0) {
            val idx1 = a.neighborIds.indexOf(-1)
            if (idx1 < 0) throw IllegalStateException(
                "Cannot connect ${a.index} to $bi, because first is full; " +
                        "${a.neighborIds.joinToString()}!"
            )
            a.neighborIds[idx1] = bi
        }
    }

    fun find(id: Long): Hexagon {
        return when {
            id < special0 -> {
                val n1 = (n + 1L)
                val line = lines[(id / n1).toInt() * 2]
                val li = (id % n1).toInt()
                val hex = line[li]
                connectLine(line, hex, li)
                hex
            }
            // connections are lines, and done already :)
            id < special -> pentagons[(id - special0).toInt()]
            else -> {
                val lr = id - special
                val tri = triangles[(lr / perSide).toInt()]
                val li = lr % perSide
                val i = triFindI(li)
                val j = triFindJ(li, i)
                val hex = tri[i, j, id]
                connectTriHex(tri, hex, i, j)
                hex
            }
        }
    }

    val pHalf = -(0.5 + n)
    fun triFindI(li: Long): Int {
        val q = (2 * li).toDouble()
        return (-pHalf - sqrt(pHalf * pHalf - q)).toInt()
    }

    fun triFindJ(li: Long, i: Int): Int {
        return (li - triIdx(0, i, 0)).toInt()
    }

    fun connectLine(line: Line, hex: Hexagon, li: Int) {
        val id = hex.index
        val nei = hex.neighborIds
        val l = line.left
        val r = line.right
        if (li > 0) {
            nei[2] = id - 1
            nei[1] = triIdx(l.tri.idx0, l.mapI(li - 1, 0), l.mapJ(li - 1, 0))
            nei[3] = triIdx(r.tri.idx0, r.mapI(li - 1, 0), r.mapJ(li - 1, 0))
        } // else pentagon / other lines
        if (li < n) {
            nei[5] = id + 1
            nei[0] = triIdx(l.tri.idx0, l.mapI(li, 0), l.mapJ(li, 0))
            nei[4] = triIdx(r.tri.idx0, r.mapI(li, 0), r.mapJ(li, 0))
        } // else pentagon / other lines
    }

    fun connectTriHex(tri: Triangle, hex: Hexagon, i: Int, j: Int) {
        val idx0 = tri.idx0
        val nei = hex.neighborIds
        if (i > 0) {
            nei[2] = triIdx(idx0, i - 1, j) // left
            nei[1] = triIdx(idx0, i - 1, j + 1) // top left
        } else {
            val line = tri.acLine
            nei[2] = line.getIndex(j)
            nei[1] = line.getIndex(j + 1)
        }
        if (j > 0) {
            nei[3] = triIdx(idx0, i, j - 1) // bottom left
            nei[4] = triIdx(idx0, i + 1, j - 1) // bottom right
        } else {
            val line = tri.abLine
            nei[3] = line.getIndex(i)
            nei[4] = line.getIndex(i + 1)
        }
        if (i + j + 1 < n) {
            nei[5] = triIdx(idx0, i + 1, j) // right
            nei[0] = triIdx(idx0, i, j + 1) // top right
        } else {
            val line = tri.bcLine
            nei[5] = line.getIndex(j)
            nei[0] = line.getIndex(j + 1)
        }
    }

    // todo given a hexagon, find its subchunk

    class SubChunk(val center: Vector3f, val tri: Triangle, val i: Int, val j: Int)

    fun findSubChunk(dir: Vector3f): SubChunk {
        var bestDistance = triangles[0].center.distanceSquared(dir)
        var bestI = 0
        for (i in 1 until triangles.size) {
            val dist = triangles[i].center.distanceSquared(dir)
            if (dist < bestDistance) {
                bestDistance = dist
                bestI = i
            }
        }
        val tri = triangles[bestI]
        val i3 = bestI * 3
        val a = vertices[indices[i3]]
        val b = vertices[indices[i3 + 1]]
        val c = vertices[indices[i3 + 2]]
        val uvw = barycentric(a, b, c, dir, Vector3f())
        val i = uvw.x * n
        val j = uvw.y * n
        val ii = i.toInt()
        val ji = j.toInt()
        val cj = clamp((ji) / t, 0, s - 1)
        val ci = clamp((ii + (ji % t) / 2) / t, 0, s - 1 - cj)
        val ni = min(ci * t + t.shr(1), n - 1)
        val nj = min(cj * t + t.shr(1), n - 1 - ni)
        val pos = tri[ni, nj].center
        return SubChunk(pos, tri, ci, cj)
    }

    fun barycentric(a: Vector3f, b: Vector3f, c: Vector3f, p: Vector3f, uvw: Vector3f): Vector3f {
        val v0 = b - a
        val v1 = c - a
        val v2 = p - a
        val d00 = v0.lengthSquared()
        val d01 = v0.dot(v1)
        val d11 = v1.lengthSquared()
        val d20 = v2.dot(v0)
        val d21 = v2.dot(v1)
        val denominator = d00 * d11 - d01 * d01
        uvw.x = (d11 * d20 - d01 * d21) / denominator
        uvw.y = (d00 * d21 - d01 * d20) / denominator
        uvw.z = 1.0f - uvw.x - uvw.y
        return uvw
    }

    fun ensureNeighbors(all: ArrayList<Hexagon>, hexMap: HashMap<Long, Hexagon>, d: Int) {
        // ensure all neighbors
        val size = all.size
        for (hi in all.indices) {
            val hex = all[hi]
            for (i in 0 until hex.corners.size) {
                if (hex.neighbors[i] == null) {
                    val neighborId = hex.neighborIds[i]
                    if (neighborId < 0) continue
                    var neighbor = hexMap[neighborId]
                    if (neighbor == null) {
                        // register
                        neighbor = find(neighborId)
                        hexMap[neighborId] = neighbor
                        all.add(neighbor)
                    }
                    // connect
                    hex.neighbors[i] = neighbor
                }
            }
        }
        // todo remove items, that are done
        if (d > 0) {
            ensureNeighbors(all, hexMap, d - 1)
        }
    }

    val lines = ArrayList<Line>(lineIndices.size)
    val pentagons = Array(pentagonCount) {
        val v = vertices[it]
        creator.create(special + it, v, Array(5) { v })
    }

    fun calcHexPos(center: Vector3f, ab: Vector3f, ac: Vector3f, b0: Float, b1: Float): Vector3f {
        val c0 = b0 * len
        val c1 = b1 * len
        return Vector3f(center)
            .add(ab.x * c0, ab.y * c0, ab.z * c0)
            .add(ac.x * c1, ac.y * c1, ac.z * c1)
    }

    fun create(center: Vector3f, ab: Vector3f, ac: Vector3f, index: Long, b0: Float, b1: Float): Hexagon {
        val pos = calcHexPos(center, ab, ac, b0, b1)
        val hex: Hexagon
        if (creator is HexagonCreator.PooledHexagonCreator) {
            hex = creator.pool.create()
            hex.index = index
            hex.center.set(center)
            for (i in 0 until 6) {
                HexagonSphere.create(pos, ab, ac, i, len, hex.corners[i])
            }
        } else {
            hex = creator.create(index, pos, Array(6) { HexagonSphere.create(pos, ab, ac, it, len) })
        }
        pos.normalize()
        return hex
    }

    fun createLineHexagon(pos: Vector3f, ab: TRef, ba: TRef, i0: Int, index: Long): Hexagon {

        val i0Inv = n - i0

        val j = -1
        val ps00 = calcHexPos(ab.tri.center, ab.tri.ab, ab.tri.ac, ab.mapI(i0, j) - this.i0, ab.mapJ(i0, j) - j0)
        val ps10 = calcHexPos(ba.tri.center, ba.tri.ab, ba.tri.ac, ba.mapI(i0Inv, j) - this.i0, ba.mapJ(i0Inv, j) - j0)

        val corners = Array(6) {
            val a = (it % 6) < 3
            val x = if (a) ab else ba
            val tri = x.tri
            var i = it + x.d
            if (!a) i += 3
            if (i >= 12) i -= 12
            if (i >= 6) i -= 6
            val ps = if (a) ps00 else ps10
            HexagonSphere.create(ps, tri.ab, tri.ac, i, len)
        }

        // if (corners.any { it.distance(pos) > len * 1.2f }) throw IllegalStateException("$pos, $ab,$i0,$index, ${corners.joinToString()}")

        return creator.create(index, pos, corners)
    }

    val triangleCenters = Array(20) { i ->
        val i3 = i * 3
        val ai = indices[i3]
        val bi = indices[i3 + 1]
        val ci = indices[i3 + 2]
        val a = vertices[ai]
        val b = vertices[bi]
        val c = vertices[ci]
        Vector3f(a).add(b).add(c).div(3f).normalize()
    }

    val triangles = Array(20) { i ->

        val i3 = i * 3
        val ai = indices[i3]
        val bi = indices[i3 + 1]
        val ci = indices[i3 + 2]

        val a = vertices[ai]
        val b = vertices[bi]
        val c = vertices[ci]

        val center = triangleCenters[i]
        val ab = Vector3f(b).sub(a).normalize()
        val ac = Vector3f(c).sub(a).normalize()

        val tri = Triangle(this, i, center, ab, ac)
        tri.aabb.setMin(a).setMax(a).union(b).union(c)
        tri

    }

    // find all nearby lines
    fun findLine(l0: Int, l1: Int): Line {
        for (li2 in lines.indices step 2) {
            val p0 = lineIndices[li2]
            val p1 = lineIndices[li2 + 1]
            if (l0 == p0 && l1 == p1) {
                return lines[li2]
            } else if (l0 == p1 && l1 == p0) {
                return lines[li2 + 1]
            }
        }
        // must not happen
        throw IllegalStateException()
    }

    fun findTriangle0(a: Int, b: Int): TRef {
        val ts = triangles
        for (i in 0 until 20) {
            val i3 = i * 3
            val ai = indices[i3]
            val bi = indices[i3 + 1]
            val ci = indices[i3 + 2]
            when {
                ai == a && bi == b -> return TRef(ts[i])
                bi == a && ci == b -> return RRef(n, TRef(ts[i]))
                ci == a && ai == b -> return RRef(n, RRef(n, TRef(ts[i])))
            }
        }
        throw IllegalStateException()
    }

    fun findTriangle(a: Int, b: Int): TRef {
        val ref = findTriangle0(a, b)
        // check left and right
        val d0 = vertices[a].distance(
            calcHexPos(
                ref.tri.center, ref.tri.ab, ref.tri.ac,
                ref.mapI(-1, -1) - i0, ref.mapJ(-1, -1) - j0
            ).normalize()
        )
        val d1 = vertices[b].distance(
            calcHexPos(
                ref.tri.center, ref.tri.ab, ref.tri.ac,
                ref.mapI(n + 1, -1) - i0, ref.mapJ(n + 1, -1) - j0
            ).normalize()
        )
        // println("$d0,$d1,$len")
        if (d0 > 0.1f || d1 > 0.1f) throw IllegalStateException("$a,$b -> ${ref.tri.index}, ${ref.d}, $d0,$d1")
        return ref
    }

    open class TRef(val tri: Triangle, val d: Int = 0) {
        open fun mapI(i: Int, j: Int) = i
        open fun mapJ(i: Int, j: Int) = j
        override fun toString() = "TRef(${tri.index},x$d)"
    }

    class RRef(val n: Int, val ref: TRef) : TRef(ref.tri, ref.d + 2) {
        override fun mapI(i: Int, j: Int) = ref.mapI(n - 1 - (i + j), i)
        override fun mapJ(i: Int, j: Int) = ref.mapJ(n - 1 - (i + j), i)
    }

    init {

        val pointsToLines = Array(12) { ArrayList<Hexagon>(5) }

        // define edges
        for (i in lineIndices.indices step 2) {

            val ai = lineIndices[i]
            val bi = lineIndices[i + 1]

            val a = vertices[ai]
            val b = vertices[bi]

            val ta = findTriangle(ai, bi)
            val tb = findTriangle(bi, ai)

            val i0 = (i.shr(1)) * (n + 1L)
            val i1 = i0 + n
            val factor = 1.070f
            val center = Vector3f(a).add(b).mul(0.5f).normalize(factor)

            val fx = 0.5f
            val ab = Vector3f(b).sub(a).normalize()
            val ac = Vector3f(center).cross(ab).add(ab.x * fx, ab.y * fx, ab.z * fx).normalize()

            val abn = ab.negate(Vector3f())
            val acn = ac.negate(Vector3f())
            val abLine = Line(this, ta, tb, center, ab, ac, i0, i1, +1)
            val baLine = Line(this, tb, ta, center, abn, acn, i1, i0, -1)
            lines.add(abLine)
            lines.add(baLine)

            val hex0 = abLine[0]
            val hex1 = if (n > 0) baLine[0] else hex0

            abLine.firstH = hex0
            abLine.lastH = hex1
            baLine.firstH = hex1
            baLine.lastH = hex0

            /*if (hex0.center.distance(a) > len * 2)
                throw IllegalStateException()
            if (hex1.center.distance(b) > len * 2)
                throw IllegalStateException()

            if (ai == 1) println("[$ai] += ${hex0.center}, ${hex0.corners.joinToString()}")
            if (bi == 1) println("[$bi] += ${hex1.center}, ${hex1.corners.joinToString()}")*/
            pointsToLines[ai].add(hex0)
            pointsToLines[bi].add(hex1)

        }

        fun connect(a: Hexagon, b: Hexagon) {
            val bis = b.neighborIds.indexOf(-1)
            val ais = a.neighborIds.indexOf(-1)
            b.neighbors[bis] = a
            b.neighborIds[bis] = a.index
            a.neighbors[ais] = b
            a.neighborIds[ais] = b.index
        }

        // create all pentagons
        for (i in 0 until pentagonCount) {

            // build coordinate system
            val point = vertices[i]
            val coords = point.normalToQuaternion(Quaternionf())
            val ax = coords.transform(Vector3f(1f, 0f, 0f))
            val az = coords.transform(Vector3f(0f, 0f, 1f))
            val hexagons1 = pointsToLines[i]

            val ax0 = ax.dot(point)
            val az0 = az.dot(point)

            // sort neighbors by their angle
            hexagons1.sortBy {
                val c = it.center
                atan2(ax.dot(c) - ax0, az.dot(c) - az0)
            }

            // create a pentagon
            val pentagon = pentagons[i]
            for (j in 0 until 5) {
                val x0 = hexagons1[j].center
                val target = Vector3f(point).lerp(x0, -0.5f).normalize()
                val x1 = hexagons1[(j + 2) % 5]
                pentagon.corners[j] = x1.corners.minByOrNull { it.distanceSquared(target) }!!
                /*val dist = pentagon.corners[j].distance(target)
                if (dist > len * 2) {
                    for (hex in hexagons1) {
                        println("${hex.center}, ${hex.corners.joinToString()}")
                    }
                    throw IllegalStateException("[$i/$j] $dist > $len*2, $x0 x $target x ${x1.center}")
                }*/
            }

            // add all connections
            var h0 = hexagons1.last()
            for (h1i in hexagons1) {
                connect(pentagon, h1i)
                connect(h0, h1i)
                h0 = h1i
            }
        }

        for (i in triangles.indices) {

            val i3 = i * 3
            val a = indices[i3]
            val b = indices[i3 + 1]
            val c = indices[i3 + 2]
            val tri = triangles[i]

            tri.abLine = findLine(a, b)
            tri.acLine = findLine(a, c)
            tri.baLine = findLine(b, a)
            tri.caLine = findLine(c, a)
            tri.bcLine = findLine(b, c)

        }

    }

    fun triIdx(idx0: Long, i: Int, j: Int): Long {
        if (i !in 0 until n || j !in 0 until n - i)
            throw IndexOutOfBoundsException("$i,$j is out of bounds for $n")
        return idx0 + j + n.toLong() * i - (i * (i - 1L)).shr(1)
    }

    /**
     * group lines onto triangle faces
     * then split each triangle face into s*(s+1)/2 sub-triangles
     * */
    fun querySubChunk(
        triIndex: Int,
        si: Int,
        sj: Int,
    ): ArrayList<Hexagon> {

        if (si + sj >= s || si < 0 || sj < 0) throw IllegalArgumentException("$si,$sj is out of bounds for $s")

        // size could be estimated better
        val group = ArrayList<Hexagon>((t + 1) * t)

        val tri = triangles[triIndex]

        var ab: Line? = null
        var ac: Line? = null
        val ti3 = triIndex * 3
        val a = indices[ti3]
        val b = indices[ti3 + 1]
        for (i in 0 until 2) {
            val li = triangleLines[triIndex * 2 + i]
            if (li < 0) continue
            val l0 = lineIndices[li * 2]
            val l1 = lineIndices[li * 2 + 1]
            if (l0 == a) {
                if (l1 == b) ab = tri.abLine
                else ac = tri.acLine
            } else {
                // reverse
                if (l0 == b) ab = tri.baLine
                else ac = tri.caLine
            }
        }

        // add a pentagon to the left corner
        if (pentagonTris[a] == triIndex) {
            group.add(pentagons[a])
        }

        if (sj == 0 && ab != null) {
            // add all bottom ones
            for (i in si * t until (si + 1) * t) {
                val hex = ab[i]
                group.add(hex)
                connectLine(ab, hex, i)
            }
            if (si == s - 1) { // last one
                val hex = ab[n]
                group.add(hex)
                connectLine(ab, hex, n)
            }
        }

        if (si == 0 && sj == s - 1) {
            // add top region
            val j0 = n - t
            if (ac != null) {
                // add left part
                // one extra for tip
                for (tj in j0..n) {
                    val hex = ac[tj]
                    group.add(hex)
                    connectLine(ac, hex, tj)
                }
            }
            for (ti in 0 until t) {
                for (tj in j0 until n - ti) {
                    val hex = tri[ti, tj]
                    group.add(hex)
                    connectTriHex(tri, hex, ti, tj)
                }
            }
        } else {
            when (si) {
                0 -> {
                    // left
                    val j0 = t * sj
                    for (tj in 0 until t) {
                        val tl = t - (tj + 1).shr(1)
                        for (ti in 0 until tl) {
                            val jj = tj + j0
                            val hex = tri[ti, jj]
                            group.add(hex)
                            connectTriHex(tri, hex, ti, jj)
                        }
                    }
                    if (ac != null) {
                        // add left part
                        for (j in j0 until j0 + t) {
                            val hex = ac[j]
                            group.add(hex)
                            connectLine(ac, hex, j)
                        }
                    }
                }
                s - sj - 1 -> {
                    // right
                    val j0 = t * sj
                    for (tj in 0 until t) {
                        val tl = t - tj.shr(1)
                        val jj = tj + j0
                        for (ti in n - jj - tl until n - jj) {
                            val hex = tri[ti, jj]
                            group.add(hex)
                            connectTriHex(tri, hex, ti, jj)
                        }
                    }
                }
                else -> {
                    // center
                    val j0 = t * sj
                    for (tj in 0 until t) {
                        val i0 = si * t - (tj + 1).shr(1)
                        val jj = tj + j0
                        for (ti in i0 until i0 + t) {
                            val hex = tri[ti, jj]
                            group.add(hex)
                            connectTriHex(tri, hex, ti, jj)
                        }
                    }
                }
            }
        }
        return group
    }

}