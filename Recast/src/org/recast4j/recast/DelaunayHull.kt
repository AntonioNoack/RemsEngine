package org.recast4j.recast

import me.anno.utils.pooling.JomlPools
import org.joml.Vector3f
import org.recast4j.IntArrayList
import org.recast4j.recast.RecastMeshDetail.EV_HULL
import org.recast4j.recast.RecastMeshDetail.EV_UNDEF
import org.recast4j.recast.RecastMeshDetail.sub
import org.recast4j.recast.RecastMeshDetail.vdot2
import kotlin.math.abs
import kotlin.math.hypot

object DelaunayHull {

    @JvmStatic
    fun delaunayHull(numVertices: Int, vertices: FloatArray, numHulls: Int, hulls: IntArray, triangles: IntArrayList) {
        val maxEdges = numVertices * 10
        val edges = findEdges(numHulls, hulls, maxEdges)
        val numFaces = completeFacets(edges, numVertices, vertices, maxEdges)
        val trisData = createTrianglesFromEdges(triangles, numFaces, edges)
        triangles.size = removeDanglingTriangles(trisData)
    }

    @JvmStatic
    private fun findEdges(numHulls: Int, hulls: IntArray, maxEdges: Int): IntArrayList {
        val edges = IntArrayList(64)
        var j = numHulls - 1
        for (i in 0 until numHulls) {
            addEdge(edges, maxEdges, hulls[j], hulls[i], EV_HULL, EV_UNDEF)
            j = i
        }
        return edges
    }

    @JvmStatic
    private fun completeFacets(edges: IntArrayList, numVertices: Int, vertices: FloatArray, maxEdges: Int): Int {
        var numFaces = 0
        var currentEdge = 0
        while (currentEdge < (edges.size shr 2)) {
            if (edges[currentEdge * 4 + 2] == EV_UNDEF) {
                numFaces = completeFacet(vertices, numVertices, edges, maxEdges, numFaces, currentEdge)
            }
            if (edges[currentEdge * 4 + 3] == EV_UNDEF) {
                numFaces = completeFacet(vertices, numVertices, edges, maxEdges, numFaces, currentEdge)
            }
            currentEdge++
        }
        return numFaces
    }

    @JvmStatic
    private fun createTrianglesFromEdges(triangles: IntArrayList, numFaces: Int, edges: IntArrayList): IntArray {
        triangles.clear()
        triangles.ensureExtra(numFaces * 4)
        triangles.values.fill(-1, 0, numFaces * 4)
        triangles.size = numFaces * 4
        val edgeData = edges.values
        val trisData = triangles.values
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
        return trisData
    }

    @JvmStatic
    private fun removeDanglingTriangles(triangles: IntArray): Int {
        var size = triangles.size
        var t = 0
        while (t < size) {
            if (triangles[t] == -1 || triangles[t + 1] == -1 || triangles[t + 2] == -1) {
                // System.err.println("Dangling! " + trisData[t] + " " + trisData[t + 1] + "  " + trisData[t + 2])
                triangles[t] = triangles[size - 4]
                triangles[t + 1] = triangles[size - 3]
                triangles[t + 2] = triangles[size - 2]
                triangles[t + 3] = triangles[size - 1]
                size -= 4
                t -= 4
            }
            t += 4
        }
        return size
    }

    @JvmStatic
    private fun completeFacet(
        vertices: FloatArray,
        numVertices: Int,
        edges: IntArrayList,
        maxEdges: Int,
        numFaces0: Int,
        edge0: Int
    ): Int {
        var numFaces = numFaces0
        var e = edge0
        val epsilon = 1e-5f
        val edge = e * 4

        val x = edges[edge + 2] == EV_UNDEF
        val y = edges[edge + 3] == EV_UNDEF

        // Edge already completed.
        if (!x && !y) return numFaces

        // Cache s and t.
        val x01 = if (x) 1 else 0
        val s = edges[edge + 1 - x01]
        val t = edges[edge + x01]

        // Find best point on left of edge.
        var pt = numVertices
        val c = Vector3f()
        var r = -1f
        for (u in 0 until numVertices) {
            if (u != s && u != t && cross2d(vertices, s * 3, t * 3, u * 3) > epsilon) {
                if (r < 0) {
                    // The circle is not updated yet, do it now.
                    pt = u
                    r = circumcircle(vertices, s * 3, t * 3, u * 3, c)
                } else {
                    val d = distance2d(c, vertices, u * 3)
                    val tol = 0.001f
                    if (d <= r * (1 + tol)) {
                        if (d < r * (1 - tol)) {
                            // Inside safe circumcircle, update circle.
                            pt = u
                            r = circumcircle(vertices, s * 3, t * 3, u * 3, c)
                        } else {
                            // Inside epsilon circumcircle, do extra tests to make sure the edge is valid.
                            // s-u and t-u cannot overlap with s-pt nor t-pt if they exist.
                            if (doesNotOverlapEdges(vertices, edges, s, u) && doesNotOverlapEdges(vertices, edges, t, u)) {
                                // Edge is valid.
                                pt = u
                                r = circumcircle(vertices, s * 3, t * 3, u * 3, c)
                            }
                        }
                    } // else Outside current circumcircle, skip.
                }
            }
        }

        // Add new triangle or update edge info if s-t is on hull.
        if (pt < numVertices) {
            // Update face information of edge being completed.
            updateLeftFace(edges, e * 4, s, t, numFaces)

            // Add new edge or update face info of old edge.
            e = findEdge(edges, pt, s)
            if (e == EV_UNDEF) {
                addEdge(edges, maxEdges, pt, s, numFaces, EV_UNDEF)
            } else {
                updateLeftFace(edges, e * 4, pt, s, numFaces)
            }

            // Add new edge or update face info of old edge.
            e = findEdge(edges, t, pt)
            if (e == EV_UNDEF) {
                addEdge(edges, maxEdges, t, pt, numFaces, EV_UNDEF)
            } else {
                updateLeftFace(edges, e * 4, t, pt, numFaces)
            }
            numFaces++
        } else {
            updateLeftFace(edges, e * 4, s, t, EV_HULL)
        }
        return numFaces
    }

