package org.recast4j.recast

import org.joml.Vector3f
import org.recast4j.IntArrayList
import org.recast4j.Vectors.clamp
import org.recast4j.recast.RecastCommon.getCon
import org.recast4j.recast.RecastCommon.getDirForOffset
import org.recast4j.recast.RecastCommon.getDirOffsetX
import org.recast4j.recast.RecastCommon.getDirOffsetY
import org.recast4j.recast.RecastConstants.RC_MESH_NULL_IDX
import org.recast4j.recast.RecastConstants.RC_MULTIPLE_REGS
import org.recast4j.recast.RecastConstants.RC_NOT_CONNECTED
import org.recast4j.recast.RecastConstants.SPAN_MAX_HEIGHT
import org.recast4j.recast.RecastMesh.next
import org.recast4j.recast.RecastMesh.prev
import kotlin.math.*

object RecastMeshDetail {

    private class HeightPatch(val data: IntArray) {
        var xmin = 0
        var ymin = 0
        var width = 0
        var height = 0
    }

    var MAX_VERTS = 127

    /**
     * Max tris for delaunay is 2n-2-k (n=num vertices, k=num hull vertices);
     * must not be more than 256, because we have to fix the data into a byte
     * */
    var MAX_TRIS = 255

    var MAX_VERTS_PER_EDGE = 32

    var RC_UNSET_HEIGHT = SPAN_MAX_HEIGHT
    const val EV_UNDEF = -1
    const val EV_HULL = -2

    val offset = intArrayOf(0, 0, -1, -1, 0, -1, 1, -1, 1, 0, 1, 1, 0, 1, -1, 1, -1, 0)

    @JvmStatic
    fun vdot2(a: Vector3f, b: Vector3f): Float {
        return a.x * b.x + a.z * b.z
    }

    @JvmStatic
    fun vdistSq2(vertices: FloatArray, p: Int, q: Int): Float {
        val dx = vertices[q] - vertices[p]
        val dy = vertices[q + 2] - vertices[p + 2]
        return dx * dx + dy * dy
    }

    @JvmStatic
    fun vdist2(vertices: FloatArray, p: Int, q: Int): Float {
        return sqrt(vdistSq2(vertices, p, q))
    }

    @JvmStatic
    fun vdistSq2(p: Vector3f, q: Vector3f): Float {
        val dx = q.x - p.x
        val dy = q.z - p.z
        return dx * dx + dy * dy
    }

    @JvmStatic
    fun vdist2(p: Vector3f, q: Vector3f): Float {
        return sqrt(vdistSq2(p, q))
    }

    @JvmStatic
    fun vdistSq2(p: Vector3f, vertices: FloatArray, q: Int): Float {
        val dx = vertices[q] - p.x
        val dy = vertices[q + 2] - p.z
        return dx * dx + dy * dy
    }

    @JvmStatic
    fun vdist2(p: Vector3f, vertices: FloatArray, q: Int): Float {
        return sqrt(vdistSq2(p, vertices, q))
    }

    @JvmStatic
    fun vcross2(vertices: FloatArray, p1: Int, p2: Int, p3: Int): Float {
        val u1 = vertices[p2] - vertices[p1]
        val v1 = vertices[p2 + 2] - vertices[p1 + 2]
        val u2 = vertices[p3] - vertices[p1]
        val v2 = vertices[p3 + 2] - vertices[p1 + 2]
        return u1 * v2 - v1 * u2
    }

    @JvmStatic
    fun vcross2(p1: Vector3f, p2: Vector3f, p3: Vector3f): Float {
        val u1 = p2.x - p1.x
        val v1 = p2.z - p1.z
        val u2 = p3.x - p1.x
        val v2 = p3.z - p1.z
        return u1 * v2 - v1 * u2
    }

    @JvmStatic
    fun sub(dst: Vector3f, data: FloatArray, p1: Int, p2: Int) {
        dst.set(data[p1] - data[p2], data[p1 + 1] - data[p2 + 1], data[p1 + 2] - data[p2 + 2])
    }

    @JvmStatic
    fun sub(dst: Vector3f, a: Vector3f, b: FloatArray, bi: Int) {
        dst.set(a.x - b[bi], a.y - b[bi + 1], a.z - b[bi + 2])
    }

    @JvmStatic
    fun add(dst: Vector3f, a: Vector3f, b: FloatArray, bi: Int) {
        dst.set(a.x - b[bi], a.y - b[bi + 1], a.z - b[bi + 2])
    }

    @JvmStatic
    fun copy(dst: Vector3f, a: FloatArray, ai: Int) {
        dst.set(a[ai], a[ai + 1], a[ai + 2])
    }

    @JvmStatic
    fun copy(dst: FloatArray, di: Int, a: FloatArray, ai: Int) {
        dst[di] = a[ai]
        dst[di + 1] = a[ai + 1]
        dst[di + 2] = a[ai + 2]
    }

    @JvmStatic
    fun copy(dst: FloatArray, di: Int, a: Vector3f) {
        dst[di] = a.x
        dst[di + 1] = a.y
        dst[di + 2] = a.z
    }

    @JvmStatic
    fun findEdge(edges: IntArrayList, s: Int, t: Int): Int {
        var e = 0
        while (e < edges.size) {
            if ((edges[e] == s && edges[e + 1] == t) || (edges[e] == t && edges[e + 1] == s)) {
                return e shr 2
            }
            e += 4
        }
        return EV_UNDEF
    }

    @JvmStatic
    fun addEdge(edges: IntArrayList, maxEdges: Int, s: Int, t: Int, l: Int, r: Int) {
        if (edges.size shr 2 >= maxEdges) {
            throw RuntimeException("addEdge: Too many edges (" + (edges.size / 4) + "/" + maxEdges + ").")
        }

        // Add edge if not already in the triangulation.
        val e = findEdge(edges, s, t)
        if (e == EV_UNDEF) {
            edges.add(s)
            edges.add(t)
            edges.add(l)
            edges.add(r)
        }
    }

