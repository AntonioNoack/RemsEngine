package com.bulletphysics.linearmath.convexhull

import com.bulletphysics.linearmath.convexhull.PackedNormalsCompressor.compressVertices
import me.anno.maths.Maths.clamp
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Floats.toIntOr
import me.anno.utils.types.Triangles.subCross
import org.joml.AABBd
import org.joml.Vector3d
import org.joml.Vector4i
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * HullLibrary class can create a convex hull from a collection of vertices, using the ComputeHull method.
 * The ShapeHull class uses this HullLibrary to create an approximate convex mesh given a general (non-polyhedral) convex shape.
 *
 * Includes modifications/improvements by John Ratcliff, see BringOutYourDead(compressVertices) below.
 * Unless you're using the native version, your vertices might be filtered first for much better performance.
 *
 * @author jezek2
 */
class HullLibrary {

    private val tris = ArrayList<Triangle?>()

    /**
     * Converts point cloud to polygonal representation.
     *
     * @param desc describes the input request
     * @return conversion result
     */
    private fun createConvexHullImpl(desc: HullDesc): ConvexHull? {

        val vertexCount = max(desc.vertices.size, 8)
        val cleanVertices = createArrayList(vertexCount) { Vector3d() }

        val scale = newVec()

        val numResultVertices = IntArray(1)
        val ok = cleanupVertices(
            desc.vertices, numResultVertices,
            cleanVertices, desc.normalEpsilon, scale
        ) // normalize point cloud, remove duplicates!
        if (!ok) {
            subVec(1)
            return null
        }

        cleanVertices.subList(numResultVertices[0], cleanVertices.size).clear()
        // scale vertices back to their original size.
        for (i in cleanVertices.indices) {
            cleanVertices[i].mul(scale)
        }
        subVec(1)

        val triangles = calcHull(cleanVertices, desc.maxNumVertices)
            ?: return null

        // re-index triangle mesh so it refers to only used vertices, rebuild a new vertex table.
        val resultVertices = ArrayList<Vector3d>(cleanVertices.size)
        val finalNumVertices = compactVertices(
            cleanVertices,
            resultVertices, triangles
        )

        val result = ConvexHull(resultVertices, triangles)
        resultVertices.subList(finalNumVertices, resultVertices.size).clear()
        return result
    }

    private fun allocateTriangle(a: Int, b: Int, c: Int): Triangle {
        val tr = Triangle(a, b, c)
        tr.id = tris.size
        tris.add(tr)
        return tr
    }

    private fun deAllocateTriangle(tri: Triangle) {
        assert(tris[tri.id] === tri)
        tris[tri.id] = null
    }

    private fun b2bfix(s: Triangle, t: Triangle) {
        b2bFixI(s, t, t.y, t.z)
        b2bFixI(s, t, t.z, t.x)
        b2bFixI(s, t, t.x, t.y)
    }

    private fun b2bFixI(s: Triangle, t: Triangle, a: Int, b: Int) {
        assert(tris[s.getNeighbor(a, b)]!!.getNeighbor(b, a) == s.id)
        assert(tris[t.getNeighbor(a, b)]!!.getNeighbor(b, a) == t.id)
        tris[s.getNeighbor(a, b)]!!.setNeighbor(b, a, t.getNeighbor(b, a))
        tris[t.getNeighbor(b, a)]!!.setNeighbor(a, b, s.getNeighbor(a, b))
    }

    private fun removeB2b(s: Triangle, t: Triangle) {
        b2bfix(s, t)
        deAllocateTriangle(s)
        deAllocateTriangle(t)
    }

    private fun checkIt(t: Triangle) {
        assert(tris[t.id] === t)
        checkItI(t, t.n.x, t.y, t.z)
        checkItI(t, t.n.y, t.z, t.x)
        checkItI(t, t.n.z, t.x, t.y)
    }

    private fun checkItI(t: Triangle, tni: Int, a: Int, b: Int) {
        assert(a != b)
        assert(tris[tni]!!.getNeighbor(b, a) == t.id)
    }

