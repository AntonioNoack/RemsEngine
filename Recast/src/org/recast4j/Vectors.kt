/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
recast4j Copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package org.recast4j

import org.joml.Vector3f
import org.joml.Vector3i
import org.recast4j.detour.BVNode
import kotlin.math.abs
import kotlin.math.sqrt

object Vectors {

    fun clamp(x: Float, min: Float, max: Float): Float {
        return if (x < min) min else if (x < max) x else max
    }

    fun clamp(x: Int, min: Int, max: Int): Int {
        return if (x < min) min else if (x < max) x else max
    }

    fun min(a: Vector3f, b: FloatArray, i: Int) {
        a.min(b[i], b[i + 1], b[i + 2])
    }

    fun max(a: Vector3f, b: FloatArray, i: Int) {
        a.max(b[i], b[i + 1], b[i + 2])
    }

    fun copy(dst: FloatArray, input: FloatArray, i: Int) {
        copy(dst, 0, input, i)
    }

    fun copy(dst: FloatArray, src: FloatArray) {
        copy(dst, 0, src, 0)
    }

    fun copy(dst: FloatArray, dstI: Int, src: FloatArray, srcI: Int) {
        dst[dstI] = src[srcI]
        dst[dstI + 1] = src[srcI + 1]
        dst[dstI + 2] = src[srcI + 2]
    }

    fun add(dst: FloatArray, a: FloatArray, vertices: FloatArray, i: Int) {
        dst[0] = a[0] + vertices[i]
        dst[1] = a[1] + vertices[i + 1]
        dst[2] = a[2] + vertices[i + 2]
    }

    fun sub(dst: Vector3f, vertices: FloatArray, i: Int, j: Int) {
        dst.x = vertices[i] - vertices[j]
        dst.y = vertices[i + 1] - vertices[j + 1]
        dst.z = vertices[i + 2] - vertices[j + 2]
    }

    fun normalize(v: FloatArray) {
        val invLength = 1f / sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        v[0] *= invLength
        v[1] *= invLength
        v[2] *= invLength
    }

