// includes modifications/improvements by John Ratcliff, see BringOutYourDead below.
package com.bulletphysics.linearmath.convexhull

import com.bulletphysics.BulletGlobals
import com.bulletphysics.linearmath.MiscUtil
import com.bulletphysics.linearmath.VectorUtil.add
import com.bulletphysics.linearmath.VectorUtil.mul
import com.bulletphysics.linearmath.VectorUtil.setMax
import com.bulletphysics.linearmath.VectorUtil.setMin
import com.bulletphysics.util.IntArrayList
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setCross
import com.bulletphysics.util.setNegate
import com.bulletphysics.util.setNormalize
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setSub
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * HullLibrary class can create a convex hull from a collection of vertices, using
 * the ComputeHull method. The [com.bulletphysics.collision.shapes.ShapeHull] class uses this HullLibrary to create
 * a approximate convex mesh given a general (non-polyhedral) convex shape.
 *
 * @author jezek2
 */
class HullLibrary {
    val vertexIndexMapping = IntArrayList()

    private val tris = ArrayList<Tri?>()

    /**
     * Converts point cloud to polygonal representation.
     *
     * @param desc   describes the input request
     * @param result contains the result
     * @return whether conversion was successful
     */
    fun createConvexHull(desc: HullDesc, result: HullResult): Boolean {
        val hr = PHullResult()

        var vcount = desc.vcount
        if (vcount < 8) vcount = 8

        val vertexSource = ArrayList<Vector3d>()
        MiscUtil.resize(vertexSource, vcount, Vector3d::class.java)

        val scale = Stack.newVec()

        val ovcount = IntArray(1)

        var ok = cleanupVertices(
            desc.vcount,
            desc.vertices!!,
            ovcount,
            vertexSource,
            desc.normalEpsilon,
            scale
        ) // normalize point cloud, remove duplicates!
        if (!ok) {
            Stack.subVec(1)
            return false
        }

        // scale vertices back to their original size.
        for (i in 0 until ovcount[0]) {
            val v = vertexSource[i]
            mul(v, v, scale)
        }

        ok = computeHull(ovcount[0], vertexSource, hr, desc.maxVertices)
        if (!ok) {
            Stack.subVec(1)
            return false
        }

        // re-index triangle mesh so it refers to only used vertices, rebuild a new vertex table.
        val vertexScratch = ArrayList<Vector3d>()
        MiscUtil.resize(vertexScratch, hr.vertexCount, Vector3d::class.java)

        bringOutYourDead(hr.vertices!!, hr.vertexCount, vertexScratch, ovcount, hr.indices, hr.indexCount)

        if (desc.hasHullFlag(HullFlags.TRIANGLES)) { // if he wants the results as triangle!
            result.polygons = false
            result.numOutputVertices = ovcount[0]
            MiscUtil.resize(result.outputVertices, ovcount[0], Vector3d::class.java)
            result.numFaces = hr.faceCount
            result.numIndices = hr.indexCount

            MiscUtil.resize(result.indices, hr.indexCount, 0)

            for (i in 0 until ovcount[0]) {
                result.outputVertices[i].set(vertexScratch[i])
            }

            if (desc.hasHullFlag(HullFlags.REVERSE_ORDER)) {
                val srcPtr = hr.indices
                var srcIdx = 0

                val dstPtr = result.indices
                var dstIdx = 0

                repeat(hr.faceCount) {
                    dstPtr.set(dstIdx, srcPtr.get(srcIdx + 2))
                    dstPtr.set(dstIdx + 1, srcPtr.get(srcIdx + 1))
                    dstPtr.set(dstIdx + 2, srcPtr.get(srcIdx))
                    dstIdx += 3
                    srcIdx += 3
                }
            } else {
                for (i in 0 until hr.indexCount) {
                    result.indices.set(i, hr.indices.get(i))
                }
            }
        } else {
            result.polygons = true
            result.numOutputVertices = ovcount[0]
            MiscUtil.resize(result.outputVertices, ovcount[0], Vector3d::class.java)
            result.numFaces = hr.faceCount
            result.numIndices = hr.indexCount + hr.faceCount
            MiscUtil.resize(result.indices, result.numIndices, 0)
            for (i in 0 until ovcount[0]) {
                result.outputVertices[i].set(vertexScratch[i])
            }

            val srcPtr = hr.indices
            var srcIdx = 0

            val dstPtr = result.indices
            var dstIdx = 0

            repeat(hr.faceCount) {
                dstPtr.set(dstIdx, 3)
                if (desc.hasHullFlag(HullFlags.REVERSE_ORDER)) {
                    dstPtr.set(dstIdx + 1, srcPtr.get(srcIdx + 2))
                    dstPtr.set(dstIdx + 2, srcPtr.get(srcIdx + 1))
                    dstPtr.set(dstIdx + 3, srcPtr.get(srcIdx))
                } else {
                    dstPtr.set(dstIdx + 1, srcPtr.get(srcIdx))
                    dstPtr.set(dstIdx + 2, srcPtr.get(srcIdx + 1))
                    dstPtr.set(dstIdx + 3, srcPtr.get(srcIdx + 2))
                }

                dstIdx += 4
                srcIdx += 3
            }
        }
        releaseHull(hr)

        Stack.subVec(1)
        return true
    }