    @JvmStatic
    fun getEdgeFlags(vertices: FloatArray, va: Int, vb: Int, vpoly: FloatArray, npoly: Int): Int {
        // The flag returned by this function matches getDetailTriEdgeFlags in Detour.
        // Figure out if edge (va,vb) is part of the polygon boundary.
        val thrSqr = 0.001f * 0.001f
        var i = 0
        var j = npoly - 1
        while (i < npoly) {
            if (distancePtSeg2d(vertices, va, vpoly, j * 3, i * 3) < thrSqr
                && distancePtSeg2d(vertices, vb, vpoly, j * 3, i * 3) < thrSqr
            ) return 1
            j = i++
        }
        return 0
    }

    @JvmStatic
    fun getTriFlags(vertices: FloatArray, va: Int, vb: Int, vc: Int, vpoly: FloatArray, npoly: Int): Int {
        var flags = getEdgeFlags(vertices, va, vb, vpoly, npoly)
        flags = flags or (getEdgeFlags(vertices, vb, vc, vpoly, npoly) shl 2)
        flags = flags or (getEdgeFlags(vertices, vc, va, vpoly, npoly) shl 4)
        return flags
    }

    @JvmStatic
    fun distToTriMesh(p: Vector3f, vertices: FloatArray, tris: IntArrayList, ntris: Int): Float {
        var dmin = Float.MAX_VALUE
        for (i in 0 until ntris) {
            val va = tris[i * 4] * 3
            val vb = tris[i * 4 + 1] * 3
            val vc = tris[i * 4 + 2] * 3
            val d = distPtTri(p, vertices, va, vb, vc)
            if (d < dmin) {
                dmin = d
            }
        }
        return if (dmin == Float.MAX_VALUE) -1f else dmin
    }

    @JvmStatic
    fun distToPoly(nvert: Int, vertices: FloatArray, p: Vector3f): Float {
        var dmin = Float.MAX_VALUE
        var c = false
        val px = p.x
        val pz = p.z
        var j = nvert - 1
        for (i in 0 until nvert) {
            val vi = i * 3
            val vj = j * 3
            if (((vertices[vi + 2] > pz) != (vertices[vj + 2] > pz)) &&
                (px < (vertices[vj] - vertices[vi]) * (pz - vertices[vi + 2]) / (vertices[vj + 2] - vertices[vi + 2]) + vertices[vi])
            ) c = !c
            dmin = min(dmin, distancePtSeg2d(p, vertices, vj, vi))
            j = i
        }
        return if (c) -dmin else dmin
    }

    @JvmStatic
    fun getJitterX(i: Int): Float {
        return (i * -0x72594cbd and 0xffff) / 65535f * 2f - 1f
    }

    @JvmStatic
    fun getJitterY(i: Int): Float {
        return (i * -0x27e9c7bf and 0xffff) / 65535f * 2f - 1f
    }

    @JvmStatic
    fun min(dst: Vector3f, a: FloatArray, ai: Int) {
        dst.set(min(dst.x, a[ai]), min(dst.y, a[ai + 1]), min(dst.z, a[ai + 2]))
    }

    @JvmStatic
    fun max(dst: Vector3f, a: FloatArray, ai: Int) {
        dst.set(
            max(dst.x, a[ai]),
            max(dst.y, a[ai + 1]),
            max(dst.z, a[ai + 2])
        )
    }

    @JvmStatic
    fun distPtTri(p: Vector3f, vertices: FloatArray, a: Int, b: Int, c: Int): Float {
        val v0 = Vector3f()
        val v1 = Vector3f()
        val v2 = Vector3f()
        sub(v0, vertices, c, a)
        sub(v1, vertices, b, a)
        sub(v2, p, vertices, a)
        val dot00 = vdot2(v0, v0)
        val dot01 = vdot2(v0, v1)
        val dot02 = vdot2(v0, v2)
        val dot11 = vdot2(v1, v1)
        val dot12 = vdot2(v1, v2)

        // Compute barycentric coordinates
        val invDenom = 1f / (dot00 * dot11 - dot01 * dot01)
        val u = (dot11 * dot02 - dot01 * dot12) * invDenom
        val v = (dot00 * dot12 - dot01 * dot02) * invDenom

        // If point lies inside the triangle, return interpolated y-coord.
        val epsilon = 1e-4f
        if (u >= -epsilon && v >= -epsilon && (u + v) <= 1 + epsilon) {
            val y = vertices[a + 1] + v0.y * u + v1.y * v
            return abs(y - p.y)
        }
        return Float.MAX_VALUE
    }

    /** Calculate minimum extend of the polygon. */
    @JvmStatic
    fun polyMinExtent(vertices: FloatArray, numVertices: Int): Float {
        var minDist = Float.MAX_VALUE
        for (i in 0 until numVertices) {
            val ni = (i + 1) % numVertices
            val p1 = i * 3
            val p2 = ni * 3
            var maxEdgeDist = 0f
            for (j in 0 until numVertices) {
                if (j == i || j == ni) {
                    continue
                }
                val d = distancePtSeg2d(vertices, j * 3, vertices, p1, p2)
                maxEdgeDist = max(maxEdgeDist, d)
            }
            minDist = min(minDist, maxEdgeDist)
        }
        return sqrt(minDist)
    }

    private fun td(t0: Float, d: Float): Float {
        var t = t0
        if (d > 0) {
            t /= d
        }
        if (t < 0) {
            t = 0f
        } else if (t > 1) {
            t = 1f
        }
        return t
    }

    @JvmStatic
    fun distancePtSeg2d(vx: Float, vz: Float, poly: FloatArray, p: Int, q: Int): Float {
        val pqx = poly[q] - poly[p]
        val pqz = poly[q + 2] - poly[p + 2]
        var dx = vx - poly[p]
        var dz = vz - poly[p + 2]
        val d = pqx * pqx + pqz * pqz
        val t = td(pqx * dx + pqz * dz, d)
        dx = poly[p] + t * pqx - vx
        dz = poly[p + 2] + t * pqz - vz
        return dx * dx + dz * dz
    }

    @JvmStatic
    fun distancePtSeg2d(vertices: FloatArray, pt: Int, poly: FloatArray, p: Int, q: Int): Float {
        return distancePtSeg2d(vertices[pt], vertices[pt + 2], poly, p, q)
    }

