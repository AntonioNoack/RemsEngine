package me.sp4cerat.fqms

import me.anno.maths.Maths.min
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Triangles
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Original: https://github.com/sp4cerat/Fast-Quadric-Mesh-Simplification
 * Good source for improvements: https://github.com/Whinarn/UnityMeshSimplifier
 * */
class FastQuadraticMeshSimplification {

    val vertices = ArrayList<Vertex>()
    val triangles = ArrayList<Triangle>()
    private val refs = IntArrayList()

    // todo option to preserve corners...
    var preserveBorder = false

    /**
     * @param targetCount target number of triangles
     * @param aggressiveness sharpness to increase threshold; 5..8 are good. More iterations yield higher quality.
     * */
    fun simplifyMesh(targetCount: Int, aggressiveness: Int = 7) {
        for (i in triangles.indices) {
            triangles[i].deleted = false
        }

        val tmp = Vector3d()
        val deletedTriangles = IntArray(1)
        val deleted0 = ArrayList<Boolean>()
        val deleted1 = ArrayList<Boolean>()
        val deleteTarget = triangles.size - targetCount
        for (iteration in 0 until 100) {
            if (deletedTriangles[0] >= deleteTarget) break

            // update mesh once in a while
            if (iteration % 5 == 0) {
                updateMesh(iteration)
            }

            // clear dirty flag
            clearDirtyFlags()

            // remove vertices & mark deleted triangles
            val threshold = 1e-9 * (iteration + 3.0).pow(aggressiveness)
            for (i in triangles.indices) {
                val t = triangles[i]
                if (t.errors[3] > threshold || t.deleted || t.dirty) continue

                for (j in 0 until 3) {
                    if (!(t.errors[j] < threshold)) continue

                    val i0 = t.vertexIds[j]
                    val i1 = t.vertexIds[if (j == 2) 0 else j + 1]
                    val v0 = vertices[i0]
                    val v1 = vertices[i1]

                    // border check
                    if (v0.border != v1.border) {
                        continue
                    }

                    if (preserveBorder && (v0.border or v1.border)) {
                        continue
                    }

                    // Compute vertex to collapse to
                    val p = tmp
                    calculateError(v0, v1, p)
                    deleted0.ensureSize(v0.numTriangles)
                    deleted1.ensureSize(v1.numTriangles)

                    // don't remove if flipped
                    if (flipped(p, i1, v0, deleted0)) continue
                    if (flipped(p, i0, v1, deleted1)) continue

                    if (t.attrFlags.hasFlag(Attributes.TEXCOORD.flag)) {
                        updateUVs(v0, p, deleted0)
                        updateUVs(v1, p, deleted1)
                    }

                    // not flipped, so remove edge
                    v0.position.set(p)
                    v0.normalMatrix += v1.normalMatrix

                    val tstart = refs.size
                    updateTriangles(i0, v0, tmp, deleted0, deletedTriangles)
                    updateTriangles(i0, v1, tmp, deleted1, deletedTriangles)

                    val tcount = refs.size - tstart
                    if (tcount <= v0.numTriangles) {
                        // save ram (???)
                        for (i in 0 until tcount) {
                            refs[v0.firstRefIndex + i] = refs[tstart + i]
                        }
                    } else {
                        // append
                        v0.firstRefIndex = tstart
                    }

                    v0.numTriangles = tcount
                    break
                }

                // done?
                if (deletedTriangles[0] >= deleteTarget) {
                    break
                }
            }
        }
        compactMesh()
    }

    private fun clearDirtyFlags() {
        for (i in triangles.indices) {
            triangles[i].dirty = false
        }
    }

    private fun ArrayList<Boolean>.ensureSize(newSize: Int) {
        clear()
        while (size < newSize) add(false)
    }

    private fun updateUVs(v: Vertex, p: Vector3d, deleted: ArrayList<Boolean>) {
        for (k in 0 until v.numTriangles) {
            val r = refs[v.firstRefIndex + k]
            val t = triangles[r.triangleId()]
            if (t.deleted || deleted[k]) continue

            val p1 = vertices[t.vertexIds[0]].position
            val p2 = vertices[t.vertexIds[1]].position
            val p3 = vertices[t.vertexIds[2]].position
            interpolate(p, p1, p2, p3, t.uvs, t.uvs[r.vertex0To3()])
        }
    }

    private fun barycentric(p: Vector3d, a: Vector3d, b: Vector3d, c: Vector3d, dst: Vector3d): Vector3d {
        val v0 = b - a
        val v1 = c - a
        val v2 = p - a
        val d00 = v0.dot(v0)
        val d01 = v0.dot(v1)
        val d11 = v1.dot(v1)
        val d20 = v2.dot(v0)
        val d21 = v2.dot(v1)
        val denominator = d00 * d11 - d01 * d01
        val v = (d11 * d20 - d01 * d21) / denominator
        val w = (d00 * d21 - d01 * d20) / denominator
        val u = 1.0 - v - w
        return dst.set(u, v, w)
    }

