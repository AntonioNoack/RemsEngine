package me.anno.maths.geometry.convexhull

import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertLessThan
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertSame
import me.anno.utils.assertions.assertTrue
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.count2
import me.anno.utils.types.Floats.toIntOr
import me.anno.utils.types.Floats.toLongOr
import me.anno.utils.types.Triangles.subCross
import org.joml.AABBd
import org.joml.Vector3d
import org.joml.Vector4i
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * HullLibrary class can create a convex hull from a collection of vertices, using the ComputeHull method.
 * The ShapeHull class uses this HullLibrary to create an approximate convex mesh given a general (non-polyhedral) convex shape.
 *
 * Includes modifications/improvements by John Ratcliff, see BringOutYourDead(compressVertices) below.
 * Unless you're using the native version, your vertices might be filtered first for much better performance.
 * This naive algorithm is roughly O(n log n), taking 1700ns/vertex, the filtered version takes as little as 30ns/vertex (128k -> 32).
 *
 * @author Antonio Noack O(n log n, 700ms for 128k->32, 53x faster), jezek2 O(n² log n, 37700ms for 128k->32)
 */
class ConvexHulls {

    private val triangles = ArrayList<Triangle?>()

    /**
     * Converts point cloud to polygonal representation.
     *
     * @param desc describes the input request
     * @return conversion result
     */
    private fun createConvexHullImpl(desc: HullDesc): ConvexHull? {

        // normalize point cloud, remove duplicates, restore!
        // originally was O(n²), now is O(n log n) (Java HashMap becomes O(n log n) if over capacity)
        val cleanVertices = cleanupVertices(desc.vertices, desc.normalEpsilon)
        if (cleanVertices == null) return null

        val ok = calculateConvexHull(cleanVertices, desc.maxNumVertices)
        if (!ok) return null

        val triangles = serializeTriangles()

        // re-index triangle mesh so it refers to only used vertices, rebuild a new vertex table.
        val resultVertices = ArrayList<Vector3d>(cleanVertices.size)
        val finalNumVertices = compactVertices(cleanVertices, resultVertices, triangles)

        val result = ConvexHull(resultVertices, triangles)
        resultVertices.subList(finalNumVertices, resultVertices.size).clear()
        return result
    }

    private fun allocateTriangle(a: Int, b: Int, c: Int): Triangle {
        val triangle = Triangle(a, b, c)
        triangle.id = triangles.size
        triangles.add(triangle)
        return triangle
    }

    private fun deAllocateTriangle(tri: Triangle) {
        assertSame(tri, triangles[tri.id])
        triangles[tri.id] = null
    }

    private fun b2bFixI(s: Triangle, t: Triangle, a: Int, b: Int) {
        assertEquals(s.id, triangles[s.getNeighbor(a, b)]!!.getNeighbor(b, a))
        assertEquals(t.id, triangles[t.getNeighbor(a, b)]!!.getNeighbor(b, a))
        triangles[s.getNeighbor(a, b)]!!.setNeighbor(b, a, t.getNeighbor(b, a))
        triangles[t.getNeighbor(b, a)]!!.setNeighbor(a, b, s.getNeighbor(a, b))
    }

    private fun removeB2b(s: Triangle, t: Triangle) {
        b2bFixI(s, t, t.y, t.z)
        b2bFixI(s, t, t.z, t.x)
        b2bFixI(s, t, t.x, t.y)
        deAllocateTriangle(s)
        deAllocateTriangle(t)
    }

    private fun validateTriangleNeighbors(t: Triangle) {
        assertSame(t, triangles[t.id])
        validateTriangleEdge(t, t.n.x, t.y, t.z)
        validateTriangleEdge(t, t.n.y, t.z, t.x)
        validateTriangleEdge(t, t.n.z, t.x, t.y)
    }

    private fun validateTriangleEdge(t: Triangle, tni: Int, a: Int, b: Int) {
        assertNotEquals(a, b)
        assertEquals(t.id, triangles[tni]!!.getNeighbor(b, a))
    }