    @JvmStatic
    fun distancePtSeg2d(v: Vector3f, poly: FloatArray, p: Int, q: Int): Float {
        return distancePtSeg2d(v.x, v.z, poly, p, q)
    }

    @JvmStatic
    fun distancePtSeg(vertices: FloatArray, pt: Int, p: Int, q: Int): Float {
        val pqx = vertices[q] - vertices[p]
        val pqy = vertices[q + 1] - vertices[p + 1]
        val pqz = vertices[q + 2] - vertices[p + 2]
        var dx = vertices[pt] - vertices[p]
        var dy = vertices[pt + 1] - vertices[p + 1]
        var dz = vertices[pt + 2] - vertices[p + 2]
        val d = pqx * pqx + pqy * pqy + pqz * pqz
        val t = td(pqx * dx + pqy * dy + pqz * dz, d)
        dx = vertices[p] + t * pqx - vertices[pt]
        dy = vertices[p + 1] + t * pqy - vertices[pt + 1]
        dz = vertices[p + 2] + t * pqz - vertices[pt + 2]
        return dx * dx + dy * dy + dz * dz
    }

    @JvmStatic
    fun circumcircle(vertices: FloatArray, p1: Int, p2: Int, p3: Int, c: Vector3f): Float {
        val epsilon = 1e-6f
        // Calculate the circle relative to p1, to avoid some precision issues.
        val v1 = Vector3f()
        val v2 = Vector3f()
        val v3 = Vector3f()
        sub(v2, vertices, p2, p1)
        sub(v3, vertices, p3, p1)
        val cp = vcross2(v1, v2, v3)
        return if (abs(cp) > epsilon) {
            val v1Sq = vdot2(v1, v1)
            val v2Sq = vdot2(v2, v2)
            val v3Sq = vdot2(v3, v3)
            val n = 0.5f / cp
            c.set(
                (v1Sq * (v2.z - v3.z) + v2Sq * (v3.z - v1.z) + v3Sq * (v1.z - v2.z)) * n,
                0f,
                (v1Sq * (v3.x - v2.x) + v2Sq * (v1.x - v3.x) + v3Sq * (v2.x - v1.x)) * n
            )
            val r = vdist2(c, v1)
            add(c, c, vertices, p1)
            r
        } else {
            copy(c, vertices, p1)
            0f
        }
    }

    @JvmStatic
    fun updateLeftFace(edges0: IntArrayList, e: Int, s: Int, t: Int, f: Int) {
        val edges = edges0.values
        if (edges[e] == s && edges[e + 1] == t && edges[e + 2] == EV_UNDEF) {
            edges[e + 2] = f
        } else if (edges[e + 1] == s && edges[e] == t && edges[e + 3] == EV_UNDEF) {
            edges[e + 3] = f
        }
    }

    @JvmStatic
    fun overlapSegSeg2d(vertices: FloatArray, a: Int, b: Int, c: Int, d: Int): Boolean {
        val a1 = vcross2(vertices, a, b, d)
        val a2 = vcross2(vertices, a, b, c)
        if (a1 * a2 < 0f) {
            val a3 = vcross2(vertices, c, d, a)
            val a4 = a3 + a2 - a1
            return a3 * a4 < 0f
        }
        return false
    }

    @JvmStatic
    fun doesNotOverlapEdges(points: FloatArray, edges: IntArrayList, s1: Int, t1: Int): Boolean {
        val edgeData = edges.values
        var e = 0
        val l = edges.size
        while (e < l) {
            val s0 = edgeData[e]
            val t0 = edgeData[e + 1]
            // Same or connected edges do not overlap.
            if ((s0 == s1) || (s0 == t1) || (t0 == s1) || (t0 == t1)) {
                e += 4
                continue
            }
            if (overlapSegSeg2d(points, s0 * 3, t0 * 3, s1 * 3, t1 * 3)) {
                return false
            }
            e += 4
        }
        return true
    }

    @JvmStatic
    private fun getHeight(fx: Float, fy: Float, fz: Float, ics: Float, ch: Float, radius: Int, hp: HeightPatch): Int {
        var ix = floor(fx * ics + 0.01f).toInt()
        var iz = floor(fz * ics + 0.01f).toInt()
        ix = clamp(ix - hp.xmin, 0, hp.width - 1)
        iz = clamp(iz - hp.ymin, 0, hp.height - 1)
        var h = hp.data[ix + iz * hp.width]
        if (h == RC_UNSET_HEIGHT) {
            // Special case when data might be bad.
            // Walk adjacent cells in a spiral up to 'radius', and look
            // for a pixel, which has a valid height.
            var x = 1
            var z = 0
            var dx = 1
            var dz = 0
            val maxSize = radius * 2 + 1
            val maxIter = maxSize * maxSize - 1
            var nextRingIterStart = 8
            var nextRingIterations = 16
            var dmin = Float.MAX_VALUE
            for (i in 0 until maxIter) {
                val nx = ix + x
                val nz = iz + z
                if ((nx >= 0) && (nz >= 0) && (nx < hp.width) && (nz < hp.height)) {
                    val nh = hp.data[nx + nz * hp.width]
                    if (nh != RC_UNSET_HEIGHT) {
                        val d = abs(nh * ch - fy)
                        if (d < dmin) {
                            h = nh
                            dmin = d
                        }
                    }
                }

                // We are searching in a grid, which looks approximately like this:
                // __________
                // |2 ______ 2|
                // | |1 __ 1| |
                // | | |__| | |
                // | |______| |
                // |__________|
                // We want to find the best height as close to the center cell as possible. This means that
                // if we find a height in one of the neighbor cells to the center, we don't want to
                // expand further out than the 8 neighbors - we want to limit our search to the closest
                // of these "rings", but the best height in the ring.
                // For example, the center is just 1 cell. We checked that at the entrance to the function.
                // The next "ring" contains 8 cells (marked 1 above). Those are all the neighbors to the center cell.
                // The next one again contains 16 cells (marked 2). In general each ring has 8 additional cells, which
                // can be thought of as adding 2 cells around the "center" of each side when we expand the ring.
                // Here we detect if we are about to enter the next ring, and if we are and we have found
                // a height, we abort the search.
                if (i + 1 == nextRingIterStart) {
                    if (h != RC_UNSET_HEIGHT) {
                        break
                    }
                    nextRingIterStart += nextRingIterations
                    nextRingIterations += 8
                }
                if ((x == z) || ((x < 0) && (x == -z)) || ((x > 0) && (x == 1 - z))) {
                    val tmp = dx
                    dx = -dz
                    dz = tmp
                }
                x += dx
                z += dz
            }
        }
        return h
    }