    private fun interpolate(
        p: Vector3d, a: Vector3d, b: Vector3d, c: Vector3d,
        attrs: Array<Vector3d>, dst: Vector3d
    ): Vector3d {
        val (bx, by, bz) = barycentric(p, a, b, c, dst)
        dst.set(0.0)
        attrs[0].mulAdd(bx, dst, dst)
        attrs[1].mulAdd(by, dst, dst)
        attrs[2].mulAdd(bz, dst, dst)
        return dst
    }

    /**
     * Update triangle connections and edge error after a edge is collapsed
     * */
    private fun updateTriangles(
        i0: Int, v: Vertex, tmp: Vector3d,
        deleted: ArrayList<Boolean>, numDeletedTriangles: IntArray
    ) {
        for (k in 0 until v.numTriangles) {
            val r = refs[v.firstRefIndex + k]
            val t = triangles[r.triangleId()]
            if (t.deleted) continue
            if (deleted[k]) {
                t.deleted = true
                numDeletedTriangles[0]++
                continue
            }
            t.vertexIds[r.vertex0To3()] = i0
            t.dirty = true
            calculateTriangleError(t, tmp)
            refs.add(r)
        }
    }

    private fun updateMesh(iteration: Int) {
        if (iteration == 0) {
            // initialization
            calculateErrors()
            updateReferences()
            identifyBoundary()
        } else {
            // updating triangles
            compactTriangles()
            updateReferences()
        }
    }

    private fun compactTriangles() {
        var dst = 0
        for (i in triangles.indices) {
            val t = triangles[i]
            if (!t.deleted) {
                triangles[dst++] = t
            }
        }
        if (dst < triangles.size) {
            triangles.subList(dst, triangles.size).clear()
        }
    }

    private fun calculateErrors() {
        // Init Quadrics by Plane & Edge Errors;
        // required at the beginning ( iteration == 0 )
        // recomputing during the simplification is not required,
        // but mostly improves the result for closed meshes
        for (i in vertices.indices) {
            vertices[i].normalMatrix.m.fill(0.0)
        }

        val added = SymmetricMatrix()
        for (i in triangles.indices) {
            val t = triangles[i]
            val vertexIds = t.vertexIds
            val p0 = vertices[vertexIds[0]]
            val p1 = vertices[vertexIds[1]]
            val p2 = vertices[vertexIds[2]]
            val n = t.normal
            Triangles.subCross(p0.position, p1.position, p2.position, n)
            val lenSq = n.lengthSquared()
            if (lenSq > 1e-300) {
                n.mul(1.0 / sqrt(lenSq))
                added.set(n.x, n.y, n.z, -n.dot(p0.position))
                p0.normalMatrix += added
                p1.normalMatrix += added
                p2.normalMatrix += added
            }
        }

        // calculate edge error
        val tmp = Vector3d()
        for (i in triangles.indices) {
            val t = triangles[i]
            calculateTriangleError(t, tmp)
        }
    }

    private fun calculateTriangleError(t: Triangle, tmp: Vector3d) {
        val errors = t.errors
        val vertexIds = t.vertexIds
        val v0 = vertices[vertexIds[0]]
        val v1 = vertices[vertexIds[1]]
        val v2 = vertices[vertexIds[2]]
        errors[0] = calculateError(v0, v1, tmp)
        errors[1] = calculateError(v1, v2, tmp)
        errors[2] = calculateError(v2, v0, tmp)
        errors[3] = min(errors[0], min(errors[1], errors[2]))
    }

    private fun identifyBoundary() {
        val vcount = IntArrayList()
        val vids = IntArrayList()
        for (i in vertices.indices) {
            vertices[i].border = false
        }
        for (i in vertices.indices) {
            val v = vertices[i]
            vcount.clear()
            vids.clear()
            for (j in 0 until v.numTriangles) {
                val k = refs[v.firstRefIndex + j].triangleId()
                val t = triangles[k]
                for (k in 0 until 3) {
                    var ofs = 0
                    val id = t.vertexIds[k]
                    while (ofs < vcount.size) {
                        if (vids[ofs] == id) break
                        ofs++
                    }
                    if (ofs == vcount.size) {
                        vcount.add(1)
                        vids.add(id)
                    } else {
                        vcount[ofs]++
                    }
                }
            }
            for (j in vcount.indices) {
                if (vcount[j] == 1) {
                    vertices[vids[j]].border = true
                }
            }
        }
    }

    private fun updateReferences() {

        // init reference ID list
        for (i in vertices.indices) {
            val v = vertices[i]
            v.firstRefIndex = 0
            v.numTriangles = 0
        }

        for (i in triangles.indices) {
            val vertexIds = triangles[i].vertexIds
            vertices[vertexIds[0]].numTriangles++
            vertices[vertexIds[1]].numTriangles++
            vertices[vertexIds[2]].numTriangles++
        }

        var tstart = 0
        for (i in vertices.indices) {
            val v = vertices[i]
            v.firstRefIndex = tstart
            tstart += v.numTriangles
            v.numTriangles = 0
        }

        // write references
        val numRefs = triangles.size * 3
        while (refs.size < numRefs) refs.add(0)
        refs.size = numRefs

        for (i in triangles.indices) {
            val vertexIds = triangles[i].vertexIds
            for (j in 0 until 3) {
                val v = vertices[vertexIds[j]]
                refs[v.firstRefIndex + v.numTriangles] = ref(i, j)
                v.numTriangles++
            }
        }
    }

