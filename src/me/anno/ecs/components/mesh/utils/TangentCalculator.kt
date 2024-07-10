package me.anno.ecs.components.mesh.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.gpu.buffer.DrawMode
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

object TangentCalculator {

    @JvmStatic
    fun add(v: FloatArray, i: Int, x: Float, y: Float, z: Float) {
        v[i] += x
        v[i + 1] += y
        v[i + 2] += z
    }

    @JvmStatic
    fun add(v: FloatArray, i: Int, s: Vector3f) {
        v[i] += s.x
        v[i + 1] += s.y
        v[i + 2] += s.z
    }

    @JvmStatic
    private fun computeTangentsIndexed(
        mesh: Mesh,
        positions: FloatArray,
        normals: FloatArray,
        tan1: FloatArray,
        uvs: FloatArray
    ) {

        tan1.fill(0f) // in the future we could keep old values, probably not worth the effort
        val tan2 = FloatArray(tan1.size)

        val n = Vector3f()
        val s = Vector3f()
        val t = Vector3f()

        // https://gamedev.stackexchange.com/questions/68612/how-to-compute-tangent-and-bitangent-vectors
        mesh.forEachTriangleIndex { i0, i1, i2 ->
            addTriangle(positions, i0, i1, i2, uvs, tan1, tan2, s, t)
        }

        // apply all the normals, smooth shading
        var j = 0
        for (i in normals.indices step 3) {
            compute(n, s, t, normals, i, tan1, j, tan2, j)
            j += 4
        }
    }

    private fun addTriangle(
        positions: FloatArray, i0: Int, i1: Int, i2: Int,
        uvs: FloatArray, tan1: FloatArray, tan2: FloatArray,
        resultS: Vector3f, resultT: Vector3f
    ) {

        addTriangle(positions, i0, i1, i2, uvs, resultS, resultT)

        val i04 = i0 shl 2
        val i14 = i1 shl 2
        val i24 = i2 shl 2

        add(tan1, i04, resultS)
        add(tan1, i14, resultS)
        add(tan1, i24, resultS)

        add(tan2, i04, resultT)
        add(tan2, i14, resultT)
        add(tan2, i24, resultT)
    }

    private fun addTriangle(
        positions: FloatArray, i0: Int, i1: Int, i2: Int,
        uvs: FloatArray, resultS: Vector3f, resultT: Vector3f
    ) {

        val i02 = i0 + i0
        val i03 = i0 + i02

        val i12 = i1 + i1
        val i13 = i1 + i12

        val i22 = i2 + i2
        val i23 = i2 + i22

        val x1 = positions[i13] - positions[i03]
        val x2 = positions[i23] - positions[i03]
        val y1 = positions[i13 + 1] - positions[i03 + 1]
        val y2 = positions[i23 + 1] - positions[i03 + 1]
        val z1 = positions[i13 + 2] - positions[i03 + 2]
        val z2 = positions[i23 + 2] - positions[i03 + 2]

        val w0x = uvs[i02]
        val w0y = uvs[i02 + 1]

        val s1 = uvs[i12] - w0x
        val s2 = uvs[i22] - w0x
        val t1 = uvs[i12 + 1] - w0y
        val t2 = uvs[i22 + 1] - w0y

        val area = s1 * t2 - s2 * t1
        if (abs(area) > 1e-16f) {
            val r = 1f / area
            val sx = (t2 * x1 - t1 * x2) * r
            val sy = (t2 * y1 - t1 * y2) * r
            val sz = (t2 * z1 - t1 * z2) * r
            resultS.set(sx, sy, sz)

            val tx = (s1 * x2 - s2 * x1) * r
            val ty = (s1 * y2 - s2 * y1) * r
            val tz = (s1 * z2 - s2 * z1) * r
            resultT.set(tx, ty, tz)
        }
    }

    private fun compute(
        n: Vector3f, s: Vector3f, t: Vector3f,
        normals: FloatArray, i: Int,
        tan1: FloatArray, j: Int,
        tan2: FloatArray, k: Int
    ) {
        n.set(normals, i)
        s.set(tan1, j)
        t.set(tan2[k], tan2[k + 1], tan2[k + 2])
        compute(n, s, t, tan1, j)
    }

    private fun compute(
        n: Vector3f, s: Vector3f, t: Vector3f,
        dst: FloatArray, j: Int
    ) {

        // Gram-Schmidt orthogonalize
        val dot = n.dot(s)
        val sx = s.x - n.x * dot
        val sy = s.y - n.y * dot
        val sz = s.z - n.z * dot
        val r = 1f / sqrt(sx * sx + sy * sy + sz * sz)

        if (r.isFinite()) {

            // theoretically normalize t, however we don't need the amplitude in the handedness, so do it later, when the sign is known

            // n is no longer needed -> we can reuse it here
            val hv = n.cross(sx, sy, sz).dot(t)

            dst[j] = sx * r
            dst[j + 1] = sy * r
            dst[j + 2] = sz * r
            dst[j + 3] = sign(hv)
        }
    }

    @JvmStatic
    private fun computeTangentsNonIndexed(
        positions: FloatArray,
        normals: FloatArray,
        dst: FloatArray,
        uvs: FloatArray
    ) {
        dst.fill(0f) // in the future we could keep old values, probably not worth the effort
        val size = min(positions.size / 9, uvs.size / 6)
        val n = Vector3f()
        val s = Vector3f()
        val t = Vector3f()
        for (k in 0 until size) {

            val i = k * 3
            val i3 = i * 3
            val i4 = i shl 2

            val needsUpdate = !NormalCalculator.isNormalValid(dst, i4) ||
                    !NormalCalculator.isNormalValid(dst, i4 + 3) ||
                    !NormalCalculator.isNormalValid(dst, i4 + 6)

            if (needsUpdate) {
                // calculate
                addTriangle(positions, i, i + 1, i + 2, uvs, s, t)
                n.set(normals, i3)
                compute(n, s, t, dst, i4)
                n.set(normals, i3 + 3)
                compute(n, s, t, dst, i4 + 4)
                n.set(normals, i3 + 6)
                compute(n, s, t, dst, i4 + 8)
            }
        }
    }

    @JvmStatic
    fun checkTangents(
        mesh: Mesh,
        positions: FloatArray,
        normals: FloatArray,
        tangents: FloatArray?,
        uvs: FloatArray?,
    ) {
        // first an allocation free check
        val drawMode = mesh.drawMode
        if (drawMode != DrawMode.TRIANGLES && drawMode != DrawMode.TRIANGLE_STRIP) return
        if (uvs == null || tangents == null) return
        if (NormalCalculator.needsNormalsComputation(tangents, 4)) {
            if (mesh.indices == null) {
                // todo support GL_TRIANGLE_STRIP properly
                computeTangentsNonIndexed(positions, normals, tangents, uvs)
            } else {
                computeTangentsIndexed(mesh, positions, normals, tangents, uvs)
            }
        }
    }
}