    private fun findExtrudableTriangle(epsilon: Double): Triangle? {
        var bestCandidate: Triangle? = null
        var bestScore = epsilon
        for (i in 0 until triangles.size) {
            val candidate = triangles[i] ?: continue
            val score = candidate.rise
            if (score > bestScore) {
                bestCandidate = candidate
                bestScore = score
            }
        }
        return bestCandidate
    }

    private fun serializeTriangles(): IntArray {
        val numTriangles = triangles.count2 { it != null }
        val vertexIds = IntArray(numTriangles * 3)
        var k = 0
        for (i in 0 until triangles.size) {
            val triangle = triangles[i] ?: continue
            vertexIds[k++] = triangle.x
            vertexIds[k++] = triangle.y
            vertexIds[k++] = triangle.z
        }
        triangles.clear() // not strictly necessary, but might help GC
        return vertexIds
    }

    private val mip = MaximumInnerProduct()
    private lateinit var vertexToIndex: HashMap<Vector3d, Int>

    private fun calculateConvexHull(vertices: ArrayList<Vector3d>, vertexLimit: Int): Boolean {
        var numRemainingVertices = vertexLimit
        if (vertices.size < 4) return false

        val tmp = newVec()
        val tmp1 = newVec()
        val tmp2 = newVec()

        mip.addAll(vertices)

        val isExtreme = BooleanArray(vertices.size)
        val isUsed = BooleanArray(vertices.size)

        val bounds = JomlPools.aabbd.borrow()
        calculateBounds(vertices, bounds)
        val epsilon = bounds.diagonal * 0.001
        if (epsilon == 0.0 || bounds.isEmpty()) return false

        val p = findSimplex(vertices, isUsed)
        if (p == null) { // simplex failed
            subVec(3)
            return false // a valid interior point
        }

        val center = newVec()
        vertices[p.x].add(vertices[p.y], center)
            .add(vertices[p.z]).add(vertices[p.w])
        center.mul(0.25)

        // mark first simplex as extreme
        val t0 = allocateTriangle(p.z, p.w, p.y)
        t0.n.set(2, 3, 1)
        val t1 = allocateTriangle(p.w, p.z, p.x)
        t1.n.set(3, 2, 0)
        val t2 = allocateTriangle(p.x, p.y, p.w)
        t2.n.set(0, 1, 3)
        val t3 = allocateTriangle(p.y, p.x, p.z)
        t3.n.set(1, 0, 2)

        isExtreme[p.x] = true
        isExtreme[p.y] = true
        isExtreme[p.z] = true
        isExtreme[p.w] = true

        validateTriangleNeighbors(t0)
        validateTriangleNeighbors(t1)
        validateTriangleNeighbors(t2)
        validateTriangleNeighbors(t3)

        val dir = newVec()

        for (j in triangles.indices) {
            val triangle = checkNotNull(triangles[j])
            assertLessThan(triangle.maxValue, 0)
            triNormal(vertices[triangle.x], vertices[triangle.y], vertices[triangle.z], dir)
            triangle.maxValue = findVertexForSimplex(vertices, dir, isUsed)
            vertices[triangle.maxValue].sub(vertices[triangle.x], tmp)
            triangle.rise = dir.dot(tmp)
        }

        numRemainingVertices -= 4
        while (numRemainingVertices > 0) {
            val te = findExtrudableTriangle(epsilon) ?: break
            val v = te.maxValue
            assertNotEquals(-1, v)
            assertFalse(isExtreme[v]) // this should be our first time looking at that vertex
            isExtreme[v] = true
            //if(v==p0 || v==p1 || v==p2 || v==p3) continue; // done these already

            for (j in triangles.lastIndex downTo 0) {
                val tri = triangles[j] ?: continue
                if (isAbove(vertices, tri, vertices[v], 0.01 * epsilon)) {
                    extrude(tri, v)
                }
            }

            // now check for those degenerate cases where we have a flipped triangle or a really skinny triangle
            var j = triangles.size
            while ((j--) != 0) {
                val nt = triangles[j] ?: continue
                if (!hasVertex(nt, v)) break

                vertices[nt.y].sub(vertices[nt.x], tmp1)
                vertices[nt.z].sub(vertices[nt.y], tmp2)
                tmp1.cross(tmp2, tmp)
                if (isAbove(vertices, nt, center, 0.01 * epsilon) || tmp.length() < epsilon * epsilon * 0.1) {
                    val nb = checkNotNull(triangles[triangles[j]!!.n.x])
                    assertFalse(hasVertex(nb, v))
                    assertLessThan(nb.id, j)
                    extrude(nb, v)
                    j = triangles.size
                }
            }

            for (j in triangles.lastIndex downTo 0) {
                val triangle = triangles[j] ?: continue
                if (triangle.maxValue >= 0) break

                triNormal(vertices[triangle.x], vertices[triangle.y], vertices[triangle.z], dir)
                triangle.maxValue = findVertexForSimplex(vertices, dir, isUsed)
                if (isExtreme[triangle.maxValue]) {
                    triangle.maxValue = -1 // already done that vertex - algorithm needs to be able to terminate.
                } else {
                    vertices[triangle.maxValue].sub(vertices[triangle.x], tmp)
                    triangle.rise = dir.dot(tmp)
                }
            }
            numRemainingVertices--
        }
        subVec(5)
        return true
    }