    /**
     * Finally compact mesh before exiting
     * */
    private fun compactMesh() {
        for (i in vertices.indices) {
            vertices[i].numTriangles = 0
        }
        triangles.removeIf { it.deleted }
        for (i in triangles.indices) {
            val vertexIds = triangles[i].vertexIds
            vertices[vertexIds[0]].numTriangles = 1
            vertices[vertexIds[1]].numTriangles = 1
            vertices[vertexIds[2]].numTriangles = 1
        }
        var dst = 0
        for (i in vertices.indices) {
            val v = vertices[i]
            if (v.numTriangles == 0) { // not referenced -> gets deleted
                continue
            }
            v.firstRefIndex = dst // store new index into firstRefIndex
            val w = vertices[dst]
            w.position.set(v.position)
            w.border = v.border
            dst++
        }
        // renumber vertex indices in triangles
        for (i in triangles.indices) {
            val vertexIds = triangles[i].vertexIds
            vertexIds[0] = vertices[vertexIds[0]].firstRefIndex
            vertexIds[1] = vertices[vertexIds[1]].firstRefIndex
            vertexIds[2] = vertices[vertexIds[2]].firstRefIndex
        }
        vertices.subList(dst, vertices.size).clear()
    }

    private val tmp0 = Vector3d()
    private val tmp1 = Vector3d()

    private fun Int.vertex0To3(): Int = this and 3
    private fun Int.triangleId(): Int = this ushr 2
    private fun ref(triangleId: Int, vertex0To3: Int): Int {
        assertTrue(vertex0To3 in 0 until 3)
        assertTrue(triangleId >= 0 && triangleId < 1 shl 30)
        return triangleId.shl(2) + vertex0To3
    }

    /**
     * Check if a triangle flips when this edge is removed
     * */
    private fun flipped(p: Vector3d, i1: Int, v0: Vertex, deleted: ArrayList<Boolean>): Boolean {
        for (k in 0 until v0.numTriangles) {
            val t = triangles[refs[v0.firstRefIndex + k].triangleId()]
            if (t.deleted) continue

            val s = refs[v0.firstRefIndex + k].vertex0To3()
            val id1 = t.vertexIds[(s + 1) % 3]
            val id2 = t.vertexIds[(s + 2) % 3]
            if (id1 == i1 || id2 == i1) {
                deleted[k] = true
                continue
            }

            val d1 = (vertices[id1].position.sub(p, tmp0)).safeNormalize()
            val d2 = (vertices[id2].position.sub(p, tmp1)).safeNormalize()
            if (abs(d1.dot(d2)) > 0.999) return true // vertices are parallel

            val n = d1.cross(d2).safeNormalize()
            deleted[k] = false
            if (n.dot(t.normal) < 0.2) return true
        }
        return false
    }

    /**
     * Error between vertex and Quadric
     * */
    private fun vertexError(q: SymmetricMatrix, x: Double, y: Double, z: Double): Double {
        val q = q.m
        return (q[0] * x * x + 2 * q[1] * x * y + 2 * q[2] * x * z + 2 * q[3] * x +
                q[4] * y * y + 2 * q[5] * y * z + 2 * q[6] * y +
                q[7] * z * z + 2 * q[8] * z + q[9])
    }

    private val tmpSum = SymmetricMatrix()

    /**
     * Error for one edge
     * */
    private fun calculateError(va: Vertex, vb: Vertex, dst: Vector3d): Double {
        val q = va.normalMatrix.add(vb.normalMatrix, tmpSum)
        val border = va.border and vb.border
        val det = q.det(0, 1, 2, 1, 4, 5, 2, 5, 7)
        if (abs(det) > 1e-38 && !border) {
            val invDet = 1.0 / det
            // q_delta is invertible
            dst.x = -invDet * q.det(1, 2, 3, 4, 5, 6, 5, 7, 8) // vx = A41/det(q_delta)
            dst.y = +invDet * q.det(0, 2, 3, 1, 5, 6, 2, 7, 8) // vy = A42/det(q_delta)
            dst.z = -invDet * q.det(0, 1, 3, 1, 4, 6, 2, 5, 8) // vz = A43/det(q_delta)
            return vertexError(q, dst.x, dst.y, dst.z)
        } else {
            // det = 0 -> try to find best result
            val p1 = va.position
            val p2 = vb.position
            val p3x = (p1.x + p2.x) * 0.5
            val p3y = (p1.y + p2.y) * 0.5
            val p3z = (p1.z + p2.z) * 0.5
            val error1 = vertexError(q, p1.x, p1.y, p1.z)
            val error2 = vertexError(q, p2.x, p2.y, p2.z)
            val error3 = vertexError(q, p3x, p3y, p3z)
            val minError = min(error1, min(error2, error3))
            if (error1 == minError) dst.set(p1)
            if (error2 == minError) dst.set(p2)
            if (error3 == minError) dst.set(p3x, p3y, p3z)
            return minError
        }
    }
}

