package me.anno.maths.chunks.spherical

import me.anno.maths.Maths
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.assertions.assertFail
import me.anno.utils.assertions.assertTrue
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.tuples.IntPair
import me.anno.utils.types.Arrays.rotateRight
import me.anno.utils.types.Booleans.hasFlag
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix3f
import org.joml.Vector3f
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

class HexagonSphere(
    val hexagonsPerSide: Int, val chunkCount: Int,
    val creator: HexagonCreator = HexagonCreator.DefaultHexagonCreator
) {

    companion object {

        private val LOGGER = LogManager.getLogger(HexagonSphere::class)

        const val LINE_COUNT = 30
        const val PENTAGON_COUNT = 12
        const val TRIANGLE_COUNT = 20

        fun findLength(n: Int): Float {
            // todo find this value mathematically
            //  ~ 4/3 / (1+1/(1+n)) / (n+1)
            //  = 4/3 / (n+2)
            return findLength0(n) * baseLength(n)
        }

        fun baseLength(n: Int): Float {
            return (4f / 3f) / (n + 2)
        }

        // magic values, calculated using linear search
        // by comparing the size of the pentagons with their neighbors
        val lengthI = intArrayOf(
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            10, 15, 20, 25, 40, 60, 90, 150, 200, 500,
            2000, 5000, 10_000, 20_000, 50_000, 100_000
        )
        val lengthF = floatArrayOf(
            1f, 0.9522594f, 0.9678919f, 0.9774843f, 0.98317885f, 0.98666704f, 0.9888875f, 0.9903511f, 0.9913437f,
            0.9925203f, 0.9935367f, 0.99372196f, 0.993703f, 0.9934611f, 0.9932019f, 0.9929746f, 0.9927591f,
            0.9926703f, 0.99249905f, 0.9924079f, 0.99238926f, 0.99238306f, 0.99237984f, 0.99237794f, 0.99237734f
        )

        private fun findLength0(n: Int): Float {
            if (n < 0) throw IllegalArgumentException()
            val bi = lengthI.binarySearch(n)
            if (bi >= 0) return lengthF[bi]
            val i0 = min(-bi - 1, lengthI.size - 2)
            val i1 = i0 + 1
            return Maths.mix(
                lengthF[i0],
                lengthF[i1],
                Maths.unmix(ln(lengthI[i0].toFloat()), ln(lengthI[i1].toFloat()), ln(n.toFloat()))
            )
        }

        // Icosphere without subdivisions from Blender = dodecahedron (20 triangle faces, each corner is a pentagon)
        private val indices = intArrayOf(
            // this order was brute-forced for a nice layout for partitionIntoChunks 😅
            0, 1, 2, 1, 0, 5, 0, 2, 3, 0, 3, 4, 0, 4, 5, 1, 5, 10, 1, 6, 2, 2, 7, 3, 3, 8, 4, 5, 4, 9, 1,
            10, 6, 6, 7, 2, 7, 8, 3, 4, 8, 9, 5, 9, 10, 10, 11, 6, 11, 7, 6, 7, 11, 8, 8, 11, 9, 9, 11, 10
        )

        val vertices = run {
            // these values were derived via HexagonSpherePositionTests
            // we could increase their precision to double, if we need it...
            val a = 0.2763932f
            val b = 0.4472136f
            val c = 0.8506508f
            val d = 0.7236068f
            val e = 0.5257311f
            val f = 0.8944272f
            val list = listOf(
                Vector3f(0f, -1f, 0f), Vector3f(d, -b, e),
                Vector3f(-a, -b, c), Vector3f(-f, -b, 0f),
                Vector3f(-a, -b, -c), Vector3f(d, -b, -e),
                Vector3f(a, b, c), Vector3f(-d, b, e),
                Vector3f(-d, b, -e), Vector3f(a, b, -c),
                Vector3f(f, b, 0f), Vector3f(0f, 1f, 0f)
            )
            for (idx in list.indices) {
                list[idx].normalize()
            }
            list
        }

        val lineIndices = intArrayOf(
            0, 1, 1, 2, 0, 5, 0, 2, 2, 3, 0, 3, 3, 4, 0, 4, 4, 5, 1, 5, 5, 10, 1, 6, 2, 7, 3, 8, 4, 9, 1, 10,
            2, 6, 6, 7, 3, 7, 7, 8, 4, 8, 8, 9, 5, 9, 9, 10, 6, 10, 10, 11, 6, 11, 7, 11, 8, 11, 9, 11
        )

        private val hexInLocalCoords = floatArrayOf(1f, 1f, -1f, 2f, -2f, 1f, -1f, -1f, 1f, -2f, 2f, -1f)
        private val triangleLines = intArrayOf(
            0, 3, 9, -1, 5, -1, 7, -1, 2, -1, 15, -1, 1, 11, 4, 12, 6, 13, 8, 22, -1, -1,
            16, 17, 18, 19, 14, 20, 10, -1, 24, 25, 26, 27, -1, -1, 21, 28, 23, 29
        )
        private val pentagonTris = intArrayOf(0, 6, 7, 8, 13, 9, 11, 12, 18, 19, 15, 16)
        private val hexSortOrders0 = // base 8 indices for sorting the hexagons around the pentagons; pre-calculated
            shortArrayOf(794, 17419, 16467, 16467, 16467, 16915, 12372, 16915, 16915, 16915, 17419, 16467)
        private val lineToTriangle = byteArrayOf(
            0, 1, 20, 46, 21, 44, 2, 40, 22, 47, 3, 42, 23, 48, 4, 43, 24, 9, 5, 41, 25, 54, 6, 50, 7, 31, 8, 32, 29,
            53, 10, 45, 51, 26, 11, 36, 52, 27, 12, 57, 13, 28, 33, 58, 14, 49, 34, 59, 55, 30, 15, 39, 56, 35, 17, 16,
            18, 37, 19, 38,
        )
    }

    // if hexagonsPerSide = 0, chunkCount will be 0, too, and then hexagonsPerChunk shall be 0
    val hexagonsPerChunk = hexagonsPerSide / max(chunkCount, 1)

    val special0 = LINE_COUNT * (hexagonsPerSide + 1L)
    val special = special0 + PENTAGON_COUNT
    val perSide = (hexagonsPerSide * (hexagonsPerSide + 1L)) shr 1
    val numHexagons = special + perSide * 20

    val i0 = (hexagonsPerSide - 1) / 3f
    val j0 = (hexagonsPerSide - 1.5f) * 0.5f - hexagonsPerSide / 6f + 0.4f // why 0.4???

    val len = findLength(hexagonsPerSide)
    val lenX3 = len / 3f

    init {
        if (hexagonsPerChunk * chunkCount != hexagonsPerSide) {
            throw IllegalArgumentException("hex/chunk * chunk must be equal to hex")
        }
        LOGGER.debug("HexSphere-Cell-Length: {}", len * (hexagonsPerSide + 1))
    }

    private fun create(pos: Vector3f, ab: Vector3f, ac: Vector3f, i: Int): Vector3f {
        val i2 = i + i
        return create(pos, ab, ac, hexInLocalCoords[i2] * lenX3, hexInLocalCoords[i2 + 1] * lenX3)
    }

    private fun create(pos: Vector3f, ab: Vector3f, ac: Vector3f, d0: Float, d1: Float): Vector3f {
        return Vector3f(
            pos.x + ab.x * d0 + ac.x * d1,
            pos.y + ab.y * d0 + ac.y * d1,
            pos.z + ab.z * d0 + ac.z * d1
        ).normalize()
    }

    fun getChunkCenter(tri: Int, si: Int, sj: Int, dst: Vector3f = Vector3f()): Vector3f {
        return triangles[tri].getChunkCenter(si, sj, dst)
    }

    class Triangle(
        val sphere: HexagonSphere, val index: Int,
        val center: Vector3f, val ab: Vector3f, val ac: Vector3f,
    ) {

        lateinit var abLine: Line
        lateinit var acLine: Line
        lateinit var baLine: Line
        lateinit var caLine: Line
        lateinit var bcLine: Line

        val hexPosMatrix = sphere.calcHexPosMatrix(center, ab, ac)

        val i0: Float
        val di: Float
        val j0: Float
        val dj: Float

        init {
            val t = sphere.hexagonsPerSide
            val tmp = JomlPools.vec3f.create()
            sphere.calcHexPos(hexPosMatrix, 0, 0, tmp)
            sphere.calcChunkUVW(this, tmp, tmp)
            i0 = tmp.x
            j0 = tmp.y
            sphere.calcHexPos(hexPosMatrix, t, 0, tmp)
            sphere.calcChunkUVW(this, tmp, tmp)
            di = t / (tmp.x - i0)
            sphere.calcHexPos(hexPosMatrix, 0, t, tmp)
            sphere.calcChunkUVW(this, tmp, tmp)
            dj = t / (tmp.y - j0)
            JomlPools.vec3f.sub(1)
        }

        val aabb = AABBf()
        val idx0 = sphere.special + index * sphere.perSide
        operator fun get(i: Int, j: Int): Hexagon {
            return get(i, j, sphere.triIdx(idx0, i, j))
        }

        operator fun get(i: Int, j: Int, id: Long): Hexagon {
            return sphere.create(center, ab, ac, id, i - sphere.i0, j - sphere.j0)
        }

        fun getChunkCenter(si: Int, sj: Int, dst: Vector3f = Vector3f()): Vector3f {
            if (si !in 0 until sphere.chunkCount || sj !in 0 until sphere.chunkCount - si) {
                assertFail("$si,$sj !in ${sphere.chunkCount}")
            }

            val t = sphere.hexagonsPerChunk
            val j0 = t * sj
            val tj = t * 0.5f

            val i0 = si * t - (tj + 1) * 0.5f
            val jj = tj + j0
            val ti = i0 + (t - 1) * 0.5f

            return sphere.calcHexPos(center, ab, ac, ti - sphere.i0, jj - sphere.j0, dst).normalize()
        }
    }

    class Line(
        val sphere: HexagonSphere,
        val left: TRef, val right: TRef,
        val first: Long, val last: Long, val step: Long
    ) {
        var firstH: Hexagon? = null
        var lastH: Hexagon? = null
        fun getIndex(index: Int): Long = first + index * step
        operator fun get(index: Int): Hexagon {
            val idx = first + index * step
            if (idx == first && firstH != null) return firstH!!
            else if (idx == last && lastH != null) return lastH!!
            if ((step > 0 && idx > last) || (step < 0 && idx < last)) {
                assertFail("Index out of bounds")
            }
            return sphere.createLineHexagon(left, right, index, idx)
        }
    }

    fun find(id: Long, connect: Boolean = true): Hexagon {
        return when {
            id !in 0 until numHexagons -> {
                assertFail("Id out of bounds: $id !in 0 until $numHexagons")
            }
            id < special0 -> {
                val n1 = (hexagonsPerSide + 1L)
                val line = lines[(id / n1).toInt() * 2]
                val li = (id % n1).toInt()
                val hex = line[li]
                if (connect && li > 0) connectLine(line, hex, li)
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
                if (connect) connectTriHex(tri, hex, i, j)
                hex
            }
        }
    }

    val pHalf = -(0.5 + hexagonsPerSide)
    fun triFindI(li: Long): Int {
        val q = (2 * li).toDouble()
        return (-pHalf - sqrt(pHalf * pHalf - q)).toInt()
    }

    fun triFindJ(li: Long, i: Int): Int {
        return (li - triIdx(0, i, 0)).toInt()
    }

    fun connectLine(line: Line, hex: Hexagon, li: Int) {
        if (li >= hexagonsPerSide) return // theoretically would be defined, but I rotated them for ease of implementation,
        // so they would be a special case; but I don't want to handle it, so I inited them in reverse at the start :)
        val nei = hex.neighborIds
        val l = line.left
        val r = line.right
        val lj = hexagonsPerSide - li
        if (li > 0) {
            nei[2] = line.getIndex(li - 1)
            nei[1] = triIdx(l.tri.idx0, l.mapI(li - 1, 0), l.mapJ(li - 1, 0))
            nei[3] = triIdx(r.tri.idx0, r.mapI(lj, 0), r.mapJ(lj, 0))
        } // else pentagon / other lines
        nei[5] = line.getIndex(li + 1)
        nei[0] = triIdx(l.tri.idx0, l.mapI(li, 0), l.mapJ(li, 0))
        nei[4] = triIdx(r.tri.idx0, r.mapI(lj - 1, 0), r.mapJ(lj - 1, 0))
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
        if (i + j + 1 < hexagonsPerSide) {
            nei[5] = triIdx(idx0, i + 1, j) // right
            nei[0] = triIdx(idx0, i, j + 1) // top right
        } else {
            val line = tri.bcLine
            nei[5] = line.getIndex(j)
            nei[0] = line.getIndex(j + 1)
        }
    }

    /**
     * finds the chunk for a given hexagon; always O(1)
     * */
    fun findChunk(hex: Hexagon): Chunk {
        when (val id = hex.index) {
            in 0L until special0 -> {
                // find triangle with respective line
                val lineIndex = (id / (hexagonsPerSide + 1)).toInt()
                val tri = triangleLines.indexOf(lineIndex).shr(1)
                // then find chunk, which contains this line
                val li = (id % (hexagonsPerSide + 1)).toInt()
                val l0 = lineIndices[lineIndex * 2]
                val l1 = lineIndices[lineIndex * 2 + 1]
                val a = indices[tri * 3]
                val b = indices[tri * 3 + 1]
                val c = indices[tri * 3 + 2]
                val lj = hexagonsPerSide - li
                val sm1 = chunkCount - 1
                return when {
                    l0 == a && l1 == b -> chunk(tri, min(li / hexagonsPerChunk, sm1), 0)
                    l0 == b && l1 == a -> chunk(tri, min(lj / hexagonsPerChunk, sm1), 0)
                    l0 == a && l1 == c -> chunk(tri, 0, min(li / hexagonsPerChunk, sm1))
                    l0 == c && l1 == a -> chunk(tri, 0, min(lj / hexagonsPerChunk, sm1))
                    else -> throw IllegalStateException()
                }
            }
            in special0 until special -> {
                // this is a pentagon: find, which triangle owns us,
                // and then return 0,0, as it will be the owner
                val tri = pentagonTris[(id - special0).toInt()]
                return chunk(tri, 0, 0)
            }
            in special until numHexagons -> {
                val id1 = id - special
                val tri = (id1 / perSide).toInt()
                if (chunkCount == 1) return chunk(tri, 0, 0)
                val li = id1 % perSide
                val i = triFindI(li)
                val j = triFindJ(li, i)
                val sj = j / hexagonsPerChunk // easy
                // si is a bit more complicated; use the end of the left block for comparison
                // works for the tip as well :)
                val i0 = ((j % hexagonsPerChunk) + 1).shr(1)
                val si = (i + i0) / hexagonsPerChunk
                return chunk(tri, si, sj)
            }
            else -> assertFail("Invalid ID")
        }
    }

    fun chunk(tri: Int, i: Int, j: Int): Chunk {
        return Chunk(triangles[tri].getChunkCenter(i, j), tri, i, j)
    }

    data class Chunk(val center: Vector3f, val tri: Int, val si: Int, val sj: Int)

    /**
     * iterative, correct finding algorithm
     * */
    fun findClosestChunk(dir: Vector3f): Chunk {
        return findChunk(findClosestHexagon(dir))
    }

    fun calcChunkUVW(tri: Triangle, dir: Vector3f, dst: Vector3f): Vector3f {
        val i3 = tri.index * 3
        val a = vertices[indices[i3]]
        val b = vertices[indices[i3 + 1]]
        val c = vertices[indices[i3 + 2]]
        dir.div(dir.dot(tri.center), dst)
        return barycentric(a, b, c, dst, dst)
    }

    /**
     * analytical estimation to find chunks
     * */
    fun findChunk(tri: Triangle, dir: Vector3f): Chunk {
        if (hexagonsPerSide == 0 || chunkCount <= 1) {
            return Chunk(tri.center, tri.index, 0, 0)
        }
        val tmp = JomlPools.vec3f.create()
        dir.div(dir.dot(tri.center), tmp)
        val uvw = calcChunkUVW(tri, dir, tmp)
        val ii = findI(tri, uvw.x)
        val ji = findJ(tri, uvw.y)
        JomlPools.vec3f.sub(1)
        val sj = clamp((ji) / hexagonsPerChunk, 0, chunkCount - 1)
        val si = clamp((ii + (ji % hexagonsPerChunk) / 2) / hexagonsPerChunk, 0, chunkCount - 1 - sj)
        val pos = tri.getChunkCenter(si, sj)
        return Chunk(pos, tri.index, si, sj)
    }

    private fun findI(triangle: Triangle, x: Float): Int {
        return ((x - triangle.i0) * triangle.di).roundToInt()
    }

    private fun findJ(triangle: Triangle, y: Float): Int {
        return ((y - triangle.j0) * triangle.dj).roundToInt()
    }

    fun findClosestHexagon(dir: Vector3f): Hexagon {
        assertTrue(dir.isFinite)
        var bestDistance = triangles[0].center.distanceSquared(dir)
        var bestI = 0
        for (i in 1 until triangles.size) {
            val dist = triangles[i].center.distanceSquared(dir)
            if (dist < bestDistance) {
                bestDistance = dist
                bestI = i
            }
        }
        return findClosestHexagon(triangles[bestI], dir)
    }

    fun findClosestHexagon(tri: Triangle, dir: Vector3f): Hexagon {
        val hex0 = if (hexagonsPerSide == 0) {
            pentagons.minByOrNull { it.center.distanceSquared(dir) }
        } else {
            val tmp = JomlPools.vec3f.create()
            dir.div(dir.dot(tri.center), tmp)
            val uvw = calcChunkUVW(tri, dir, tmp)
            JomlPools.vec3f.sub(1)
            val ii = clamp(findI(tri, uvw.x), 0, hexagonsPerSide - 1)
            val ji = clamp(findJ(tri, uvw.y), 0, hexagonsPerSide - 1 - ii)
            val hex = tri[ii, ji]
            connectTriHex(tri, hex, ii, ji)
            hex
        }!!
        return findClosestHexagon(dir, hex0)
    }

    fun findClosestHexagon(dir: Vector3f, hex0: Hexagon): Hexagon {
        var bestHex = hex0
        var bestDist = bestHex.center.distanceSquared(dir)
        search@ while (true) {
            for (id in bestHex.neighborIds) {
                val hex = find(id)
                val dist = hex.center.distanceSquared(dir)
                if (dist < bestDist) {
                    bestHex = hex
                    bestDist = dist
                    continue@search
                }
            }
            break
        }
        return bestHex
    }

    private class Checker(
        var tri: Triangle, val s: Int, val dir: Vector3f,
        val maxAngleCos1: Float, val callback: (Chunk) -> Boolean
    ) {

        private val remaining = ArrayList<IntPair>()
        private val checked = HashSet<IntPair>()

        fun reset() {
            checked.clear()
        }

        fun checkChunk(si0: Int, sj0: Int): Boolean {
            enqueue(si0, sj0)
            while (true) {
                val (si, sj) = remaining.removeLastOrNull()
                    ?: return false // done, no valid entry found
                val center = tri.getChunkCenter(si, sj)
                if (center.angleCos(dir) >= maxAngleCos1) {
                    if (callback(Chunk(center, tri.index, si, sj))) {
                        return true
                    }
                    if (si > 0) {
                        enqueue(si - 1, sj)
                        enqueue(si - 1, sj + 1)
                    }
                    if (sj > 0) {
                        enqueue(si, sj - 1)
                        enqueue(si + 1, sj - 1)
                    }
                    if (si + sj + 1 < s) {
                        enqueue(si, sj + 1)
                        enqueue(si + 1, sj)
                    }
                }
            }
        }

        fun enqueue(si: Int, sj: Int) {
            val key = IntPair(si, sj)
            if (checked.add(key)) {
                remaining.add(key)
            }
        }
    }

    /**
     * iterates over all chunks within a certain angle (on the surface);
     * if any callback returns true, iteration is cancelled, any the method returns true
     * */
    fun queryChunks(dir: Vector3f, angleRadiusRadians: Float, callback: (Chunk) -> Boolean): Boolean {
        if (!dir.isFinite || dir.lengthSquared() < 1e-19f) throw IllegalArgumentException(dir.toString())
        // ~37.4°
        val triangleSelfRadius = 1.002f * triangles.first().run { vertices[indices[0]].angle(center) }
        val chunkRadius = triangleSelfRadius * 1.4f / max(chunkCount, 1)
        val maxAngleCos0 = cos(min(angleRadiusRadians + triangleSelfRadius, PIf))
        val maxAngleCos1 = cos(min(angleRadiusRadians + chunkRadius, PIf))
        val checker = Checker(triangles.first(), chunkCount, dir, maxAngleCos1, callback)
        for (tri in triangles) {
            if (tri.center.angleCos(dir) >= maxAngleCos0) {
                checker.tri = tri
                checker.reset()
                // find the closest chunk to dir
                val sub = findChunk(tri, dir)
                if (checker.checkChunk(sub.si, sub.sj)) {
                    return true
                }
            }
        }
        return false
    }

    fun barycentric(a: Vector3f, b: Vector3f, c: Vector3f, p: Vector3f, dst: Vector3f): Vector3f {
        val pool = JomlPools.vec3f
        val v0 = b.sub(a, pool.create())
        val v1 = c.sub(a, pool.create())
        val v2 = p.sub(a, pool.create())
        val d00 = v0.lengthSquared()
        val d01 = v0.dot(v1)
        val d11 = v1.lengthSquared()
        val d20 = v2.dot(v0)
        val d21 = v2.dot(v1)
        val denominator = d00 * d11 - d01 * d01
        dst.x = (d11 * d20 - d01 * d21) / denominator
        dst.y = (d00 * d21 - d01 * d20) / denominator
        dst.z = 1.0f - dst.x - dst.y
        pool.sub(3)
        return dst
    }

    fun ensureNeighbors(hexList: ArrayList<Hexagon>, hexMap: HashMap<Long, Hexagon>, depth: Int) {
        // ensure all neighbors
        var i0 = 0
        for (di in 0..depth) {
            val i1 = hexList.size
            for (i in i0 until i1) {
                val hex = hexList[i]
                for (j in 0 until hex.neighbors.size) {
                    val neighborId = hex.neighborIds[j]
                    var neighbor = hex.neighbors[j]
                    if (neighbor == null) {
                        neighbor = hexMap[neighborId]
                        if (neighbor == null) {
                            // register
                            neighbor = find(neighborId)
                            hexMap[neighborId] = neighbor
                            hexList.add(neighbor)
                        }
                        // connect
                        hex.neighbors[j] = neighbor
                    } else {
                        // add hexagon, if not already in list
                        val prev = hexMap.put(neighborId, neighbor)
                        if (prev == null) hexList.add(neighbor)
                    }
                }
            }
            i0 = i1
        }
    }

    @Suppress("unused")
    fun ensureNeighbors(hex: Hexagon) {
        // ensure all neighbors
        val neighbors = hex.neighbors
        for (j in neighbors.indices) {
            var neighbor = neighbors[j]
            if (neighbor == null) {
                // register
                neighbor = find(hex.neighborIds[j])
                // connect
                neighbors[j] = neighbor
            }
        }
    }

    val lines = ArrayList<Line>(lineIndices.size)
    val pentagons = createArrayList(PENTAGON_COUNT) { idx ->
        val center = vertices[idx]
        creator.create(special0 + idx, center, createArrayList(5, center))
    }

    private fun calcHexPos(
        center: Vector3f,
        ab: Vector3f,
        ac: Vector3f,
        b0: Float,
        b1: Float,
        dst: Vector3f = Vector3f()
    ): Vector3f {
        val c0 = b0 * len
        val c1 = b1 * len
        return dst.set(center)
            .add(ab.x * c0, ab.y * c0, ab.z * c0)
            .add(ac.x * c1, ac.y * c1, ac.z * c1)
    }

    private fun calcHexPos(
        m: Matrix3f, i: Int, j: Int,
        dst: Vector3f = Vector3f()
    ): Vector3f = calcHexPos(m, i - i0, j - j0, dst)

    private fun calcHexPos(
        m: Matrix3f, b0: Float, b1: Float,
        dst: Vector3f = Vector3f()
    ): Vector3f = m.transform(b0, b1, 1f, dst)

    /**
     * return matrix such that
     * hexPos = result * (b0,b1,1)
     * */
    private fun calcHexPosMatrix(
        center: Vector3f,
        ab: Vector3f,
        ac: Vector3f,
    ): Matrix3f {
        return Matrix3f(ab, ac, center)
            .scale(len, len, 1f)
    }

    private fun create(center: Vector3f, ab: Vector3f, ac: Vector3f, index: Long, b0: Float, b1: Float): Hexagon {
        val pos = calcHexPos(center, ab, ac, b0, b1)
        val hex = creator.create(index, pos, createArrayList(6) { create(pos, ab, ac, it) })
        hex.center.normalize()
        return hex
    }

    private fun createLineHexagon(ab: TRef, ba: TRef, i: Int, index: Long): Hexagon {

        val i0Inv = hexagonsPerSide - i

        val j = -1
        val pos0 = ab.calcHexPos(this, i, j)
        val pos1 = ba.calcHexPos(this, i0Inv, j)

        val corners = createArrayList(6) { idx ->
            val a = (idx % 6) < 3
            val x = if (a) ab else ba
            val tri = x.tri
            var k = idx + x.d
            if (!a) k += 3
            if (k >= 12) k -= 12
            if (k >= 6) k -= 6
            val ps = if (a) pos0 else pos1
            create(ps, tri.ab, tri.ac, k)
        }

        val pos = Vector3f()
        for (k in corners.indices) {
            pos.add(corners[k])
        }
        pos.normalize()
        return creator.create(index, pos, corners)
    }

    val triangleCenters = createArrayList(20) { i ->
        val i3 = i * 3
        val ai = indices[i3]
        val bi = indices[i3 + 1]
        val ci = indices[i3 + 2]
        val a = vertices[ai]
        val b = vertices[bi]
        val c = vertices[ci]
        Vector3f(a).add(b).add(c)
            // .div(3f) // can be skipped
            .normalize()
    }

    val triangles = createArrayList(20) { i ->

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
    private fun findLine(l0: Int, l1: Int): Line {
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

    private fun decodeTriangle(a: Int): TRef {
        val tri = triangles[a % 20]
        return when (a / 20) {
            0 -> TRef(tri)
            1 -> RRef(hexagonsPerSide, TRef(tri))
            else -> RRef(hexagonsPerSide, RRef(hexagonsPerSide, TRef(tri)))
        }
    }

    open class TRef(val tri: Triangle, val d: Int = 0) {
        open fun mapI(i: Int, j: Int) = i
        open fun mapJ(i: Int, j: Int) = j

        fun calcHexPos(sphere: HexagonSphere, i: Int, j: Int): Vector3f {
            return sphere.calcHexPos(tri.hexPosMatrix, mapI(i, j), mapJ(i, j))
        }

        override fun toString() = "TRef(${tri.index},x$d)"
    }

    class RRef(val n: Int, val ref: TRef) : TRef(ref.tri, ref.d + 2) {
        override fun mapI(i: Int, j: Int) = ref.mapI(n - 1 - (i + j), i)
        override fun mapJ(i: Int, j: Int) = ref.mapJ(n - 1 - (i + j), i)
    }

    init {

        val pointsToLines = createArrayList(12) { ArrayList<Hexagon>(5) }

        // define edges
        forLoopSafely(lineIndices.size, 2) { i ->

            val ai = lineIndices[i]
            val bi = lineIndices[i + 1]

            val ta = decodeTriangle(lineToTriangle[i].toInt())
            val tb = decodeTriangle(lineToTriangle[i + 1].toInt())

            val i0 = (i.shr(1)) * (hexagonsPerSide + 1L)
            val i1 = i0 + hexagonsPerSide

            val abLine = Line(this, ta, tb, i0, i1, +1)
            val baLine = Line(this, tb, ta, i1, i0, -1)
            lines.add(abLine)
            lines.add(baLine)

            val hex0 = abLine[0]
            val hex1 = if (hexagonsPerSide > 0) baLine[0] else hex0

            abLine.firstH = hex0
            abLine.lastH = hex1
            baLine.firstH = hex1
            baLine.lastH = hex0

            pointsToLines[ai].add(hex0)
            pointsToLines[bi].add(hex1)
        }

        for (i in lineIndices.indices step 2) {

            val abLine = lines[i]
            val baLine = lines[i + 1]

            val hex0 = abLine[0]
            val hex1 = if (hexagonsPerSide > 0) baLine[0] else hex0

            connectLine(abLine, hex0, 0)
            connectLine(baLine, hex1, 0)
        }

        fun connect(a: Hexagon, b: Hexagon) {
            val sa = a.neighborIds.indexOf(-1)
            val sb = b.neighborIds.indexOf(-1)
            b.neighbors[sb] = a
            b.neighborIds[sb] = a.index
            a.neighbors[sa] = b
            a.neighborIds[sa] = b.index
        }

        // create all pentagons
        for (i in 0 until PENTAGON_COUNT) {

            // build coordinate system
            val hexagons1 = pointsToLines[i]

            // sort neighbors by their angle
            val sortOrder = hexSortOrders0[i].toInt()

            // create a pentagon
            val pentagon = pentagons[i]
            for (j in 0 until 5) {
                val hj = hexagons1[sortOrder.shr(3 * j).and(7)]
                pentagon.corners[j] = if (hexagonsPerSide == 0) {
                    val target = Vector3f(vertices[i]).mix(hj.center, -0.5f).normalize()
                    val hk = hexagons1[sortOrder.shr(3 * ((j + 2) % 5)).and(7)]
                    hk.corners.minByOrNull { it.distanceSquared(target) }!!
                } else hj.corners[3]
            }

            // add all connections
            var h0 = hexagons1[sortOrder.shr(12)]
            for (j in hexagons1.indices) {
                val neighbor = hexagons1[sortOrder.shr(3 * j).and(7)]
                connect(pentagon, neighbor)
                connect(h0, neighbor)
                h0 = neighbor
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

        if (hexagonsPerSide == 0) {
            for (hexList in pointsToLines) {
                for (hex in hexList) {
                    sortNeighbors(hex)
                }
            }
        } else {
            // pre-computed sorting, so it no longer relies on coordinates
            // luckily, there are only two orders that actually appear :),
            // and one of them is already sorted
            val flags = 0x7bdef7ddef7bdfe
            var i = 0
            // order, that is to be applied,
            // whenever the flag is set: 0, 3, 1, 2, 4, 5
            for (hexList in pointsToLines) {
                for (hex in hexList) {
                    if (flags.hasFlag(1L.shl(i++))) {
                        val nei = hex.neighborIds
                        val nex = hex.neighbors
                        val tmi = nei[1]
                        nei[1] = nei[3]
                        nei[3] = nei[2]
                        nei[2] = tmi
                        val tmx = nex[1]
                        nex[1] = nex[3]
                        nex[3] = nex[2]
                        nex[2] = tmx
                    }
                }
            }
        }

        if (hexagonsPerSide == 0) {
            // every second needs to be reordered
            forLoopSafely(PENTAGON_COUNT, 2) { i ->
                val pentagon = pentagons[i]
                pentagon.neighborIds.rotateRight(2)
                pentagon.neighbors.rotateRight(2)
            }
        } // else perfectly sorted :)
    }

    private fun sortNeighbors(hex: Hexagon) {
        val center = hex.center
        val ax = Vector3f(hex.corners.last()).sub(center)
        val az = Vector3f(ax).cross(center)
        fun angle(c: Vector3f) = atan2(ax.dot(c), az.dot(c))
        val neighbors = hex.neighbors
        val neighborIds = hex.neighborIds
        for (i in neighbors.indices) {
            if (neighbors[i] == null) {
                neighbors[i] = find(neighborIds[i])
            }
        }
        // sort neighbors by their angle
        neighbors.sortBy {
            val c = it!!.center
            angle(c)
        }
        for (i in neighbors.indices) {
            neighborIds[i] = neighbors[i]!!.index
        }
    }

    fun triIdx(idx0: Long, i: Int, j: Int): Long {
        if (i !in 0 until hexagonsPerSide || j !in 0 until hexagonsPerSide - i) {
            assertFail("$i,$j is out of bounds for $hexagonsPerSide")
        }
        return idx0 + j + hexagonsPerSide.toLong() * i - (i * (i - 1L)).shr(1)
    }

    fun queryChunk(sc: Chunk): ArrayList<Hexagon> = queryChunk(sc.tri, sc.si, sc.sj)

    /**
     * group lines onto triangle faces
     * then split each triangle face into s*(s+1)/2 sub-triangles
     * */
    fun queryChunk(
        triangleIndex: Int,
        chunkS: Int,
        chunkT: Int,
    ): ArrayList<Hexagon> {

        assertTrue(chunkS + chunkT < chunkCount)
        assertTrue(chunkS >= 0)
        assertTrue(chunkT >= 0)

        // size could be estimated better
        val group = ArrayList<Hexagon>((hexagonsPerChunk + 1) * hexagonsPerChunk)

        val tri = triangles[triangleIndex]

        var ab: Line? = null
        var ac: Line? = null
        val ti3 = triangleIndex * 3
        val a = indices[ti3]
        val b = indices[ti3 + 1]
        val lenSq = len * len
        for (i in 0 until 2) {
            val li = triangleLines[triangleIndex * 2 + i]
            if (li < 0) continue
            val l0 = lineIndices[li * 2]
            val l1 = lineIndices[li * 2 + 1]
            if (l0 == a && l1 == b || l0 == b && l1 == a) {
                ab = if (vertices[a].distanceSquared(tri.abLine[0].center) > lenSq) tri.baLine else tri.abLine
            } else {
                ac = if (vertices[a].distanceSquared(tri.acLine[0].center) > lenSq) tri.caLine else tri.acLine
            }
        }

        // add a pentagon to the left corner
        if (chunkS == 0 && chunkT == 0 && pentagonTris[a] == triangleIndex) {
            group.add(pentagons[a])
        }

        if (chunkT == 0 && ab != null) {
            // add all bottom ones
            for (i in chunkS * hexagonsPerChunk until (chunkS + 1) * hexagonsPerChunk) {
                val hex = ab[i]
                group.add(hex)
                if (i > 0) connectLine(ab, hex, i)
            }
            if (chunkS == chunkCount - 1) { // last one
                val hex = ab[hexagonsPerSide]
                group.add(hex)
                connectLine(ab, hex, hexagonsPerSide)
            }
        }

        if (chunkS == 0 && chunkT == chunkCount - 1) {
            // add top region
            val j0 = hexagonsPerSide - hexagonsPerChunk
            if (ac != null) {
                // add left part
                // one extra for tip
                for (tj in j0..hexagonsPerSide) {
                    val hex = ac[tj]
                    group.add(hex)
                    if (tj > 0) connectLine(ac, hex, tj)
                }
            }
            for (ti in 0 until hexagonsPerChunk) {
                for (tj in j0 until hexagonsPerSide - ti) {
                    val hex = tri[ti, tj]
                    group.add(hex)
                    connectTriHex(tri, hex, ti, tj)
                }
            }
        } else {
            when (chunkS) {
                0 -> {
                    // left
                    val j0 = hexagonsPerChunk * chunkT
                    for (tj in 0 until hexagonsPerChunk) {
                        val tl = hexagonsPerChunk - (tj + 1).shr(1)
                        for (ti in 0 until tl) {
                            val jj = tj + j0
                            val hex = tri[ti, jj]
                            group.add(hex)
                            connectTriHex(tri, hex, ti, jj)
                        }
                    }
                    if (ac != null) {
                        // add left part
                        for (j in j0 until j0 + hexagonsPerChunk) {
                            val hex = ac[j]
                            group.add(hex)
                            if (j > 0) connectLine(ac, hex, j)
                        }
                    }
                }
                chunkCount - chunkT - 1 -> {
                    // right
                    val j0 = hexagonsPerChunk * chunkT
                    for (tj in 0 until hexagonsPerChunk) {
                        val tl = hexagonsPerChunk - tj.shr(1)
                        val jj = tj + j0
                        for (ti in hexagonsPerSide - jj - tl until hexagonsPerSide - jj) {
                            val hex = tri[ti, jj]
                            group.add(hex)
                            connectTriHex(tri, hex, ti, jj)
                        }
                    }
                }
                else -> {
                    // center
                    val j0 = hexagonsPerChunk * chunkT
                    for (tj in 0 until hexagonsPerChunk) {
                        val i0 = chunkS * hexagonsPerChunk - (tj + 1).shr(1)
                        val jj = tj + j0
                        for (ti in i0 until i0 + hexagonsPerChunk) {
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