    private fun findSimplex(vertices: List<Vector3d>, isUsed: BooleanArray): Vector4i? {

        val tmp = newVec()
        val tmp1 = newVec()
        val tmp2 = newVec()

        val basisX = newVec()
        val basisY = newVec()
        val basisZ = newVec()

        basisX.set(0.01, 0.02, 1.0)
        val p0 = findVertexForSimplex(vertices, basisX, isUsed)
        basisX.negate(tmp)
        val p1 = findVertexForSimplex(vertices, tmp, isUsed)
        vertices[p0].sub(vertices[p1], basisX)
        if (p0 == p1 || (basisX.x == 0.0 && basisX.y == 0.0 && basisX.z == 0.0)) {
            subVec(6)
            return null
        }

        tmp.set(1.0, 0.02, 0.0).cross(basisX, basisY)
        tmp.set(-0.02, 1.0, 0.0).cross(basisX, basisZ)
        if (basisY.length() > basisZ.length()) {
            basisY.normalize()
        } else {
            basisY.set(basisZ)
            basisY.normalize()
        }

        var p2 = findVertexForSimplex(vertices, basisY, isUsed)
        if (p2 == p0 || p2 == p1) {
            basisY.negate(tmp)
            p2 = findVertexForSimplex(vertices, tmp, isUsed)
        }
        if (p2 == p0 || p2 == p1) {
            subVec(6)
            return null
        }

        vertices[p2].sub(vertices[p0], basisY)
        basisY.cross(basisX, basisZ)
        basisZ.normalize()
        var p3 = findVertexForSimplex(vertices, basisZ, isUsed)
        if (p3 == p0 || p3 == p1 || p3 == p2) {
            basisZ.negate(tmp)
            p3 = findVertexForSimplex(vertices, tmp, isUsed)
        }
        if (p3 == p0 || p3 == p1 || p3 == p2) {
            subVec(6)
            return null
        }

        vertices[p1].sub(vertices[p0], tmp1)
        vertices[p2].sub(vertices[p0], tmp2)
        tmp1.cross(tmp2, tmp2)
        vertices[p3].sub(vertices[p0], tmp1)
        if (tmp1.dot(tmp2) < 0) {
            val tmp = p2
            p2 = p3
            p3 = tmp
        }

        subVec(6)
        return Vector4i(p0, p1, p2, p3)
    }

    private fun extrude(t0: Triangle, v: Int) { // O(1)

        val tx = t0.x
        val ty = t0.y
        val tz = t0.z
        val n = triangles.size

        val ta = allocateTriangle(v, ty, tz)
        ta.n.set(t0.n.x, n + 1, n + 2)
        triangles[t0.n.x]!!.setNeighbor(ty, tz, n)

        val tb = allocateTriangle(v, tz, tx)
        tb.n.set(t0.n.y, n + 2, n)
        triangles[t0.n.y]!!.setNeighbor(tz, tx, n + 1)

        val tc = allocateTriangle(v, tx, ty)
        tc.n.set(t0.n.z, n, n + 1)
        triangles[t0.n.z]!!.setNeighbor(tx, ty, n + 2)

        validateTriangleNeighbors(ta)
        validateTriangleNeighbors(tb)
        validateTriangleNeighbors(tc)

        val tax = triangles[ta.n.x]!!
        if (hasVertex(tax, v)) {
            removeB2b(ta, tax)
        }

        val tbx = triangles[tb.n.x]!!
        if (hasVertex(tbx, v)) {
            removeB2b(tb, tbx)
        }

        val tcx = triangles[tc.n.x]!!
        if (hasVertex(tcx, v)) {
            removeB2b(tc, tcx)
        }

        deAllocateTriangle(t0)
    }

