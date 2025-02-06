/*
recast4j copyright (c) 2021 Piotr Piastucki piotr@jtilia.org

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
package org.recast4j.detour

import org.joml.Vector3f
import org.recast4j.FloatSubArray
import org.recast4j.ReStack
import org.recast4j.Vectors
import kotlin.math.abs

/**
 * Convex-convex intersection based on "Computational Geometry in C" by Joseph O'Rourke
 */
object ConvexConvexIntersection {

    private const val EPSILON = 0.0001f

    fun intersect(
        p: FloatSubArray, q: FloatSubArray,
        tmp: FloatSubArray // FloatArray(max(p.size, q.size) * 3)
    ): FloatSubArray? {
        val n = p.size / 3
        val m = q.size / 3
        var ii = 0
        /* Initialize variables. */
        val a = ReStack.vec3fs.create()
        val b = ReStack.vec3fs.create()
        val a1 = ReStack.vec3fs.create()
        val b1 = ReStack.vec3fs.create()
        var aa = 0
        var ba = 0
        var ai = 0
        var bi = 0
        var f = InFlag.Unknown
        var isFirstPoint = true
        val ip = ReStack.vec3fs.create()
        val iq = ReStack.vec3fs.create()
        val A = ReStack.vec3fs.create()
        val B = ReStack.vec3fs.create()
        do {
            a.set(p.data, 3 * (ai % n))
            b.set(q.data, 3 * (bi % m))
            a1.set(p.data, 3 * ((ai + n - 1) % n)) // prev a
            b1.set(q.data, 3 * ((bi + m - 1) % m)) // prev b
            a.sub(a1, A)
            b.sub(b1, B)
            var cross = B.x * A.z - A.x * B.z // triArea2D({0, 0}, A, B);
            val aHB = Vectors.triArea2D(b1, b, a)
            val bHA = Vectors.triArea2D(a1, a, b)
            if (abs(cross) < EPSILON) cross = 0f
            val parallel = cross == 0f
            val code =
                if (parallel) parallelInt(a1, a, b1, b, ip, iq)
                else segSegInt(a1, a, b1, b, ip)
            if (code == Intersection.Single) {
                if (isFirstPoint) {
                    isFirstPoint = false
                    ba = 0
                    aa = 0
                }
                ii = addVertex(tmp, ii, ip)
                f = inOut(f, aHB, bHA)
            }

            /*-----Advance rules-----*/

            /* Special case: A & B overlap and oppositely oriented. */
            if (code == Intersection.Overlap &&
                Vectors.dot2D(A, B) < 0
            ) {
                ii = addVertex(tmp, ii, ip)
                ii = addVertex(tmp, ii, iq)
                break
            }

            /* Special case: A & B parallel and separated. */
            if (parallel && aHB < 0f && bHA < 0f) {
                ReStack.vec3fs.sub(8)
                return null
            } else if (parallel && abs(aHB) < EPSILON && abs(bHA) < EPSILON) {
                /* Advance but do not output point. */
                if (f == InFlag.Pin) {
                    ba++
                    bi++
                } else {
                    aa++
                    ai++
                }
            } else if (cross >= 0) {
                if (bHA > 0) {
                    if (f == InFlag.Pin) {
                        ii = addVertex(tmp, ii, a)
                    }
                    aa++
                    ai++
                } else {
                    if (f == InFlag.Qin) {
                        ii = addVertex(tmp, ii, b)
                    }
                    ba++
                    bi++
                }
            } else {
                if (aHB > 0) {
                    if (f == InFlag.Qin) {
                        ii = addVertex(tmp, ii, b)
                    }
                    ba++
                    bi++
                } else {
                    if (f == InFlag.Pin) {
                        ii = addVertex(tmp, ii, a)
                    }
                    aa++
                    ai++
                }
            }
            /* Quit when both adv. indices have cycled, or one has cycled twice. */
        } while ((aa < n || ba < m) && aa < 2 * n && ba < 2 * m)

        ReStack.vec3fs.sub(8)
        /* Deal with special cases: not implemented. */
        return if (f == InFlag.Unknown) null
        else {
            tmp.size = ii
            tmp
        }
    }

    private fun addVertex(inters0: FloatSubArray, ii: Int, p: Vector3f): Int {
        val inters = inters0.data
        if (ii > 0) {
            if (inters[ii - 3] == p.x && inters[ii - 2] == p.y && inters[ii - 1] == p.z) return ii
            if (inters[0] == p.x && inters[1] == p.y && inters[2] == p.z) return ii
        }
        inters[ii] = p.x
        inters[ii + 1] = p.y
        inters[ii + 2] = p.z
        return ii + 3
    }

    private fun inOut(inflag: InFlag, aHB: Float, bHA: Float): InFlag {
        if (aHB > 0) {
            return InFlag.Pin
        } else if (bHA > 0) {
            return InFlag.Qin
        }
        return inflag
    }

    private fun segSegInt(a: Vector3f, b: Vector3f, c: Vector3f, d: Vector3f, p: Vector3f): Intersection {
        val i = Vectors.intersectSegSeg2D(a, b, c, d)
        if (i != null) {
            val s = i.first
            val t = i.second
            if (s in 0f..1f && t >= 0f && t <= 1f) {
                p.set(a).lerp(b, s)
                return Intersection.Single
            }
        }
        return Intersection.None
    }

    private fun parallelInt(
        a: Vector3f, b: Vector3f, c: Vector3f, d: Vector3f,
        p: Vector3f, q: Vector3f
    ): Intersection {
        if (between(a, b, c) && between(a, b, d)) {
            p.set(c)
            q.set(d)
            return Intersection.Overlap
        }
        if (between(c, d, a) && between(c, d, b)) {
            p.set(a)
            q.set(b)
            return Intersection.Overlap
        }
        if (between(a, b, c) && between(c, d, b)) {
            p.set(c)
            q.set(b)
            return Intersection.Overlap
        }
        if (between(a, b, c) && between(c, d, a)) {
            p.set(c)
            q.set(a)
            return Intersection.Overlap
        }
        if (between(a, b, d) && between(c, d, b)) {
            p.set(d)
            q.set(b)
            return Intersection.Overlap
        }
        if (between(a, b, d) && between(c, d, a)) {
            p.set(d)
            q.set(a)
            return Intersection.Overlap
        }
        return Intersection.None
    }

    private fun between(a: Vector3f, b: Vector3f, c: Vector3f): Boolean {
        return if (abs(a.x - b.x) > abs(a.z - b.z)) {
            a.x <= c.x && c.x <= b.x || a.x >= c.x && c.x >= b.x
        } else {
            a.z <= c.z && c.z <= b.z || a.z >= c.z && c.z >= b.z
        }
    }

    private enum class InFlag {
        Pin, Qin, Unknown
    }

    private enum class Intersection {
        None, Single, Overlap
    }
}