    @JvmStatic
    fun triangulateHull(vertices: FloatArray, nhull: Int, hull: IntArray, nin: Int, tris: IntArrayList) {
        var start = 0
        var left = 1
        var right = nhull - 1

        // Start from an ear with the shortest perimeter.
        // This tends to favor well-formed triangles as starting point.
        var dmin = Float.MAX_VALUE
        for (i in 0 until nhull) {
            if (hull[i] >= nin) {
                continue  // Ears are triangles with original vertices as middle vertex while others are actually line
            }
            // segments on edges
            val pi = prev(i, nhull)
            val ni = next(i, nhull)
            val pv = hull[pi] * 3
            val cv = hull[i] * 3
            val nv = hull[ni] * 3
            val d = vdist2(vertices, pv, cv) + vdist2(vertices, cv, nv) + vdist2(vertices, nv, pv)
            if (d < dmin) {
                start = i
                left = ni
                right = pi
                dmin = d
            }
        }

        // Add first triangle
        tris.add(hull[start])
        tris.add(hull[left])
        tris.add(hull[right])
        tris.add(0)

        // Triangulate the polygon by moving left or right,
        // depending on which triangle has shorter perimeter.
        // This heuristic was chose emprically, since it seems
        // handle tesselated straight edges well.
        while (next(left, nhull) != right) {
            // Check to see if se should advance left or right.
            val nleft = next(left, nhull)
            val nright = prev(right, nhull)
            val cvleft = hull[left] * 3
            val nvleft = hull[nleft] * 3
            val cvright = hull[right] * 3
            val nvright = hull[nright] * 3
            val dleft = vdist2(vertices, cvleft, nvleft) + vdist2(vertices, nvleft, cvright)
            val dright = vdist2(vertices, cvright, nvright) + vdist2(vertices, cvleft, nvright)
            tris.add(hull[left])
            if (dleft < dright) {
                tris.add(hull[nleft])
                tris.add(hull[right])
                tris.add(0)
                left = nleft
            } else {
                tris.add(hull[nright])
                tris.add(hull[right])
                tris.add(0)
                right = nright
            }
        }
    }

    @JvmStatic
    fun buildPolyMeshDetail(
        ctx: Telemetry?, mesh: PolyMesh, chf: CompactHeightfield,
        sampleDist: Float, sampleMaxError: Float
    ): PolyMeshDetail? {
        ctx?.startTimer(TelemetryType.POLYMESHDETAIL)
        if (mesh.numVertices == 0 || mesh.numPolygons == 0) {
            return null
        }
        val nvp = mesh.maxVerticesPerPolygon
        val cs = mesh.cellSize
        val ch = mesh.cellHeight
        val orig = mesh.bmin
        val borderSize = mesh.borderSize
        val heightSearchRadius = max(1f, ceil(mesh.maxEdgeError)).toInt()
        val tris = IntArrayList(512)
        val vertices = FloatArray(256 * 3)
        var nPolyVerts = 0
        var maxhw = 0
        var maxhh = 0
        val bounds = IntArray(mesh.numPolygons * 4)
        val poly = FloatArray(nvp * 3)

        // Find max size for a polygon area.
        for (i in 0 until mesh.numPolygons) {
            val p = i * nvp * 2
            var minX = chf.width
            var maxX = 0
            var minY = chf.height
            var maxY = 0
            val meshVerts = mesh.vertices
            val meshPolys = mesh.polygons
            val nullIdx = RC_MESH_NULL_IDX
            for (j in 0 until nvp) {
                if (meshPolys[p + j] == nullIdx) {
                    break
                }
                val v = meshPolys[p + j] * 3
                minX = min(minX, meshVerts[v])
                maxX = max(maxX, meshVerts[v])
                minY = min(minY, meshVerts[v + 2])
                maxY = max(maxY, meshVerts[v + 2])
                nPolyVerts++
            }
            bounds[i * 4] = max(0, minX - 1)
            bounds[i * 4 + 1] = min(chf.width, maxX + 1)
            bounds[i * 4 + 2] = max(0, minY - 1)
            bounds[i * 4 + 3] = min(chf.height, maxY + 1)
            if (bounds[i * 4] >= bounds[i * 4 + 1] || bounds[i * 4 + 2] >= bounds[i * 4 + 3]) {
                continue
            }
            maxhw = max(maxhw, bounds[i * 4 + 1] - bounds[i * 4])
            maxhh = max(maxhh, bounds[i * 4 + 3] - bounds[i * 4 + 2])
        }
        val hp = HeightPatch(IntArray(maxhw * maxhh))
        var vcap = nPolyVerts + nPolyVerts / 2
        var tcap = vcap * 2
        val dmesh = PolyMeshDetail(IntArray(mesh.numPolygons * 4), FloatArray(vcap * 3), ByteArray(tcap * 4))
        dmesh.numSubMeshes = mesh.numPolygons
        val meshPolygons = mesh.polygons
        for (i in 0 until mesh.numPolygons) {
            val p = i * nvp * 2

            // Store polygon vertices for processing.
            var npoly = 0
            val meshVerts = mesh.vertices
            val nullIdx = RC_MESH_NULL_IDX

            for (j in 0 until nvp) {
                val v = meshPolygons[p + j]
                if (v == nullIdx) break
                val v3 = v * 3
                val j3 = j * 3
                poly[j3] = meshVerts[v3] * cs
                poly[j3 + 1] = meshVerts[v3 + 1] * ch
                poly[j3 + 2] = meshVerts[v3 + 2] * cs
                npoly++
            }

            // Get the height data from the area of the polygon.
            hp.xmin = bounds[i * 4]
            hp.ymin = bounds[i * 4 + 2]
            hp.width = bounds[i * 4 + 1] - bounds[i * 4]
            hp.height = bounds[i * 4 + 3] - bounds[i * 4 + 2]
            getHeightData(
                chf, meshPolygons, p, npoly, mesh.vertices, borderSize, hp, mesh.regionIds[i]
            )

            // Build detail mesh.
            val nverts = buildPolyDetail(
                poly, npoly, sampleDist, sampleMaxError, heightSearchRadius, chf, hp,
                vertices, tris
            )

            // Move detail vertices to world space.
            for (j in 0 until nverts) {
                vertices[j * 3] += orig.x
                vertices[j * 3 + 1] += orig.y + chf.cellHeight // Is this offset necessary? See
                // https://groups.google.com/d/msg/recastnavigation/UQFN6BGCcV0/-1Ny4koOBpkJ
                vertices[j * 3 + 2] += orig.z
            }

            // Offset poly too, will be used to flag checking.
            for (j in 0 until npoly) {
                poly[j * 3] += orig.x
                poly[j * 3 + 1] += orig.y
                poly[j * 3 + 2] += orig.z
            }

            // Store detail submesh.
            val ntris = tris.size shr 2
            val dmeshSubMeshes = dmesh.subMeshes
            dmeshSubMeshes[i * 4] = dmesh.numVertices
            dmeshSubMeshes[i * 4 + 1] = nverts
            dmeshSubMeshes[i * 4 + 2] = dmesh.numTriangles
            dmeshSubMeshes[i * 4 + 3] = ntris
            val dMeshNverts = dmesh.numVertices
            // Store vertices, allocate more memory if necessary.
            if (dMeshNverts + nverts > vcap) {
                vcap += max(dMeshNverts + nverts - vcap + 255 shr 8, 0) shl 8
                val newv = FloatArray(vcap * 3)
                if (dMeshNverts != 0) {
                    dmesh.vertices.copyInto(newv, 0, 0, 3 * dMeshNverts)
                }
                dmesh.vertices = newv
            }
            val dMeshVerts = dmesh.vertices
            System.arraycopy(vertices, 0, dMeshVerts, dMeshNverts * 3, nverts * 3)
            dmesh.numVertices = dMeshNverts + nverts

            // Store triangles, allocate more memory if necessary.
            if (dmesh.numTriangles + ntris > tcap) {
                tcap += max(dmesh.numTriangles + ntris - tcap + 255 shr 8, 0) shl 8
                val newt = ByteArray(tcap * 4)
                if (dmesh.numTriangles != 0) {
                    dmesh.triangles.copyInto(newt, 0, 0, 4 * dmesh.numTriangles)
                }
                dmesh.triangles = newt
            }
            val dmeshTris = dmesh.triangles
            var l = dmesh.numTriangles shl 2
            for (j in 0 until ntris) {
                val t = j * 4
                dmeshTris[l++] = tris[t].toByte()
                dmeshTris[l++] = tris[t + 1].toByte()
                dmeshTris[l++] = tris[t + 2].toByte()
                dmeshTris[l++] =
                    getTriFlags(vertices, tris[t] * 3, tris[t + 1] * 3, tris[t + 2] * 3, poly, npoly).toByte()
            }
            dmesh.numTriangles = l shr 2
        }
        ctx?.stopTimer(TelemetryType.POLYMESHDETAIL)
        return dmesh
    }