    /**
     * BringOutYourDead (John Ratcliff): When you create a convex hull you hand it a large input set of vertices forming a 'point cloud'.
     * After the hull is generated it give you back a set of polygon faces which index the *original* point cloud.
     * The thing is, often times, there are many 'dead vertices' in the point cloud that are on longer referenced by the hull.
     * The routine 'BringOutYourDead' find only the referenced vertices, copies them to an new buffer, and re-indexes the hull so that it is a minimal representation.
     */
    private fun compactVertices(
        inputVertices: List<Vector3d>,
        resultVertices: ArrayList<Vector3d>,
        indices: IntArray
    ): Int {
        val usedIndices = IntArray(inputVertices.size)
        var numResultVertices = 0

        for (i in indices.indices) {
            val v = indices[i] // original array index

            if (usedIndices[v] != 0) { // if already remapped
                indices[i] = usedIndices[v] - 1 // index to new array
            } else {
                indices[i] = numResultVertices // new index mapping

                resultVertices.add(inputVertices[v]) // copy old vert to new vert array
                numResultVertices++ // increment output vert count
                usedIndices[v] = numResultVertices // assign new index remapping
            }
        }
        return numResultVertices
    }

    private fun cleanupVertices(inputVertices: List<Vector3d>, normalEpsilon: Double): ArrayList<Vector3d>? {

        if (inputVertices.isEmpty()) return null

        val bounds = AABBd()
        calculateBounds(inputVertices, bounds)

        var dx = bounds.deltaX
        var dy = bounds.deltaY
        var dz = bounds.deltaZ

        val center = Vector3d()
        bounds.getCenter(center)

        if (dx < EPSILON || dy < EPSILON || dz < EPSILON || inputVertices.size < 3) {
            return defineSmallBox(center, dx, dy, dz) // return cube
        }

        val invScale = Vector3d(1.0 / dx, 1.0 / dy, 1.0 / dz)
        val cleanVertices = ArrayList<Vector3d>(inputVertices.size)

        val limitI = (1 shl 21) - 1
        val centerI = (1 shl 20)

        val maxNormalScale = ((limitI - 2) * 0.999999).toLongOr()
        val normalEpsilonScale = min((1.0 / normalEpsilon).toLongOr(-1), maxNormalScale)

        val capacityGuess = max(inputVertices.size, 16)
        val uniqueVertices = HashMap<Long, Vector3d>(capacityGuess)
        val vertexToIndex = HashMap<Vector3d, Int>(capacityGuess)
        for (i in inputVertices.indices) {
            val inputVertex = inputVertices[i]

            // normalize into range [-0.5, +0.5]
            val px = (inputVertex.x - center.x) * invScale.x
            val py = (inputVertex.y - center.y) * invScale.y
            val pz = (inputVertex.z - center.z) * invScale.z

            // discretize the vertex, so we can hash it and compare it to its immediate neighbors
            val ix = (px * normalEpsilonScale).toLongOr() + centerI
            val iy = (py * normalEpsilonScale).toLongOr() + centerI
            val iz = (pz * normalEpsilonScale).toLongOr() + centerI
            assertTrue(ix in 1 until limitI)
            assertTrue(iy in 1 until limitI)
            assertTrue(iz in 1 until limitI)

            var found = false
            val score = center.distanceSquared(inputVertex)
            search@ for (dz in -1..1) {
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val id = (ix + dx).shl(42) or (iy + dy).shl(21) or (iz + dz)
                        val inResult = uniqueVertices[id] ?: continue
                        // ok, it is close enough to the old one
                        // now let us see if it is further from the center of the point cloud than the one we already recorded.
                        // in which case we keep this one instead.
                        if (score > center.distanceSquared(inResult)) {
                            // this would modify the original set
                            val indexInResult = vertexToIndex.remove(inResult) ?: continue // duplicate vertex?
                            cleanVertices[indexInResult] = inputVertex
                            vertexToIndex[inputVertex] = indexInResult
                        }
                        found = true
                        break@search
                    }
                }
            }