    private fun extrudable(epsilon: Double): Triangle? {
        var t: Triangle? = null
        for (i in 0 until tris.size) {
            if (t == null || (tris[i] != null && t.rise < tris[i]!!.rise)) {
                t = tris[i]
            }
        }
        return if (t != null && (t.rise > epsilon)) t else null
    }

    private fun calcHull(vertices: List<Vector3d>, vertexLimit: Int): IntArrayList? {
        val rc = calcHullGen(vertices, vertexLimit)
        if (rc == 0) return null
        val ts = IntArrayList()
        for (i in 0 until tris.size) {
            val tri = tris[i] ?: continue
            ts.add(tri.x)
            ts.add(tri.y)
            ts.add(tri.y)
            deAllocateTriangle(tri)
        }
        tris.clear()
        return ts
    }

    private fun calcHullGen(vertices: List<Vector3d>, vertexLimit: Int): Int {
        var numRemainingVertices = vertexLimit
        if (vertices.size < 4) return 0

        val tmp = newVec()
        val tmp1 = newVec()
        val tmp2 = newVec()

        val isExtreme = IntArray(vertices.size)
        val predicate = IntArray(vertices.size)
        predicate.fill(1)

        val bounds = JomlPools.aabbd.borrow()
        calculateBounds(vertices, vertices.size, bounds)
        val epsilon = bounds.diagonal * 0.001
        assert(epsilon != 0.0)

        val p = findSimplex(vertices, predicate)
        if (p == null) { // simplex failed
            subVec(3)
            return 0 // a valid interior point
        }

        val center = newVec()
        vertices[p.x].add(vertices[p.y], center)
            .add(vertices[p.z]).add(vertices[p.w])
        center.mul(1.0 / 4f)

        // mark first simplex as extreme
        val t0 = allocateTriangle(p.z, p.w, p.y)
        t0.n.set(2, 3, 1)
        val t1 = allocateTriangle(p.w, p.z, p.x)
        t1.n.set(3, 2, 0)
        val t2 = allocateTriangle(p.x, p.y, p.w)
        t2.n.set(0, 1, 3)
        val t3 = allocateTriangle(p.y, p.x, p.z)
        t3.n.set(1, 0, 2)

        isExtreme[p.x] = 1
        isExtreme[p.y] = 1
        isExtreme[p.z] = 1
        isExtreme[p.w] = 1

        checkIt(t0)
        checkIt(t1)
        checkIt(t2)
        checkIt(t3)

        val n = newVec()

        // todo this is O(n²), but could be optimized probably by nearest neighbors...
        for (j in 0 until tris.size) {
            val t = checkNotNull(tris[j])
            assert(t.maxValue < 0)
            triNormal(vertices[t.x], vertices[t.y], vertices[t.z], n)
            t.maxValue = findMaxInDirForSimplex(vertices, n, predicate)
            vertices[t.maxValue].sub(vertices[t.x], tmp)
            t.rise = n.dot(tmp)
        }

        var te: Triangle? = null
        numRemainingVertices -= 4
        while (numRemainingVertices > 0 && ((extrudable(epsilon).also { te = it }) != null)) {

            val v = te!!.maxValue
            assert(v != -1)
            assert(isExtreme[v] == 0) // wtf we've already done this vertex
            isExtreme[v] = 1
            //if(v==p0 || v==p1 || v==p2 || v==p3) continue; // done these already
            var j = tris.size
            while ((j--) != 0) {
                val tri = tris[j]
                if (tri == null) {
                    continue
                }
                val t = tri
                if (isAbove(vertices, t, vertices[v], 0.01 * epsilon)) {
                    extrude(tri, v)
                }
            }

            // now check for those degenerate cases where we have a flipped triangle or a really skinny triangle
            j = tris.size
            while ((j--) != 0) {
                if (tris[j] == null) {
                    continue
                }
                if (!hasVertex(tris[j]!!, v)) {
                    break
                }
                val nt = tris[j]!!
                vertices[nt.y].sub(vertices[nt.x], tmp1)
                vertices[nt.z].sub(vertices[nt.y], tmp2)
                tmp1.cross(tmp2, tmp)
                if (isAbove(vertices, nt, center, 0.01 * epsilon) || tmp.length() < epsilon * epsilon * 0.1) {
                    val nb = checkNotNull(tris[tris[j]!!.n.x])
                    assert(!hasVertex(nb, v))
                    assert(nb.id < j)
                    extrude(nb, v)
                    j = tris.size
                }
            }

            j = tris.size
            while ((j--) != 0) {
                val t = tris[j]
                if (t == null) {
                    continue
                }
                if (t.maxValue >= 0) {
                    break
                }
                triNormal(vertices[t.x], vertices[t.y], vertices[t.z], n)
                t.maxValue = findMaxInDirForSimplex(vertices, n, predicate)
                if (isExtreme[t.maxValue] != 0) {
                    t.maxValue = -1 // already done that vertex - algorithm needs to be able to terminate.
                } else {
                    vertices[t.maxValue].sub(vertices[t.x], tmp)
                    t.rise = n.dot(tmp)
                }
            }
            numRemainingVertices--
        }
        subVec(5)
        return 1
    }