    private const val RETRACT_SIZE_X3 = 256 * 3

    @JvmStatic
    private fun getHeightData(
        chf: CompactHeightfield, meshpolys: IntArray,
        poly: Int, npoly: Int, vertices: IntArray, bs: Int,
        hp: HeightPatch, region: Int
    ) {
        // Note: Reads to the compact heightfield are offset by border size (bs)
        // since border size offset is already removed from the polymesh vertices.
        val queue = IntArrayList(512)
        hp.data.fill(RC_UNSET_HEIGHT, 0, hp.width * hp.height)
        var empty = true

        // We cannot sample from this poly if it was created from polys
        // of different regions. If it was then it could potentially be overlapping
        // with polys of that region, and the heights sampled here could be wrong.
        if (region != RC_MULTIPLE_REGS) {
            // Copy the height from the same region, and mark region borders as seed points to fill the rest.
            for (hy in 0 until hp.height) {
                val y = hp.ymin + hy + bs
                for (hx in 0 until hp.width) {
                    val x = hp.xmin + hx + bs
                    val c = x + y * chf.width
                    for (i in chf.index[c] until chf.endIndex[c]) {
                        val s = chf.spans[i]
                        if (s.regionId == region) {
                            // Store height
                            hp.data[hx + hy * hp.width] = s.y
                            empty = false
                            // If any of the neighbours is not in same region,
                            // add the current location as flood fill start
                            var border = false
                            for (dir in 0..3) {
                                if (getCon(s, dir) != RC_NOT_CONNECTED) {
                                    val ax = x + getDirOffsetX(dir)
                                    val ay = y + getDirOffsetY(dir)
                                    val ai = chf.index[ax + ay * chf.width] + getCon(s, dir)
                                    val span = chf.spans[ai]
                                    if (span.regionId != region) {
                                        border = true
                                        break
                                    }
                                }
                            }
                            if (border) {
                                queue.add(x)
                                queue.add(y)
                                queue.add(i)
                            }
                            break
                        }
                    }
                }
            }
        }

        // if the polygon does not contain any points from the current region (rare, but happens)
        // or if it could potentially be overlapping polygons of the same region,
        // then use the center as the seed point.
        if (empty) {
            seedArrayWithPolyCenter(chf, meshpolys, poly, npoly, vertices, bs, hp, queue)
        }

        // We assume the seed is centered in the polygon, so a BFS to collect
        // height data will ensure we do not move onto overlapping polygons and
        // sample wrong heights.
        var head = 0
        while (head < queue.size) {
            val cx = queue[head]
            val cy = queue[head + 1]
            val ci = queue[head + 2]
            head += 3
            if (head >= RETRACT_SIZE_X3) {
                head = 0
                queue.removeRange(0, RETRACT_SIZE_X3)
            }
            val cs = chf.spans[ci]
            for (dir in 0..3) {
                if (getCon(cs, dir) == RC_NOT_CONNECTED) {
                    continue
                }
                val ax = cx + getDirOffsetX(dir)
                val ay = cy + getDirOffsetY(dir)
                val hx = ax - hp.xmin - bs
                val hy = ay - hp.ymin - bs
                if ((hx < 0) || (hx >= hp.width) || (hy < 0) || (hy >= hp.height)) {
                    continue
                }
                if (hp.data[hx + hy * hp.width] != RC_UNSET_HEIGHT) {
                    continue
                }
                val ai = chf.index[ax + ay * chf.width] + getCon(cs, dir)
                val span = chf.spans[ai]
                hp.data[hx + hy * hp.width] = span.y
                queue.add(ax)
                queue.add(ay)
                queue.add(ai)
            }
        }
    }