    /**
     * Release memory allocated for this result, we are done with it.
     */
    fun releaseResult(result: HullResult) {
        if (!result.outputVertices.isEmpty()) {
            result.numOutputVertices = 0
            result.outputVertices.clear()
        }
        if (result.indices.size() != 0) {
            result.numIndices = 0
            result.indices.clear()
        }
    }

    private fun computeHull(
        vcount: Int,
        vertices: List<Vector3d>,
        result: PHullResult,
        vlimit: Int
    ): Boolean {
        val trisCount = IntArray(1)
        val ret = calcHull(vertices, vcount, result.indices, trisCount, vlimit)
        if (ret == 0) return false
        result.indexCount = trisCount[0] * 3
        result.faceCount = trisCount[0]
        result.vertices = vertices
        result.vertexCount = vcount
        return true
    }

    private fun allocateTriangle(a: Int, b: Int, c: Int): Tri {
        val tr = Tri(a, b, c)
        tr.id = tris.size
        tris.add(tr)

        return tr
    }

    private fun deAllocateTriangle(tri: Tri) {
        assert(tris[tri.id] === tri)
        tris[tri.id] = null
    }

    private fun b2bfix(s: Tri, t: Tri) {
        for (i in 0..2) {
            val i1 = (i + 1) % 3
            val i2 = (i + 2) % 3
            val a = s.getCoord(i1)
            val b = s.getCoord(i2)
            assert(tris[s.neibGet(a, b)]!!.neibGet(b, a) == s.id)
            assert(tris[t.neibGet(a, b)]!!.neibGet(b, a) == t.id)
            tris[s.neibGet(a, b)]!!.neibSet(b, a, t.neibGet(b, a))
            tris[t.neibGet(b, a)]!!.neibSet(a, b, s.neibGet(a, b))
        }
    }

    private fun removeb2b(s: Tri, t: Tri) {
        b2bfix(s, t)
        deAllocateTriangle(s)

        deAllocateTriangle(t)
    }

    private fun checkit(t: Tri) {
        assert(tris[t.id] === t)
        for (i in 0..2) {
            val i1 = (i + 1) % 3
            val i2 = (i + 2) % 3
            val a = t.getCoord(i1)
            val b = t.getCoord(i2)

            assert(a != b)
            assert(tris[t.n.getCoord(i)]!!.neibGet(b, a) == t.id)
        }
    }