    private fun findSimplex(vertices: List<Vector3d>, predicate: IntArray): Vector4i? {
        val tmp = newVec()
        val tmp1 = newVec()
        val tmp2 = newVec()

        val basisX = newVec()
        val basisY = newVec()
        val basisZ = newVec()

        basisX.set(0.01, 0.02, 1.0)
        val p0 = findMaxInDirForSimplex(vertices, basisX, predicate)
        basisX.negate(tmp)
        val p1 = findMaxInDirForSimplex(vertices, tmp, predicate)
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

        var p2 = findMaxInDirForSimplex(vertices, basisY, predicate)
        if (p2 == p0 || p2 == p1) {
            basisY.negate(tmp)
            p2 = findMaxInDirForSimplex(vertices, tmp, predicate)
        }
        if (p2 == p0 || p2 == p1) {
            subVec(6)
            return null
        }

        vertices[p2].sub(vertices[p0], basisY)
        basisY.cross(basisX, basisZ)
        basisZ.normalize()
        var p3 = findMaxInDirForSimplex(vertices, basisZ, predicate)
        if (p3 == p0 || p3 == p1 || p3 == p2) {
            basisZ.negate(tmp)
            p3 = findMaxInDirForSimplex(vertices, tmp, predicate)
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
        val n = tris.size

        val ta = allocateTriangle(v, ty, tz)
        ta.n.set(t0.n.x, n + 1, n + 2)
        tris[t0.n.x]!!.setNeighbor(ty, tz, n)

        val tb = allocateTriangle(v, tz, tx)
        tb.n.set(t0.n.y, n + 2, n)
        tris[t0.n.y]!!.setNeighbor(tz, tx, n + 1)

        val tc = allocateTriangle(v, tx, ty)
        tc.n.set(t0.n.z, n, n + 1)
        tris[t0.n.z]!!.setNeighbor(tx, ty, n + 2)

        checkIt(ta)
        checkIt(tb)
        checkIt(tc)

        val tax = tris[ta.n.x]!!
        if (hasVertex(tax, v)) {
            removeB2b(ta, tax)
        }

        val tbx = tris[tb.n.x]!!
        if (hasVertex(tbx, v)) {
            removeB2b(tb, tbx)
        }

        val tcx = tris[tc.n.x]!!
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
        indices: IntArrayList
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

    private fun cleanupVertices(
        inputVertices: List<Vector3d>,
        vertexCount: IntArray,  // output number of vertices
        vertices: List<Vector3d>,  // location to store the results.
        normalEpsilon: Double,
        scale: Vector3d
    ): Boolean {
        if (inputVertices.isEmpty()) {
            return false
        }

        val recip = DoubleArray(3)

        scale.set(1.0, 1.0, 1.0)

        val bounds = AABBd()
        calculateBounds(inputVertices, inputVertices.size, bounds)

        var dx = bounds.deltaX
        var dy = bounds.deltaY
        var dz = bounds.deltaZ

        val center = Vector3d()
        bounds.getCenter(center)

        if (dx < EPSILON || dy < EPSILON || dz < EPSILON || vertices.size < 3) {

            var len = Double.MAX_VALUE

            if (dx > EPSILON && dx < len) len = dx
            if (dy > EPSILON && dy < len) len = dy
            if (dz > EPSILON && dz < len) len = dz

            if (len == Double.MAX_VALUE) {
                dz = 0.01
                dy = dz
                dx = dy // one centimeter
            } else {
                if (dx < EPSILON) dx = len * 0.05 // 1/5th the shortest non-zero edge.
                if (dy < EPSILON) dy = len * 0.05
                if (dz < EPSILON) dz = len * 0.05
            }

            defineBox(vertexCount, vertices, center, dx, dy, dz)
            return true // return cube
        } else {
            scale.x = dx
            scale.y = dy
            scale.z = dz

            recip[0] = 1.0 / dx
            recip[1] = 1.0 / dy
            recip[2] = 1.0 / dz

            center.x *= recip[0]
            center.y *= recip[1]
            center.z *= recip[2]
        }

        vertexCount[0] = 0
        for (i in inputVertices.indices) {
            val p = inputVertices[i]

            var px = p.x
            var py = p.y
            var pz = p.z

            px = px * recip[0] // normalize
            py = py * recip[1] // normalize
            pz = pz * recip[2] // normalize

            var j = 0
            while (j < vertexCount[0]) {
                // XXX might be broken
                val v = vertices[j]

                dx = abs(v.x - px)
                dy = abs(v.y - py)
                dz = abs(v.z - pz)

                if (dx < normalEpsilon && dy < normalEpsilon && dz < normalEpsilon) {
                    // ok, it is close enough to the old one
                    // now let us see if it is further from the center of the point cloud than the one we already recorded.
                    // in which case we keep this one instead.
                    if (center.distanceSquared(px, py, pz) > center.distanceSquared(v)) {
                        v.x = px
                        v.y = py
                        v.z = pz
                    }
                    break
                }
                j++
            }

            if (j == vertexCount[0]) {
                vertices[vertexCount[0]++].set(px, py, pz)
            }
        }

        // ok... now make sure we didn't prune so many vertices it is now invalid.
        calculateBounds(vertices, vertexCount[0], bounds)

        dx = bounds.deltaX
        dy = bounds.deltaY
        dz = bounds.deltaZ

        if (dx < EPSILON || dy < EPSILON || dz < EPSILON || vertexCount[0] < 3) {
            bounds.getCenter(center)

            var len = Double.MAX_VALUE

            if (dx >= EPSILON && dx < len) len = dx
            if (dy >= EPSILON && dy < len) len = dy
            if (dz >= EPSILON && dz < len) len = dz

            if (len == Double.MAX_VALUE) {
                dz = 0.01
                dy = dz
                dx = dy // one centimeter
            } else {
                if (dx < EPSILON) dx = len * 0.05 // 1/5th the shortest non-zero edge.
                if (dy < EPSILON) dy = len * 0.05
                if (dz < EPSILON) dz = len * 0.05
            }

            defineBox(vertexCount, vertices, center, dx, dy, dz)
        }

        return true
    }

    private fun calculateBounds(vertices: List<Vector3d>, numVertices: Int, bounds: AABBd) {
        bounds.clear()
        for (i in 0 until numVertices) {
            bounds.union(vertices[i])
        }
    }

    private fun defineBox(
        vertexCount: IntArray, vertices: List<Vector3d>,
        center: Vector3d, dx: Double, dy: Double, dz: Double
    ) {

        vertexCount[0] = 0 // add box

        val x1 = center.x - dx
        val x2 = center.x + dx

        val y1 = center.y - dy
        val y2 = center.y + dy

        val z1 = center.z - dz
        val z2 = center.z + dz

        addPoint(vertexCount, vertices, x1, y1, z1)
        addPoint(vertexCount, vertices, x2, y1, z1)
        addPoint(vertexCount, vertices, x2, y2, z1)
        addPoint(vertexCount, vertices, x1, y2, z1)
        addPoint(vertexCount, vertices, x1, y1, z2)
        addPoint(vertexCount, vertices, x2, y1, z2)
        addPoint(vertexCount, vertices, x2, y2, z2)
        addPoint(vertexCount, vertices, x1, y2, z2)
    }

    companion object {
        /** close enough to consider two btScalaring point numbers to be 'the same'. */
        private const val EPSILON = 0.000001
        private const val RADS_PER_DEG: Double = Math.PI * 180.0

        fun createConvexHullNaive(desc: HullDesc): ConvexHull? {
            return HullLibrary().createConvexHullImpl(desc)
        }

        fun createConvexHull(desc: HullDesc): ConvexHull? {
            var gridSize = ceil(4 * sqrt(desc.maxNumVertices.toFloat())).toIntOr()
            gridSize = clamp(gridSize, 16, 64) // 64² = 4096 is the standard maximum output size

            desc.vertices = compressVertices(desc.vertices, gridSize)
            return createConvexHullNaive(desc)
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

        private fun findMaxInDirFiltered(
            vertices: List<Vector3d>,
            dir: Vector3d, predicate: IntArray
        ): Int {
            var bestVertexId = -1
            var bestScore = Double.NEGATIVE_INFINITY
            for (i in vertices.indices) {
                if (predicate[i] != 0) {
                    val score = vertices[i].dot(dir)
                    if (bestVertexId == -1 || score > bestScore) {
                        bestVertexId = i
                        bestScore = score
                    }
                }
            }
            assert(bestVertexId != -1)
            return bestVertexId
        }

        private fun findMaxInDirForSimplex(
            vertices: List<Vector3d>,
            dir: Vector3d, predicate: IntArray
        ): Int {

            val tmp = newVec()
            val u = newVec()
            val v = newVec()

            while (true) {
                val m = findMaxInDirFiltered(vertices, dir, predicate)
                if (predicate[m] == 3) {
                    subVec(3)
                    return m
                }

                findOrthogonalVector(dir, u)
                u.cross(dir, v)

                var ma = -1
                var angle0 = 0.0
                while (angle0 <= 360.0) {
                    val s = 0.025 * sin(RADS_PER_DEG * angle0)
                    val c = 0.025 * cos(RADS_PER_DEG * angle0)

                    tmp.set(
                        c * v.x + s * u.x + dir.x,
                        c * v.y + s * u.y + dir.y,
                        c * v.z + s * u.z + dir.z
                    )

                    val mb = findMaxInDirFiltered(vertices, tmp, predicate)
                    if (ma == m && mb == m) {
                        predicate[m] = 3
                        subVec(3)
                        return m
                    }

                    if (ma != -1 && mb != -1) { // "Yuck"
                        var mc = ma
                        var angle1 = angle0 - 40.0
                        while (angle1 <= angle0) {
                            val s = 0.025 * sin(RADS_PER_DEG * angle1)
                            val c = 0.025 * cos(RADS_PER_DEG * angle1)

                            tmp.set(
                                c * v.x + s * u.x + dir.x,
                                c * v.y + s * u.y + dir.y,
                                c * v.z + s * u.z + dir.z
                            )

                            val md = findMaxInDirFiltered(vertices, tmp, predicate)
                            if (mc == m && md == m) {
                                predicate[m] = 3
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
                predicate[m] = 0
            }
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

        private fun addPoint(vertexCount: IntArray, vertices: List<Vector3d>, x: Double, y: Double, z: Double) {
            vertices[vertexCount[0]++].set(x, y, z)
        }

        private val joml = JomlPools.vec3d
        private fun subVec(i: Int) = joml.sub(i)
        private fun newVec() = joml.create()
    }
}