    @JvmStatic
    private fun findEdge(edges: IntArrayList, s: Int, t: Int): Int {
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
    private fun addEdge(edges: IntArrayList, maxEdges: Int, s: Int, t: Int, l: Int, r: Int) {
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
    private fun circumcircle(vertices: FloatArray, p1: Int, p2: Int, p3: Int, dst: Vector3f): Float {
        val epsilon = 1e-6f
        // Calculate the circle relative to p1, to avoid some precision issues.
        val v1 = JomlPools.vec3f.create()
        val v2 = JomlPools.vec3f.create()
        val v3 = JomlPools.vec3f.create()
        sub(v2, vertices, p2, p1)
        sub(v3, vertices, p3, p1)
        val cp = cross2d(v1, v2, v3)
        val distance = if (abs(cp) > epsilon) {
            val v1Sq = vdot2(v1, v1)
            val v2Sq = vdot2(v2, v2)
            val v3Sq = vdot2(v3, v3)
            val n = 0.5f / cp
            dst.set(
                (v1Sq * (v2.z - v3.z) + v2Sq * (v3.z - v1.z) + v3Sq * (v1.z - v2.z)) * n,
                0f,
                (v1Sq * (v3.x - v2.x) + v2Sq * (v1.x - v3.x) + v3Sq * (v2.x - v1.x)) * n
            )
            val distance = distance2d(dst, v1)
            add(dst, vertices, p1)
            distance
        } else {
            dst.set(vertices, p1)
            0f
        }
        JomlPools.vec3f.sub(3)
        return distance
    }

    @JvmStatic
    private fun updateLeftFace(edges0: IntArrayList, e: Int, s: Int, t: Int, f: Int) {
        if (edges0[e] == s && edges0[e + 1] == t && edges0[e + 2] == EV_UNDEF) {
            edges0[e + 2] = f
        } else if (edges0[e + 1] == s && edges0[e] == t && edges0[e + 3] == EV_UNDEF) {
            edges0[e + 3] = f
        }
    }


    @JvmStatic
    private fun doesNotOverlapEdges(points: FloatArray, edges: IntArrayList, s1: Int, t1: Int): Boolean {
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
    private fun overlapSegSeg2d(vertices: FloatArray, a: Int, b: Int, c: Int, d: Int): Boolean {
        val a1 = cross2d(vertices, a, b, d)
        val a2 = cross2d(vertices, a, b, c)
        if (a1 * a2 < 0f) {
            val a3 = cross2d(vertices, c, d, a)
            val a4 = a3 + a2 - a1
            return a3 * a4 < 0f
        }
        return false
    }

    @JvmStatic
    private fun add(dst: Vector3f, b: FloatArray, bi: Int) {
        dst.add(b[bi], b[bi + 1], b[bi + 2])
    }

    @JvmStatic
    private fun cross2d(vertices: FloatArray, p1: Int, p2: Int, p3: Int): Float {
        val u1 = vertices[p2] - vertices[p1]
        val v1 = vertices[p2 + 2] - vertices[p1 + 2]
        val u2 = vertices[p3] - vertices[p1]
        val v2 = vertices[p3 + 2] - vertices[p1 + 2]
        return u1 * v2 - v1 * u2
    }

    @JvmStatic
    private fun cross2d(p1: Vector3f, p2: Vector3f, p3: Vector3f): Float {
        val u1 = p2.x - p1.x
        val v1 = p2.z - p1.z
        val u2 = p3.x - p1.x
        val v2 = p3.z - p1.z
        return u1 * v2 - v1 * u2
    }

    @JvmStatic
    private fun distance2d(p: Vector3f, vertices: FloatArray, q: Int): Float {
        val dx = vertices[q] - p.x
        val dy = vertices[q + 2] - p.z
        return hypot(dx, dy)
    }

    @JvmStatic
    private fun distance2d(p: Vector3f, q: Vector3f): Float {
        val dx = q.x - p.x
        val dy = q.z - p.z
        return hypot(dx, dy)
    }
}