    @JvmStatic
    fun delaunayHull(npts: Int, pts: FloatArray, nhull: Int, hull: IntArray, tris: IntArrayList) {
        var nfaces = 0
        val maxEdges = npts * 10
        val edges = IntArrayList(64)
        var j = nhull - 1
        for (i in 0 until nhull) {
            addEdge(edges, maxEdges, hull[j], hull[i], EV_HULL, EV_UNDEF)
            j = i
        }
        var currentEdge = 0
        while (currentEdge < (edges.size shr 2)) {
            if (edges[currentEdge * 4 + 2] == EV_UNDEF) {
                nfaces = completeFacet(pts, npts, edges, maxEdges, nfaces, currentEdge)
            }
            if (edges[currentEdge * 4 + 3] == EV_UNDEF) {
                nfaces = completeFacet(pts, npts, edges, maxEdges, nfaces, currentEdge)
            }
            currentEdge++
        }
        // Create tris
        tris.clear()
        tris.ensureExtra(nfaces * 4)
        tris.values.fill(-1, 0, nfaces * 4)
        tris.size = nfaces * 4
        val edgeData = edges.values
        val trisData = tris.values
        for (e in 0 until edges.size step 4) {
            if (edgeData[e + 3] >= 0) {
                // Left face
                val t = edgeData[e + 3] * 4
                if (trisData[t] == -1) {
                    trisData[t] = edgeData[e]
                    trisData[t + 1] = edgeData[e + 1]
                } else if (trisData[t] == edgeData[e + 1]) {
                    trisData[t + 2] = edgeData[e]
                } else if (trisData[t + 1] == edgeData[e]) {
                    trisData[t + 2] = edgeData[e + 1]
                }
            }
            if (edgeData[e + 2] >= 0) {
                // Right
                val t = edgeData[e + 2] * 4
                if (trisData[t] == -1) {
                    trisData[t] = edgeData[e + 1]
                    trisData[t + 1] = edgeData[e]
                } else if (trisData[t] == edgeData[e]) {
                    trisData[t + 2] = edgeData[e + 1]
                } else if (trisData[t + 1] == edgeData[e + 1]) {
                    trisData[t + 2] = edgeData[e]
                }
            }
        }
        var size = trisData.size
        var t = 0
        while (t < size) {
            if (trisData[t] == -1 || trisData[t + 1] == -1 || trisData[t + 2] == -1) {
                // System.err.println("Dangling! " + trisData[t] + " " + trisData[t + 1] + "  " + trisData[t + 2])
                trisData[t] = trisData[size - 4]
                trisData[t + 1] = trisData[size - 3]
                trisData[t + 2] = trisData[size - 2]
                trisData[t + 3] = trisData[size - 1]
                size -= 4
                t -= 4
            }
            t += 4
        }
        tris.size = size
    }

    @JvmStatic
    fun completeFacet(pts: FloatArray, npts: Int, edges: IntArrayList, maxEdges: Int, nfaces0: Int, e0: Int): Int {
        var nfaces = nfaces0
        var e = e0
        val epsilon = 1e-5f
        val edge = e * 4

        val x = edges[edge + 2] == EV_UNDEF
        val y = edges[edge + 3] == EV_UNDEF

        // Edge already completed.
        if (!x && !y) return nfaces

        // Cache s and t.
        val x01 = if (x) 1 else 0
        val s = edges[edge + 1 - x01]
        val t = edges[edge + x01]

        // Find best point on left of edge.
        var pt = npts
        val c = Vector3f()
        var r = -1f
        for (u in 0 until npts) {
            if (u != s && u != t && vcross2(pts, s * 3, t * 3, u * 3) > epsilon) {
                if (r < 0) {
                    // The circle is not updated yet, do it now.
                    pt = u
                    r = circumcircle(pts, s * 3, t * 3, u * 3, c)
                } else {
                    val d = vdist2(c, pts, u * 3)
                    val tol = 0.001f
                    if (d <= r * (1 + tol)) {
                        if (d < r * (1 - tol)) {
                            // Inside safe circumcircle, update circle.
                            pt = u
                            r = circumcircle(pts, s * 3, t * 3, u * 3, c)
                        } else {
                            // Inside epsilon circumcircle, do extra tests to make sure the edge is valid.
                            // s-u and t-u cannot overlap with s-pt nor t-pt if they exist.
                            if (doesNotOverlapEdges(pts, edges, s, u) && doesNotOverlapEdges(pts, edges, t, u)) {
                                // Edge is valid.
                                pt = u
                                r = circumcircle(pts, s * 3, t * 3, u * 3, c)
                            }
                        }
                    } // else Outside current circumcircle, skip.
                }
            }
        }

        // Add new triangle or update edge info if s-t is on hull.
        if (pt < npts) {
            // Update face information of edge being completed.
            updateLeftFace(edges, e * 4, s, t, nfaces)

            // Add new edge or update face info of old edge.
            e = findEdge(edges, pt, s)
            if (e == EV_UNDEF) {
                addEdge(edges, maxEdges, pt, s, nfaces, EV_UNDEF)
            } else {
                updateLeftFace(edges, e * 4, pt, s, nfaces)
            }

            // Add new edge or update face info of old edge.
            e = findEdge(edges, t, pt)
            if (e == EV_UNDEF) {
                addEdge(edges, maxEdges, t, pt, nfaces, EV_UNDEF)
            } else {
                updateLeftFace(edges, e * 4, t, pt, nfaces)
            }
            nfaces++
        } else {
            updateLeftFace(edges, e * 4, s, t, EV_HULL)
        }
        return nfaces
    }