    fun dot(v1: FloatArray, v2: FloatArray): Float {
        return v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2]
    }

    var EPS = 1e-4f

    /**
     * a + b * s
     */
    fun mad(a: Vector3f, b: Vector3f, f: Float, dst: Vector3f) {
        dst.set(b).mul(f).add(a)
    }

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun lerp(vertices: FloatArray, v1: Int, v2: Int, t: Float, dst: Vector3f) {
        dst.set(
            lerp(vertices[v1], vertices[v2], t),
            lerp(vertices[v1 + 1], vertices[v2 + 1], t),
            lerp(vertices[v1 + 2], vertices[v2 + 2], t),
        )
    }

    fun lerp(vertices: FloatArray, v1: Int, v2: Int, t: Float, dst: FloatArray, dstI: Int) {
        dst[dstI] = lerp(vertices[v1], vertices[v2], t)
        dst[dstI + 1] = lerp(vertices[v1 + 1], vertices[v2 + 1], t)
        dst[dstI + 2] = lerp(vertices[v1 + 2], vertices[v2 + 2], t)
    }

    fun copy(dst: Vector3f, src: IntArray, srcI: Int) {
        dst.set(
            src[srcI].toFloat(),
            src[srcI + 1].toFloat(),
            src[srcI + 2].toFloat()
        )
    }

    fun sq(a: Float): Float {
        return a * a
    }

    /**
     * Derives the distance between the specified points on the xz-plane.
     */
    fun dist2D(v1: Vector3f, v2: Vector3f): Float {
        val dx = v2.x - v1.x
        val dz = v2.z - v1.z
        return sqrt((dx * dx + dz * dz))
    }

    fun dist2DSqr(v1: Vector3f, v2: Vector3f): Float {
        val dx = v2.x - v1.x
        val dz = v2.z - v1.z
        return dx * dx + dz * dz
    }

    fun dist2DSqr(p: Vector3f, vertices: FloatArray, i: Int): Float {
        val dx = vertices[i] - p.x
        val dz = vertices[i + 2] - p.z
        return dx * dx + dz * dz
    }

    private val EQUAL_THRESHOLD = sq(1f / 16384f)

    /**
     * Performs a 'sloppy' co-location check of the specified points.
     *
     * @return True if the points are considered to be at the same location.
     */
    fun vEqual(p0: Vector3f, p1: Vector3f): Boolean {
        return p0.distanceSquared(p1) < EQUAL_THRESHOLD
    }

    /**
     * Derives the dot product of two vectors on the xz-plane.
     * */
    fun dot2D(u: Vector3f, v: Vector3f): Float {
        return u.x * v.x + u.z * v.z
    }

    /**
     * Derives the dot product of two vectors on the xz-plane.
     * */
    fun dot2D(u: Vector3f, v: FloatArray, vi: Int): Float {
        return u.x * v[vi] + u.z * v[vi + 2]
    }

    /**
     * Derives the signed xz-plane area of the triangle ABC, or the relationship of line AB to point C.
     * */
    fun triArea2D(vertices: FloatArray, a: Int, b: Int, c: Int): Float {
        val abx = vertices[b] - vertices[a]
        val abz = vertices[b + 2] - vertices[a + 2]
        val acx = vertices[c] - vertices[a]
        val acz = vertices[c + 2] - vertices[a + 2]
        return acx * abz - abx * acz
    }

    fun triArea2D(a: Vector3f, b: Vector3f, c: Vector3f): Float {
        val abx = b.x - a.x
        val abz = b.z - a.z
        val acx = c.x - a.x
        val acz = c.z - a.z
        return acx * abz - abx * acz
    }

    /**
     * Determines if two axis-aligned bounding boxes overlap.
     */
    fun overlapQuantBounds(amin: Vector3i, amax: Vector3i, n: BVNode): Boolean {
        return amin.x <= n.maxX && amax.x >= n.minX && amin.y <= n.maxY && amax.y >= n.minY && amin.z <= n.maxZ && amax.z >= n.minZ
    }

    /**
     * Determines if two axis-aligned bounding boxes overlap.
     */
    fun overlapBounds(amin: Vector3f, amax: Vector3f, bmin: Vector3f, bmax: Vector3f): Boolean {
        return amin.x <= bmax.x && amax.x >= bmin.x &&
                amin.y <= bmax.y && amax.y >= bmin.y &&
                amin.z <= bmax.z && amax.z >= bmin.z
    }

    fun distancePtSegSqr2D(pt: Vector3f, p: Vector3f, q: Vector3f): FloatPair {
        val pqx = q.x - p.x
        val pqz = q.z - p.z
        var dx = pt.x - p.x
        var dz = pt.z - p.z
        val d = pqx * pqx + pqz * pqz
        var t = pqx * dx + pqz * dz
        if (d > 0) {
            t /= d
        }
        if (t < 0) {
            t = 0f
        } else if (t > 1) {
            t = 1f
        }
        dx = p.x + t * pqx - pt.x
        dz = p.z + t * pqz - pt.z
        return FloatPair(dx * dx + dz * dz, t)
    }

    fun distancePtSegSqr2DFirst(pt: Vector3f, p: Vector3f, q: Vector3f): Float {
        val pqx = q.x - p.x
        val pqz = q.z - p.z
        var dx = pt.x - p.x
        var dz = pt.z - p.z
        val d = pqx * pqx + pqz * pqz
        var t = pqx * dx + pqz * dz
        if (d > 0) {
            t /= d
        }
        if (t < 0) {
            t = 0f
        } else if (t > 1) {
            t = 1f
        }
        dx = p.x + t * pqx - pt.x
        dz = p.z + t * pqz - pt.z
        return dx * dx + dz * dz
    }

    fun closestHeightPointTriangle(p: Vector3f, a: Vector3f, b: Vector3f, c: Vector3f): Float {
        val ax = a.x
        val az = a.z

        val v0x = c.x - ax
        val v0z = c.z - az

        val v1x = b.x - ax
        val v1z = b.z - az

        val v2x = p.x - ax
        val v2z = p.z - az

        // Compute scaled barycentric coordinates
        var denom = v0x * v1z - v0z * v1x
        if (abs(denom) < EPS) {
            return Float.NaN
        }

        var u = v1z * v2x - v1x * v2z
        var v = v0x * v2z - v0z * v2x
        if (denom < 0) {
            denom = -denom
            u = -u
            v = -v
        }

        // If point lies inside the triangle, return interpolated y-coord.
        return if (u >= 0f && v >= 0f && u + v <= denom) {
            a.y + ((c.y - a.y) * u + (b.y - a.y) * v) / denom
        } else Float.NaN
    }

    /**
     * All points are projected onto the xz-plane, so the y-values are ignored.
     * */
    fun pointInPolygon(pt: Vector3f, vertices: FloatArray, numVertices: Int): Boolean {
        var c = false
        var i = 0
        var j = numVertices - 1
        while (i < numVertices) {
            val vi = i * 3
            val vj = j * 3
            if (vertices[vi + 2] > pt.z != vertices[vj + 2] > pt.z && (pt.x < (vertices[vj] - vertices[vi])
                        * (pt.z - vertices[vi + 2]) / (vertices[vj + 2] - vertices[vi + 2]) + vertices[vi])
            ) c = !c
            j = i++
        }
        return c
    }

    fun distancePtPolyEdgesSqr(
        pt: Vector3f,
        vertices: FloatArray,
        numVertices: Int,
        ed: FloatArray,
        et: FloatArray
    ): Boolean {
        var c = false
        var i = 0
        var j = numVertices - 1
        while (i < numVertices) {
            val vi = i * 3
            val vj = j * 3
            if (vertices[vi + 2] > pt.z != vertices[vj + 2] > pt.z && (pt.x < (vertices[vj] - vertices[vi])
                        * (pt.z - vertices[vi + 2]) / (vertices[vj + 2] - vertices[vi + 2]) + vertices[vi])
            ) {
                c = !c
            }
            val (first, second) = distancePtSegSqr2D(pt, vertices, vj, vi)
            ed[j] = first
            et[j] = second
            j = i++
        }
        return c
    }

    fun overlapRange(amin: Float, amax: Float, bmin: Float, bmax: Float, eps: Float): Boolean {
        return amin + eps <= bmax && amax - eps >= bmin
    }

    fun overlapRange(amin: Float, amax: Float, bmin: Float, bmax: Float): Boolean {
        return amin <= bmax && amax >= bmin
    }

    fun overlapRange(amin: Int, amax: Int, bmin: Int, bmax: Int): Boolean {
        return amin <= bmax && amax >= bmin
    }

    var eps = 1e-4f

    /**
     * All vertices are projected onto the xz-plane, so the y-values are ignored.
     */
    fun overlapPolyPoly2D(polya: FloatArray, npolya: Int, polyb: FloatArray, npolyb: Int, tmp: Vector3f): Boolean {
        var i = 0
        var j = npolya - 1
        while (i < npolya) {
            val va = j * 3
            val vb = i * 3
            overlapRangeSetN(tmp, polya, va, vb)
            if (overlapRangeX(tmp, polya, polyb, npolya, npolyb)) return false
            j = i++
        }
        i = 0
        j = npolyb - 1
        while (i < npolyb) {
            val va = j * 3
            val vb = i * 3
            overlapRangeSetN(tmp, polyb, va, vb)
            if (overlapRangeX(tmp, polya, polyb, npolya, npolyb)) return false
            j = i++
        }
        return true
    }

    private fun overlapRangeSetN(n: Vector3f, polya: FloatArray, va: Int, vb: Int) {
        n.set(polya[vb + 2] - polya[va + 2], 0f, polya[va] - polya[vb])
    }

    private fun overlapRangeX(n: Vector3f, polya: FloatArray, polyb: FloatArray, npolya: Int, npolyb: Int): Boolean {

        var amax = dot2D(n, polya, 0)
        var amin = amax
        for (i in 1 until npolya) {
            val d = dot2D(n, polya, i * 3)
            amin = kotlin.math.min(amin, d)
            amax = kotlin.math.max(amax, d)
        }

        var bmax = dot2D(n, polyb, 0)
        var bmin = bmax
        for (i in 1 until npolyb) {
            val d = dot2D(n, polyb, i * 3)
            bmin = kotlin.math.min(bmin, d)
            bmax = kotlin.math.max(bmax, d)
        }

        return !overlapRange(amin, amax, bmin, bmax, eps)
    }

    // Returns a random point in a convex polygon.
    // Adapted from Graphics Gems article.
    fun randomPointInConvexPoly(pts: FloatArray, npts: Int, areas: FloatArray, s: Float, t: Float): Vector3f {
        // Calc triangle areas
        var areasum = 0f
        for (i in 2 until npts) {
            areas[i] = triArea2D(pts, 0, (i - 1) * 3, i * 3)
            areasum += kotlin.math.max(0.001f, areas[i])
        }
        // Find sub triangle weighted by area.
        val thr = s * areasum
        var acc = 0f
        var u = 1f
        var tri = npts - 1
        for (i in 2 until npts) {
            val dacc = areas[i]
            if (thr >= acc && thr < acc + dacc) {
                u = (thr - acc) / dacc
                tri = i
                break
            }
            acc += dacc
        }
        val v = sqrt(t)
        val a = 1 - v
        val b = (1 - u) * v
        val c = u * v
        val pa = 0
        val pb = (tri - 1) * 3
        val pc = tri * 3
        return Vector3f(
            a * pts[pa] + b * pts[pb] + c * pts[pc],
            a * pts[pa + 1] + b * pts[pb + 1] + c * pts[pc + 1],
            a * pts[pa + 2] + b * pts[pb + 2] + c * pts[pc + 2]
        )
    }

    fun nextPow2(n: Int): Int {
        var v = n
        v--
        v = v or (v shr 1)
        v = v or (v shr 2)
        v = v or (v shr 4)
        v = v or (v shr 8)
        v = v or (v shr 16)
        v++
        return v
    }

    fun ilog2(n: Int): Int {
        var v = n
        var r: Int = (if (v > 0xffff) 1 else 0) shl 4
        v = v shr r
        var shift: Int = (if (v > 0xff) 1 else 0) shl 3
        v = v shr shift
        r = r or shift
        shift = (if (v > 0xf) 1 else 0) shl 2
        v = v shr shift
        r = r or shift
        shift = (if (v > 0x3) 1 else 0) shl 1
        v = v shr shift
        r = r or shift
        r = r or (v shr 1)
        return r
    }

    fun intersectSegmentPoly2D(p0: Vector3f, p1: Vector3f, vertices: FloatArray, nvertices: Int): IntersectResult {
        val result = IntersectResult()
        val EPS = 0.00000001f
        val dirX = p1.x - p0.x
        val dirZ = p1.z - p0.z
        var i = 0
        var j = nvertices - 1
        while (i < nvertices) {
            val v = j * 3
            val w = i * 3
            // edge
            val ex = vertices[w] - vertices[v]
            val ez = vertices[w + 2] - vertices[v + 2]
            // dir
            val dx = p0.x - vertices[v]
            val dz = p0.z - vertices[v + 2]
            val normal = ez * dx - ex * dz
            val dist = dirZ * ex - dirX * ez
            if (abs(dist) < EPS) {
                // S is nearly parallel to this edge
                if (normal < 0) {
                    return result
                } else {
                    j = i++
                    continue
                }
            }
            val t = normal / dist
            if (dist < 0) {
                // segment S is entering across this edge
                if (t > result.tmin) {
                    result.tmin = t
                    result.segMin = j
                    // S enters after leaving polygon
                    if (result.tmin > result.tmax) {
                        return result
                    }
                }
            } else {
                // segment S is leaving across this edge
                if (t < result.tmax) {
                    result.tmax = t
                    result.segMax = j
                    // S leaves before entering polygon
                    if (result.tmax < result.tmin) {
                        return result
                    }
                }
            }
            j = i++
        }
        result.intersects = true
        return result
    }

    fun distancePtSegSqr2D(ptx: Float, ptz: Float, data: FloatArray, pi: Int, qi: Int): FloatPair {
        val px = data[pi]
        val pz = data[pi + 2]
        val pqx = data[qi] - px
        val pqz = data[qi + 2] - pz
        var dx = ptx - px
        var dz = ptz - pz
        val d = pqx * pqx + pqz * pqz
        var t = pqx * dx + pqz * dz
        if (d > 0) {
            t /= d
        }
        if (t < 0) {
            t = 0f
        } else if (t > 1) {
            t = 1f
        }
        dx = px + t * pqx - ptx
        dz = pz + t * pqz - ptz
        return FloatPair(dx * dx + dz * dz, t)
    }

    fun distancePtSegSqr2D(pt: Vector3f, vertices: FloatArray, p: Int, q: Int): FloatPair {
        return distancePtSegSqr2D(pt.x, pt.z, vertices, p, q)
    }

    fun distancePtSegSqr2DFirst(ptx: Float, ptz: Float, data: FloatArray, pi: Int, qi: Int): Float {
        val px = data[pi]
        val pz = data[pi + 2]
        val pqx = data[qi] - px
        val pqz = data[qi + 2] - pz
        var dx = ptx - px
        var dz = ptz - pz
        val d = pqx * pqx + pqz * pqz
        var t = pqx * dx + pqz * dz
        if (d > 0) {
            t /= d
        }
        if (t < 0) {
            t = 0f
        } else if (t > 1) {
            t = 1f
        }
        dx = px + t * pqx - ptx
        dz = pz + t * pqz - ptz
        return dx * dx + dz * dz
    }

    fun distancePtSegSqr2DFirst(pt: Vector3f, vertices: FloatArray, p: Int, q: Int): Float {
        return distancePtSegSqr2DFirst(pt.x, pt.z, vertices, p, q)
    }

    fun distancePtSegSqr2DSecond(ptx: Float, ptz: Float, data: FloatArray, pi: Int, qi: Int): Float {
        val px = data[pi]
        val pz = data[pi + 2]
        val pqx = data[qi] - px
        val pqz = data[qi + 2] - pz
        val dx = ptx - px
        val dz = ptz - pz
        val d = pqx * pqx + pqz * pqz
        var t = pqx * dx + pqz * dz
        if (d > 0) {
            t /= d
        }
        return kotlin.math.min(kotlin.math.max(t, 0f), 1f)
    }

    fun distancePtSegSqr2DSecond(pt: Vector3f, vertices: FloatArray, p: Int, q: Int): Float {
        return distancePtSegSqr2DSecond(pt.x, pt.z, vertices, p, q)
    }

    fun oppositeTile(side: Int): Int {
        return side + 4 and 0x7
    }

    fun intersectSegSeg2D(a0: Vector3f, a1: Vector3f, b0: Vector3f, b1: Vector3f): FloatPair? {
        val ux = a1.x - a0.x
        val uz = a1.z - a0.z
        val vx = b1.x - b0.x
        val vz = b1.z - b0.z
        val wx = a0.x - b0.x
        val wz = a0.z - b0.z
        val d = ux * vz - uz * vx
        if (abs(d) < 1e-6f) return null
        val s = (vx * wz - vz * wx) / d
        val t = (ux * wz - uz * wx) / d
        return FloatPair(s, t)
    }

    class IntersectResult {
        var intersects = false
        var tmin = 0f
        var tmax = 1f
        var segMin = -1
        var segMax = -1
    }
}