    private fun extrudable(epsilon: Double): Tri? {
        var t: Tri? = null
        for (i in 0 until tris.size) {
            if (t == null || (tris[i] != null && t.rise < tris[i]!!.rise)) {
                t = tris[i]
            }
        }
        return if (t != null && (t.rise > epsilon)) t else null
    }

    private fun calcHull(
        vertices: List<Vector3d>, vertexCount: Int, trisOut: IntArrayList,
        trisCount: IntArray, vertexLimit: Int
    ): Int {
        val rc = calcHullGen(vertices, vertexCount, vertexLimit)
        if (rc == 0) return 0

        val ts = IntArrayList()

        for (i in 0 until tris.size) {
            if (tris[i] != null) {
                for (j in 0..2) {
                    ts.add((tris[i])!!.getCoord(j))
                }
                deAllocateTriangle(tris[i]!!)
            }
        }
        trisCount[0] = ts.size() / 3
        MiscUtil.resize(trisOut, ts.size(), 0)

        for (i in 0 until ts.size()) {
            trisOut.set(i, ts.get(i))
        }

        @Suppress("UNCHECKED_CAST")
        MiscUtil.resize(tris as MutableList<Tri>, 0, Tri::class.java)

        return 1
    }

    private fun calcHullGen(verts: List<Vector3d>, vertsCount: Int, vlimit: Int): Int {
        var vlimit = vlimit
        if (vertsCount < 4) return 0

        val tmp = Stack.newVec()
        val tmp1 = Stack.newVec()
        val tmp2 = Stack.newVec()

        if (vlimit == 0) {
            vlimit = 1000000000
        }
        //int j;
        val bmin = Stack.newVec(verts[0])
        val bmax = Stack.newVec(verts[0])
        val isextreme = IntArrayList()
        //isextreme.reserve(verts_count);
        val allow = IntArrayList()

        //allow.reserve(verts_count);
        for (j in 0 until vertsCount) {
            allow.add(1)
            isextreme.add(0)
            setMin(bmin, verts[j])
            setMax(bmax, verts[j])
        }
        tmp.setSub(bmax, bmin)
        val epsilon = tmp.length() * 0.001f
        assert(epsilon != 0.0)

        val p = findSimplex(verts, vertsCount, allow, Int4())
        if (p.x == -1) {
            Stack.subVec(5)
            return 0 // simplex failed
            // a valid interior point
        }

        val center = Stack.newVec()
        add(center, verts[p.x], verts[p.y], verts[p.z], verts[p.w])
        center.mul(1.0 / 4f)

        val t0 = allocateTriangle(p.z, p.w, p.y)
        t0.n.set(2, 3, 1)
        val t1 = allocateTriangle(p.w, p.z, p.x)
        t1.n.set(3, 2, 0)
        val t2 = allocateTriangle(p.x, p.y, p.w)
        t2.n.set(0, 1, 3)
        val t3 = allocateTriangle(p.y, p.x, p.z)
        t3.n.set(1, 0, 2)
        isextreme.set(p.x, 1)
        isextreme.set(p.y, 1)
        isextreme.set(p.z, 1)
        isextreme.set(p.w, 1)
        checkit(t0)
        checkit(t1)
        checkit(t2)
        checkit(t3)

        val n = Stack.newVec()

        for (j in 0 until tris.size) {
            val t = checkNotNull(tris[j])
            assert(t.maxValue < 0)
            triNormal(verts[t.x], verts[t.y], verts[t.z], n)
            t.maxValue = maxdirsterid(verts, vertsCount, n, allow)
            tmp.setSub(verts[t.maxValue], verts[t.x])
            t.rise = n.dot(tmp)
        }
        var te: Tri? = null
        vlimit -= 4
        while (vlimit > 0 && ((extrudable(epsilon).also { te = it }) != null)) {
            val v = te!!.maxValue
            assert(v != -1)
            assert(isextreme.get(v) == 0) // wtf we've already done this vertex
            isextreme.set(v, 1)
            //if(v==p0 || v==p1 || v==p2 || v==p3) continue; // done these already
            var j = tris.size
            while ((j--) != 0) {
                if (tris[j] == null) {
                    continue
                }
                val t: Int3 = tris[j]!!
                if (above(verts, t, verts[v], 0.01f * epsilon)) {
                    extrude(tris[j]!!, v)
                }
            }
            // now check for those degenerate cases where we have a flipped triangle or a really skinny triangle
            j = tris.size
            while ((j--) != 0) {
                if (tris[j] == null) {
                    continue
                }
                if (!hasvert(tris[j]!!, v)) {
                    break
                }
                val nt: Int3 = tris[j]!!
                tmp1.setSub(verts[nt.y], verts[nt.x])
                tmp2.setSub(verts[nt.z], verts[nt.y])
                tmp.setCross(tmp1, tmp2)
                if (above(verts, nt, center, 0.01f * epsilon) || tmp.length() < epsilon * epsilon * 0.1) {
                    val nb = checkNotNull(tris[tris[j]!!.n.x])
                    assert(!hasvert(nb, v))
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
                triNormal(verts[t.x], verts[t.y], verts[t.z], n)
                t.maxValue = maxdirsterid(verts, vertsCount, n, allow)
                if (isextreme.get(t.maxValue) != 0) {
                    t.maxValue = -1 // already done that vertex - algorithm needs to be able to terminate.
                } else {
                    tmp.setSub(verts[t.maxValue], verts[t.x])
                    t.rise = n.dot(tmp)
                }
            }
            vlimit--
        }
        Stack.subVec(7)
        return 1
    }

    private fun findSimplex(verts: List<Vector3d>, vertsCount: Int, allow: IntArrayList, out: Int4): Int4 {
        val tmp = Stack.newVec()
        val tmp1 = Stack.newVec()
        val tmp2 = Stack.newVec()

        val basisX = Stack.newVec()
        val basisY = Stack.newVec()
        val basisZ = Stack.newVec()
        basisX.set(0.01, 0.02, 1.0)
        val p0: Int = maxdirsterid(verts, vertsCount, basisX, allow)
        tmp.setNegate(basisX)
        val p1: Int = maxdirsterid(verts, vertsCount, tmp, allow)
        basisX.setSub(verts[p0], verts[p1])
        if (p0 == p1 || (basisX.x == 0.0 && basisX.y == 0.0 && basisX.z == 0.0)) {
            out.set(-1, -1, -1, -1)
            Stack.subVec(6)
            return out
        }
        tmp.set(1.0, 0.02, 0.0)
        basisY.setCross(tmp, basisX)
        tmp.set(-0.02, 1.0, 0.0)
        basisZ.setCross(tmp, basisX)
        if (basisY.length() > basisZ.length()) {
            basisY.normalize()
        } else {
            basisY.set(basisZ)
            basisY.normalize()
        }
        var p2: Int = maxdirsterid(verts, vertsCount, basisY, allow)
        if (p2 == p0 || p2 == p1) {
            tmp.setNegate(basisY)
            p2 = maxdirsterid(verts, vertsCount, tmp, allow)
        }
        if (p2 == p0 || p2 == p1) {
            out.set(-1, -1, -1, -1)
            Stack.subVec(6)
            return out
        }
        basisY.setSub(verts[p2], verts[p0])
        basisZ.setCross(basisY, basisX)
        basisZ.normalize()
        var p3: Int = maxdirsterid(verts, vertsCount, basisZ, allow)
        if (p3 == p0 || p3 == p1 || p3 == p2) {
            tmp.setNegate(basisZ)
            p3 = maxdirsterid(verts, vertsCount, tmp, allow)
        }
        if (p3 == p0 || p3 == p1 || p3 == p2) {
            out.set(-1, -1, -1, -1)
            Stack.subVec(6)
            return out
        }

        tmp1.setSub(verts[p1], verts[p0])
        tmp2.setSub(verts[p2], verts[p0])
        tmp2.setCross(tmp1, tmp2)
        tmp1.setSub(verts[p3], verts[p0])
        if (tmp1.dot(tmp2) < 0) {
            val tmp = p2
            p2 = p3
            p3 = tmp
        }
        out.set(p0, p1, p2, p3)
        Stack.subVec(6)
        return out
    }

    //private ConvexH convexHCrop(ConvexH convex,Plane slice);
    private fun extrude(t0: Tri, v: Int) {
        val t = Int3(t0)
        val n = tris.size
        val ta = allocateTriangle(v, t.y, t.z)
        ta.n.set(t0.n.x, n + 1, n + 2)
        tris[t0.n.x]!!.neibSet(t.y, t.z, n)
        val tb = allocateTriangle(v, t.z, t.x)
        tb.n.set(t0.n.y, n + 2, n)
        tris[t0.n.y]!!.neibSet(t.z, t.x, n + 1)
        val tc = allocateTriangle(v, t.x, t.y)
        tc.n.set(t0.n.z, n, n + 1)
        tris[t0.n.z]!!.neibSet(t.x, t.y, n + 2)
        checkit(ta)
        checkit(tb)
        checkit(tc)
        if (hasvert(tris[ta.n.x]!!, v)) {
            removeb2b(ta, tris[ta.n.x]!!)
        }
        if (hasvert(tris[tb.n.x]!!, v)) {
            removeb2b(tb, tris[tb.n.x]!!)
        }
        if (hasvert(tris[tc.n.x]!!, v)) {
            removeb2b(tc, tris[tc.n.x]!!)
        }
        deAllocateTriangle(t0)
    }

    //private ConvexH test_cube();
    /**
     * BringOutYourDead (John Ratcliff): When you create a convex hull you hand it a large input set of vertices forming a 'point cloud'.
     * After the hull is generated it give you back a set of polygon faces which index the *original* point cloud.
     * The thing is, often times, there are many 'dead vertices' in the point cloud that are on longer referenced by the hull.
     * The routine 'BringOutYourDead' find only the referenced vertices, copies them to an new buffer, and re-indexes the hull so that it is a minimal representation.
     */
    private fun bringOutYourDead(
        inputVertices: List<Vector3d>, numInputVertices: Int,
        outputVertices: List<Vector3d>, numOutputVertices: IntArray,
        indices: IntArrayList, numIndices: Int
    ) {
        val tmpIndices = IntArrayList(vertexIndexMapping.size())
        repeat(vertexIndexMapping.size()) {
            tmpIndices.add(vertexIndexMapping.size())
        }

        val usedIndices = IntArrayList()
        MiscUtil.resize(usedIndices, numInputVertices, 0)

        /*
		JAVA NOTE: redudant
		for (int i=0; i<vcount; i++) {
		usedIndices.set(i, 0);
		}
		*/
        numOutputVertices[0] = 0

        for (i in 0 until numIndices) {
            val v = indices.get(i) // original array index

            assert(v >= 0 && v < numInputVertices)

            if (usedIndices.get(v) != 0) { // if already remapped
                indices.set(i, usedIndices.get(v) - 1) // index to new array
            } else {
                indices.set(i, numOutputVertices[0]) // new index mapping

                outputVertices[numOutputVertices[0]].set(inputVertices[v]) // copy old vert to new vert array

                for (k in 0 until vertexIndexMapping.size()) {
                    if (tmpIndices.get(k) == v) {
                        vertexIndexMapping.set(k, numOutputVertices[0])
                    }
                }

                numOutputVertices[0]++ // increment output vert count
                assert(numOutputVertices[0] >= 0 && numOutputVertices[0] <= numInputVertices)
                usedIndices.set(v, numOutputVertices[0]) // assign new index remapping
            }
        }
    }

    private fun cleanupVertices(
        svcount: Int,
        svertices: List<Vector3d>,
        vcount: IntArray,  // output number of vertices
        vertices: List<Vector3d>,  // location to store the results.
        normalepsilon: Double,
        scale: Vector3d?
    ): Boolean {
        if (svcount == 0) {
            return false
        }

        vertexIndexMapping.clear()

        vcount[0] = 0

        val recip = DoubleArray(3)

        scale?.set(1.0, 1.0, 1.0)

        val bmin = Vector3d(Double.Companion.MAX_VALUE, Double.Companion.MAX_VALUE, Double.Companion.MAX_VALUE)
        val bmax = Vector3d(-Double.Companion.MAX_VALUE, -Double.Companion.MAX_VALUE, -Double.Companion.MAX_VALUE)

        var vtxPtr = svertices
        var vtxIdx = 0

        repeat(svcount) {
            val p = vtxPtr[vtxIdx]

            vtxIdx++

            setMin(bmin, p)
            setMax(bmax, p)
        }

        var dx = bmax.x - bmin.x
        var dy = bmax.y - bmin.y
        var dz = bmax.z - bmin.z

        val center = Stack.newVec()

        center.x = dx * 0.5 + bmin.x
        center.y = dy * 0.5 + bmin.y
        center.z = dz * 0.5 + bmin.z

        if (dx < EPSILON || dy < EPSILON || dz < EPSILON || svcount < 3) {
            var len = Float.Companion.MAX_VALUE.toDouble()

            if (dx > EPSILON && dx < len) len = dx
            if (dy > EPSILON && dy < len) len = dy
            if (dz > EPSILON && dz < len) len = dz

            if (len == Float.Companion.MAX_VALUE.toDouble()) {
                dz = 0.01
                dy = dz
                dx = dy // one centimeter
            } else {
                if (dx < EPSILON) dx = len * 0.05f // 1/5th the shortest non-zero edge.

                if (dy < EPSILON) dy = len * 0.05f
                if (dz < EPSILON) dz = len * 0.05f
            }

            val x1 = center.x - dx
            val x2 = center.x + dx

            val y1 = center.y - dy
            val y2 = center.y + dy

            val z1 = center.z - dz
            val z2 = center.z + dz

            addPoint(vcount, vertices, x1, y1, z1)
            addPoint(vcount, vertices, x2, y1, z1)
            addPoint(vcount, vertices, x2, y2, z1)
            addPoint(vcount, vertices, x1, y2, z1)
            addPoint(vcount, vertices, x1, y1, z2)
            addPoint(vcount, vertices, x2, y1, z2)
            addPoint(vcount, vertices, x2, y2, z2)
            addPoint(vcount, vertices, x1, y2, z2)

            return true // return cube
        } else {
            if (scale != null) {
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
        }

        vtxPtr = svertices
        vtxIdx = 0

        repeat(svcount) {
            val p = vtxPtr[vtxIdx]
            vtxIdx +=  /*stride*/1

            var px = p.x
            var py = p.y
            var pz = p.z

            if (scale != null) {
                px = px * recip[0] // normalize
                py = py * recip[1] // normalize
                pz = pz * recip[2] // normalize
            }

            //		if ( 1 )
            run {
                var j = 0
                while (j < vcount[0]) {
                    // XXX might be broken
                    val v = vertices[j]

                    dx = abs(v.x - px)
                    dy = abs(v.y - py)
                    dz = abs(v.z - pz)

                    if (dx < normalepsilon && dy < normalepsilon && dz < normalepsilon) {
                        // ok, it is close enough to the old one
                        // now let us see if it is further from the center of the point cloud than the one we already recorded.
                        // in which case we keep this one instead.

                        val dist1: Double = getDist(px, py, pz, center)
                        val dist2: Double = getDist(v.x, v.y, v.z, center)

                        if (dist1 > dist2) {
                            v.x = px
                            v.y = py
                            v.z = pz
                        }

                        break
                    }
                    j++
                }

                if (j == vcount[0]) {
                    vertices[vcount[0]].set(px, py, pz)
                    vcount[0]++
                }
                vertexIndexMapping.add(j)
            }
        }

        // ok..now make sure we didn't prune so many vertices it is now invalid.
        //	if ( 1 )
        run {
            bmin.set(Double.Companion.MAX_VALUE, Double.Companion.MAX_VALUE, Double.Companion.MAX_VALUE)
            bmax.set(-Double.Companion.MAX_VALUE, -Double.Companion.MAX_VALUE, -Double.Companion.MAX_VALUE)

            for (i in 0 until vcount[0]) {
                val p = vertices[i]
                setMin(bmin, p)
                setMax(bmax, p)
            }

            dx = bmax.x - bmin.x
            dy = bmax.y - bmin.y
            dz = bmax.z - bmin.z
            if (dx < EPSILON || dy < EPSILON || dz < EPSILON || vcount[0] < 3) {
                val cx = dx * 0.5 + bmin.x
                val cy = dy * 0.5 + bmin.y
                val cz = dz * 0.5 + bmin.z

                var len = Float.Companion.MAX_VALUE.toDouble()

                if (dx >= EPSILON && dx < len) len = dx
                if (dy >= EPSILON && dy < len) len = dy
                if (dz >= EPSILON && dz < len) len = dz

                if (len == Float.Companion.MAX_VALUE.toDouble()) {
                    dz = 0.01
                    dy = dz
                    dx = dy // one centimeter
                } else {
                    if (dx < EPSILON) dx = len * 0.05f // 1/5th the shortest non-zero edge.

                    if (dy < EPSILON) dy = len * 0.05f
                    if (dz < EPSILON) dz = len * 0.05f
                }

                val x1 = cx - dx
                val x2 = cx + dx

                val y1 = cy - dy
                val y2 = cy + dy

                val z1 = cz - dz
                val z2 = cz + dz

                vcount[0] = 0 // add box

                addPoint(vcount, vertices, x1, y1, z1)
                addPoint(vcount, vertices, x2, y1, z1)
                addPoint(vcount, vertices, x2, y2, z1)
                addPoint(vcount, vertices, x1, y2, z1)
                addPoint(vcount, vertices, x1, y1, z2)
                addPoint(vcount, vertices, x2, y1, z2)
                addPoint(vcount, vertices, x2, y2, z2)
                addPoint(vcount, vertices, x1, y2, z2)

                return true
            }
        }

        return true
    }

    companion object {
        private const val EPSILON =
            0.000001 /* close enough to consider two btScalaring point numbers to be 'the same'. */

        /** ///////////////////////////////////////////////////////////////////////// */
        private fun hasvert(t: Int3, v: Int): Boolean {
            return (t.x == v || t.y == v || t.z == v)
        }

        private fun findOrthogonalVector(v: Vector3d, out: Vector3d) {
            val vCrossZ = Stack.newVec()
            vCrossZ.set(0.0, 0.0, 1.0)
            vCrossZ.setCross(v, vCrossZ)

            val vCrossY = Stack.newVec()
            vCrossY.set(0.0, 1.0, 0.0)
            vCrossY.setCross(v, vCrossY)

            if (vCrossZ.length() > vCrossY.length()) {
                out.setNormalize(vCrossZ)
            } else {
                out.setNormalize(vCrossY)
            }
            Stack.subVec(2)
        }

        private fun maxdirfiltered(p: List<Vector3d>, count: Int, dir: Vector3d, allow: IntArrayList): Int {
            assert(count != 0)
            var m = -1
            for (i in 0 until count) {
                if (allow.get(i) != 0) {
                    if (m == -1 || p[i].dot(dir) > p[m].dot(dir)) {
                        m = i
                    }
                }
            }
            assert(m != -1)
            return m
        }

        private fun maxdirsterid(p: List<Vector3d>, count: Int, dir: Vector3d, allow: IntArrayList): Int {
            val tmp = Stack.newVec()
            val tmp1 = Stack.newVec()
            val tmp2 = Stack.newVec()
            val u = Stack.newVec()
            val v = Stack.newVec()

            while (true) {
                val m: Int = maxdirfiltered(p, count, dir, allow)
                if (allow.get(m) == 3) {
                    Stack.subVec(5)
                    return m
                }
                findOrthogonalVector(dir, u)
                v.setCross(u, dir)
                var ma = -1
                var x = 0.0
                while (x <= 360.0) {
                    var s = sin(BulletGlobals.SIMD_RADS_PER_DEG * (x))
                    var c = cos(BulletGlobals.SIMD_RADS_PER_DEG * (x))

                    tmp1.setScale(s, u)
                    tmp2.setScale(c, v)
                    tmp.setAdd(tmp1, tmp2)
                    tmp.mul(0.025)
                    tmp.add(dir)
                    val mb: Int = maxdirfiltered(p, count, tmp, allow)
                    if (ma == m && mb == m) {
                        allow.set(m, 3)
                        Stack.subVec(5)
                        return m
                    }
                    if (ma != -1 && ma != mb) { // Yuck - this is really ugly
                        var mc = ma
                        var xx = x - 40.0
                        while (xx <= x) {
                            s = sin(BulletGlobals.SIMD_RADS_PER_DEG * (xx))
                            c = cos(BulletGlobals.SIMD_RADS_PER_DEG * (xx))

                            tmp1.setScale(s, u)
                            tmp2.setScale(c, v)
                            tmp.setAdd(tmp1, tmp2)
                            tmp.mul(0.025)
                            tmp.add(dir)

                            val md: Int = maxdirfiltered(p, count, tmp, allow)
                            if (mc == m && md == m) {
                                allow.set(m, 3)
                                Stack.subVec(5)
                                return m
                            }
                            mc = md
                            xx += 5.0
                        }
                    }
                    ma = mb
                    x += 45.0
                }
                allow.set(m, 0)
            }
        }

        private fun triNormal(v0: Vector3d, v1: Vector3d, v2: Vector3d, out: Vector3d): Vector3d {
            val cross = Stack.borrowVec()

            // return the normal of the triangle
            // inscribed by v0, v1, and v2
            cross.setSub(v1, v0)
            out.setSub(v2, v1)
            cross.setCross(cross, out)
            val m = cross.length()
            if (m == 0.0) {
                out.set(1.0, 0.0, 0.0)
                return out
            }
            out.setScale(1.0 / m, cross)
            return out
        }

        private fun above(vertices: List<Vector3d>, t: Int3, p: Vector3d, epsilon: Double): Boolean {
            val n = triNormal(vertices[t.x], vertices[t.y], vertices[t.z], Stack.newVec())
            val tmp = Stack.newVec()
            tmp.setSub(p, vertices[t.x])
            Stack.subVec(2)
            return (n.dot(tmp) > epsilon) // EPSILON???
        }

        private fun releaseHull(result: PHullResult) {
            if (result.indices.size() != 0) {
                result.indices.clear()
            }

            result.vertexCount = 0
            result.indexCount = 0
            result.vertices = null
        }

        private fun addPoint(vertexCount: IntArray, p: List<Vector3d>, x: Double, y: Double, z: Double) {
            // XXX, might be broken
            p[vertexCount[0]].set(x, y, z)
            vertexCount[0]++
        }

        private fun getDist(px: Double, py: Double, pz: Double, p2: Vector3d): Double {
            val dx = px - p2.x
            val dy = py - p2.y
            val dz = pz - p2.z
            return dx * dx + dy * dy + dz * dz
        }
    }
}