    @JvmStatic
    private fun seedArrayWithPolyCenter(
        chf: CompactHeightfield, meshpoly: IntArray, poly: Int, npoly: Int, vertices: IntArray,
        bs: Int, hp: HeightPatch, array: IntArrayList
    ) {
        // Note: Reads to the compact heightfield are offset by border size (bs)
        // since border size offset is already removed from the polymesh vertices.

        // Find cell closest to a poly vertex
        var startCellX = 0
        var startCellY = 0
        var startSpanIndex = -1
        var dmin = RC_UNSET_HEIGHT

        loop@ for (j in 0 until npoly) {
            for (k in 0 until 9) {
                val ax = vertices[meshpoly[poly + j] * 3] + offset[k * 2]
                val ay = vertices[meshpoly[poly + j] * 3 + 1]
                val az = vertices[meshpoly[poly + j] * 3 + 2] + offset[k * 2 + 1]
                if (ax < hp.xmin || ax >= hp.xmin + hp.width || az < hp.ymin || az >= hp.ymin + hp.height) {
                    continue
                }
                val c = ax + bs + (az + bs) * chf.width
                for (i in chf.index[c] until chf.endIndex[c]) {
                    val s = chf.spans[i]
                    val d = abs(ay - s.y)
                    if (d < dmin) {
                        startCellX = ax
                        startCellY = az
                        startSpanIndex = i
                        dmin = d
                        if (dmin <= 0) break@loop
                    }
                }
            }
        }

        // Find center of the polygon
        var pcx = 0
        var pcy = 0
        for (j in 0 until npoly) {
            pcx += vertices[meshpoly[poly + j] * 3]
            pcy += vertices[meshpoly[poly + j] * 3 + 2]
        }
        pcx /= npoly
        pcy /= npoly
        array.clear()
        array.add(startCellX)
        array.add(startCellY)
        array.add(startSpanIndex)
        val dirs = intArrayOf(0, 1, 2, 3)
        hp.data.fill(0, 0, hp.width * hp.height)

        // DFS to move to the center. Note that we need a DFS here and can not just move
        // directly towards the center without recording intermediate nodes, even though the polygons
        // are convex. In very rare we can get stuck due to contour simplification if we do not
        // record nodes.
        var cx = -1
        var cy = -1
        var ci = -1
        while (true) {
            val size = array.size
            if (size < 3) {
                System.err.println("Walk towards polygon center failed to reach center")
                break
            }
            ci = array[size - 1]
            cy = array[size - 2]
            cx = array[size - 3]
            array.size = size - 3

            // Check if close to center of the polygon.
            if (cx == pcx && cy == pcy) {
                break
            }
            // If we are already at the correct X-position, prefer direction
            // directly towards the center in the Y-axis; otherwise prefer
            // direction in the X-axis
            val directDir = if (cx == pcx) {
                getDirForOffset(0, if (pcy > cy) 1 else -1)
            } else {
                getDirForOffset(if (pcx > cx) 1 else -1, 0)
            }

            // Push the direct dir last, so we start with this on next iteration
            var tmp = dirs[3]
            dirs[3] = dirs[directDir]
            dirs[directDir] = tmp
            val cs = chf.spans[ci]
            for (i in 0..3) {
                val dir = dirs[i]
                if (getCon(cs, dir) == RC_NOT_CONNECTED) {
                    continue
                }
                val newX = cx + getDirOffsetX(dir)
                val newY = cy + getDirOffsetY(dir)
                val hpx = newX - hp.xmin
                val hpy = newY - hp.ymin
                if (hpx < 0 || hpx >= hp.width || hpy < 0 || hpy >= hp.height) {
                    continue
                }
                if (hp.data[hpx + hpy * hp.width] != 0) {
                    continue
                }
                hp.data[hpx + hpy * hp.width] = 1
                array.add(newX)
                array.add(newY)
                array.add(chf.index[newX + bs + (newY + bs) * chf.width] + getCon(cs, dir))
            }
            tmp = dirs[3]
            dirs[3] = dirs[directDir]
            dirs[directDir] = tmp
        }
        array.clear()
        // getHeightData seeds are given in coordinates with borders
        array.add(cx + bs)
        array.add(cy + bs)
        array.add(ci)
        hp.data.fill(RC_UNSET_HEIGHT, 0, hp.width * hp.height)
        val cs = chf.spans[ci]
        hp.data[cx - hp.xmin + (cy - hp.ymin) * hp.width] = cs.y
    }