            if (!found) {
                val id = ix.shl(42) or iy.shl(21) or iz
                uniqueVertices[id] = inputVertex
                vertexToIndex[inputVertex] = cleanVertices.size
                cleanVertices.add(inputVertex)
            }
        }

        // ok... now make sure we didn't prune so many vertices it is now invalid.
        calculateBounds(cleanVertices, bounds)

        dx = bounds.deltaX
        dy = bounds.deltaY
        dz = bounds.deltaZ

        if (dx < EPSILON || dy < EPSILON || dz < EPSILON || cleanVertices.size < 3) {
            bounds.getCenter(center)
            return defineSmallBox(center, dx, dy, dz)
        }

        this.vertexToIndex = vertexToIndex
        return cleanVertices
    }

    private fun calculateBounds(vertices: List<Vector3d>, bounds: AABBd) {
        bounds.clear()
        bounds.union(vertices)
    }

    private fun defineSmallBox(
        center: Vector3d,
        dx: Double, dy: Double, dz: Double
    ): ArrayList<Vector3d> {

        var len = Double.MAX_VALUE
        var dx = dx
        var dy = dy
        var dz = dz

        if (dx >= EPSILON && dx < len) len = dx
        if (dy >= EPSILON && dy < len) len = dy
        if (dz >= EPSILON && dz < len) len = dz

        if (len == Double.MAX_VALUE) {
            dz = 0.01
            dy = dz
            dx = dy // one centimeter
        } else {
            if (dx < EPSILON) dx = len * 0.05 // 1/20th the shortest non-zero edge.
            if (dy < EPSILON) dy = len * 0.05
            if (dz < EPSILON) dz = len * 0.05
        }

        val x1 = center.x - dx
        val x2 = center.x + dx

        val y1 = center.y - dy
        val y2 = center.y + dy

        val z1 = center.z - dz
        val z2 = center.z + dz

        val result = ArrayList<Vector3d>(8)
        result.add(Vector3d(x1, y1, z1))
        result.add(Vector3d(x2, y1, z1))
        result.add(Vector3d(x2, y2, z1))
        result.add(Vector3d(x1, y2, z1))
        result.add(Vector3d(x1, y1, z2))
        result.add(Vector3d(x2, y1, z2))
        result.add(Vector3d(x2, y2, z2))
        result.add(Vector3d(x1, y2, z2))

        val map = HashMap<Vector3d, Int>(16)
        for (i in result.indices) {
            map[result[i]] = i
        }
        vertexToIndex = map
        return result
    }

    private fun findVertexWithMaxDotProduct(dir: Vector3d): Int {
        val bestVertex = mip.findBiggestDotProduct(dir)!!
        return vertexToIndex[bestVertex]!!
    }

    private fun findVertexForSimplex(vertices: List<Vector3d>, dir: Vector3d, isUsed: BooleanArray): Int {

        val tmp = newVec()
        val u = newVec()
        val v = newVec()

        while (true) {
            val m = findVertexWithMaxDotProduct(dir)
            if (isUsed[m]) { // if is already in use, return it
                subVec(3)
                return m
            }

            findOrthogonalVector(dir, u)
            u.cross(dir, v)

            var ma = -1
            var angle0 = 0.0
            while (angle0 <= 360.0) {

                setTmpFromDir(angle0, dir, tmp, v, u)

                val mb = findVertexWithMaxDotProduct(tmp)
                if (ma == m && mb == m) {
                    isUsed[m] = true // mark as in use
                    subVec(3)
                    return m
                }

                if (ma != -1 && mb != -1) { // "Yuck"
                    var mc = ma
                    var angle1 = angle0 - 40.0
                    while (angle1 <= angle0) {

                        setTmpFromDir(angle1, dir, tmp, v, u)

                        val md = findVertexWithMaxDotProduct(tmp)
                        if (mc == m && md == m) {
                            isUsed[m] = true // mark as in use
                            subVec(3)
                            return m
                        }
                        mc = md
                        angle1 += 5.0
                    }
                }
                ma = mb
                angle0 += 45.0
            }

            // "delete" that point
            assertTrue(mip.remove(vertices[m]))
        }
    }

    companion object {
        /** close enough to consider two numbers to be 'the same'. */
        private const val EPSILON = 0.000001
        private const val RADS_PER_DEG: Double = Math.PI * 180.0

        /**
         * Finds the convex hull without filtering vertices.
         * Runs in ~1700ns/element on my Ryzen 9 7950X3D.
         *
         * Returns null if too few unique vertices are provided.
         * Vertices are considered equal, if their direction matches by normalEpsilon.
         * */
        fun calculateConvexHullNaive(desc: HullDesc): ConvexHull? {
            return ConvexHulls().createConvexHullImpl(desc)
        }

        /**
         * Finds the convex hull with filtering for better performance.
         * Runs in ~30ns/element on my Ryzen 9 7950X3D under optimal conditions, 128k input vertices, 32 output vertices.
         *
         * Returns null if too few unique vertices are provided.
         * Vertices are considered equal, if their direction matches by normalEpsilon.
         * */
        fun calculateConvexHull(desc: HullDesc): ConvexHull? {
            var gridSize = ceil(4 * sqrt(desc.maxNumVertices.toFloat())).toIntOr()
            gridSize = clamp(gridSize, 16, 64) // 64² = 4096 is the standard maximum output size

            desc.vertices = PackedNormalsCompressor.compressVertices(desc.vertices, gridSize)
            return calculateConvexHullNaive(desc)
        }

        /**
         * Finds the convex hull with filtering for better performance.
         * Fewer dynamic allocations than pure HullDesc version.
         *
         * Returns null if too few unique vertices are provided.
         * Vertices are considered equal, if their direction matches by normalEpsilon.
         * */
        fun calculateConvexHull(vertices: FloatArray, desc: HullDesc): ConvexHull? {
            var gridSize = ceil(4 * sqrt(desc.maxNumVertices.toFloat())).toIntOr()
            gridSize = clamp(gridSize, 16, 64) // 64² = 4096 is the standard maximum output size

            desc.vertices = PackedNormalsCompressor.compressVertices(vertices, gridSize)
            return calculateConvexHullNaive(desc)
        }

        /** ///////////////////////////////////////////////////////////////////////// */
        private fun hasVertex(t: Triangle, v: Int): Boolean {
            return (t.x == v || t.y == v || t.z == v)
        }

        private fun findOrthogonalVector(v: Vector3d, out: Vector3d) {
            if (abs(v.y) > abs(v.z)) {
                out.set(-v.z, 0.0, v.x)
            } else {
                out.set(v.y, -v.x, 0.0)
            }
        }

        private fun setTmpFromDir(
            angleDegrees: Double, dir: Vector3d, tmp: Vector3d,
            v: Vector3d, u: Vector3d
        ) {
            val angleRadians = RADS_PER_DEG * angleDegrees
            val s = 0.025 * sin(angleRadians)
            val c = 0.025 * cos(angleRadians)

            tmp.set(
                c * v.x + s * u.x + dir.x,
                c * v.y + s * u.y + dir.y,
                c * v.z + s * u.z + dir.z
            )
        }

        private fun triNormal(v0: Vector3d, v1: Vector3d, v2: Vector3d, out: Vector3d): Vector3d {

            subCross(v0, v1, v2, out)
            val length = out.length()
            if (length == 0.0) out.set(1.0, 0.0, 0.0)
            else out.mul(1.0 / length)

            return out
        }

        private fun isAbove(vertices: List<Vector3d>, t: Triangle, p: Vector3d, minDistanceAbove: Double): Boolean {
            val n = triNormal(vertices[t.x], vertices[t.y], vertices[t.z], newVec())
            val tmp = newVec()
            p.sub(vertices[t.x], tmp)
            subVec(2)
            return n.dot(tmp) > minDistanceAbove
        }

        private val joml = JomlPools.vec3d
        private fun subVec(i: Int) = joml.sub(i)
        private fun newVec() = joml.create()
    }
}