    @JvmStatic
    private fun buildPolyDetail(
        input: FloatArray, nin: Int, sampleDist: Float, sampleMaxError: Float,
        heightSearchRadius: Int, chf: CompactHeightfield, hp: HeightPatch,
        vertices: FloatArray, tris: IntArrayList
    ): Int {
        val samples = IntArrayList(512)
        val edge = FloatArray((MAX_VERTS_PER_EDGE + 1) * 3)
        val hull = IntArray(MAX_VERTS)
        var nhull = 0
        var nverts = nin

        input.copyInto(vertices, 0, 0, nin * 3)

        tris.clear()
        val cs = chf.cellSize
        val ics = 1f / cs

        // Calculate minimum extents of the polygon based on input data.
        val minExtent = polyMinExtent(vertices, nverts)

        // Tessellate outlines.
        // This is done in separate pass to ensure
        // seamless height values across the ply boundaries.
        if (sampleDist > 0) {
            var i = 0
            var j = nin - 1
            while (i < nin) {
                var vj = j * 3
                var vi = i * 3
                var swapped = false
                // Make sure the segments are always handled in same order
                // using lexological sort or else there will be seams.
                if (abs(input[vj] - input[vi]) < 1e-6f) {
                    if (input[vj + 2] > input[vi + 2]) {
                        val temp = vi
                        vi = vj
                        vj = temp
                        swapped = true
                    }
                } else {
                    if (input[vj] > input[vi]) {
                        val temp = vi
                        vi = vj
                        vj = temp
                        swapped = true
                    }
                }
                // Create samples along the edge.
                val dx = input[vi] - input[vj]
                val dy = input[vi + 1] - input[vj + 1]
                val dz = input[vi + 2] - input[vj + 2]
                val d = sqrt(dx * dx + dz * dz)
                var nn = 1 + floor(d / sampleDist).toInt()
                if (nn >= MAX_VERTS_PER_EDGE) {
                    nn = MAX_VERTS_PER_EDGE - 1
                }
                if (nverts + nn >= MAX_VERTS) {
                    nn = MAX_VERTS - 1 - nverts
                }
                for (k in 0..nn) {
                    val u = k.toFloat() / nn.toFloat()
                    val pos = k * 3
                    edge[pos] = input[vj] + dx * u
                    edge[pos + 1] = input[vj + 1] + dy * u
                    edge[pos + 2] = input[vj + 2] + dz * u
                    edge[pos + 1] = getHeight(
                        edge[pos], edge[pos + 1], edge[pos + 2], ics, chf.cellHeight,
                        heightSearchRadius, hp
                    ) * chf.cellHeight
                }
                // Simplify samples.
                val idx = IntArray(MAX_VERTS_PER_EDGE)
                idx[0] = 0
                idx[1] = nn
                var nidx = 2
                var k = 0
                while (k < nidx - 1) {
                    val a = idx[k]
                    val b = idx[k + 1]
                    val va = a * 3
                    val vb = b * 3
                    // Find maximum deviation along the segment.
                    var maxd = 0f
                    var maxi = -1
                    for (m in a + 1 until b) {
                        val dev = distancePtSeg(edge, m * 3, va, vb)
                        if (dev > maxd) {
                            maxd = dev
                            maxi = m
                        }
                    }
                    // If the max deviation is larger than accepted error,
                    // add new point, else continue to next segment.
                    if (maxi != -1 && maxd > sampleMaxError * sampleMaxError) {
                        if (nidx - k >= 0) System.arraycopy(idx, k, idx, k + 1, nidx - k)
                        idx[k + 1] = maxi
                        nidx++
                    } else {
                        ++k
                    }
                }
                hull[nhull++] = j
                // Add new vertices.
                if (swapped) {
                    for (x in nidx - 2 downTo 1) {
                        copy(vertices, nverts * 3, edge, idx[x] * 3)
                        hull[nhull++] = nverts
                        nverts++
                    }
                } else {
                    for (x in 1 until nidx - 1) {
                        copy(vertices, nverts * 3, edge, idx[x] * 3)
                        hull[nhull++] = nverts
                        nverts++
                    }
                }
                j = i++
            }
        }

        // If the polygon minimum extent is small (sliver or small triangle), do not try to add internal points.
        if (minExtent < sampleDist * 2) {
            triangulateHull(vertices, nhull, hull, nin, tris)
            return nverts
        }

        // Tessellate the base mesh.
        // We're using the triangulateHull instead of delaunayHull as it tends to
        // create a bit better triangulation for long thin triangles when there
        // are no internal points.
        triangulateHull(vertices, nhull, hull, nin, tris)
        if (tris.size == 0) {
            // Could not triangulate the poly, make sure there is some valid data there.
            throw RuntimeException("buildPolyDetail: Could not triangulate polygon ($nverts) vertices).")
        }
        if (sampleDist > 0) {
            // Create sample locations in a grid.
            val bmin = Vector3f(input)
            val bmax = Vector3f(input)
            for (i in 1 until nin) {
                min(bmin, input, i * 3)
                max(bmax, input, i * 3)
            }
            val x0 = floor(bmin.x / sampleDist).toInt()
            val x1 = ceil(bmax.x / sampleDist).toInt()
            val z0 = floor(bmin.z / sampleDist).toInt()
            val z1 = ceil(bmax.z / sampleDist).toInt()
            samples.clear()
            for (z in z0 until z1) {
                for (x in x0 until x1) {
                    val pt = Vector3f(
                        x * sampleDist,
                        (bmax.y + bmin.y) * 0.5f,
                        z * sampleDist
                    )
                    // Make sure the samples are not too close to the edges.
                    if (distToPoly(nin, input, pt) > -sampleDist / 2) {
                        continue
                    }
                    samples.add(x)
                    samples.add(getHeight(pt.x, pt.y, pt.z, ics, chf.cellHeight, heightSearchRadius, hp))
                    samples.add(z)
                    samples.add(0) // Not added
                }
            }

            // Add the samples starting from the one that has the most
            // error. The procedure stops when all samples are added
            // or when the max error is within threshold.
            val nsamples = samples.size shr 2
            val bestpt = Vector3f()
            val pt = Vector3f()
            for (iter in 0 until nsamples) {
                if (nverts >= MAX_VERTS) {
                    break
                }

                // Find sample with most error.
                var bestd = 0f
                var besti = -1
                for (i in 0 until nsamples) {
                    val s = i * 4
                    if (samples[s + 3] != 0) {
                        continue  // skip added.
                    }
                    // The sample location is jittered to get rid of some bad triangulations,
                    // which are caused by symmetrical data from the grid structure.
                    pt.set(
                        samples[s] * sampleDist + getJitterX(i) * cs * 0.1f,
                        samples[s + 1] * chf.cellHeight,
                        samples[s + 2] * sampleDist + getJitterY(i) * cs * 0.1f
                    )
                    val d = distToTriMesh(pt, vertices, tris, tris.size shr 2)
                    if (d < 0) {
                        continue  // did not hit the mesh.
                    }
                    if (d > bestd) {
                        bestd = d
                        besti = i
                        bestpt.set(pt)
                    }
                }
                // If the max error is within accepted threshold, stop tessellating.
                if (bestd <= sampleMaxError || besti == -1) {
                    break
                }
                // Mark sample as added.
                samples[besti * 4 + 3] = 1
                // Add the new sample point.
                copy(vertices, nverts * 3, bestpt)
                nverts++

                // Create new triangulation.
                // TO DO: Incremental add instead of full rebuild.
                delaunayHull(nverts, vertices, nhull, hull, tris)
            }
        }
        val ntris = tris.size shr 2
        if (ntris > MAX_TRIS) {
            tris.size = MAX_TRIS * 4
            throw RuntimeException("buildPolyMeshDetail: Shrinking triangle count from $ntris to max $MAX_TRIS")
        }
        return nverts
    }
}