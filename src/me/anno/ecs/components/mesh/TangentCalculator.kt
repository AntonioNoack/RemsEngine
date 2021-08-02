package me.anno.ecs.components.mesh

import org.joml.Vector3f
import kotlin.math.sqrt

object TangentCalculator {

    private fun calculateFlatTangent(
        positions: FloatArray,
        normals: FloatArray,
        uvs: FloatArray,
        i0: Int, i1: Int, i2: Int,
        a: Vector3f, b: Vector3f, c: Vector3f
    ): Vector3f {

        val nx = normals[i0]
        val ny = normals[i1]
        val nz = normals[i2]

        // todo gram-schmidt ortho-normalization
        a.set(positions[i0], positions[i0 + 1], positions[i0 + 2])
        b.set(positions[i1], positions[i1 + 1], positions[i1 + 2])
        c.set(positions[i2], positions[i2 + 1], positions[i2 + 2])
        b.sub(a)
        c.sub(a)
        return b.cross(c).normalize()
    }

    fun add(v: FloatArray, i: Int, x: Float, y: Float, z: Float) {
        v[i] += x
        v[i + 1] += y
        v[i + 2] += z
    }

    private fun computeTangentsIndexed(
        positions: FloatArray,
        normals: FloatArray,
        tan1: FloatArray,
        uvs: FloatArray,
        indices: IntArray
    ) {

        tan1.fill(0f) // in the future we could keep old values, probably not worth the effort
        val tan2 = FloatArray(tan1.size)
        for (i in indices.indices step 3) {

            // https://gamedev.stackexchange.com/questions/68612/how-to-compute-tangent-and-bitangent-vectors

            val i0 = indices[i + 0]
            val i1 = indices[i + 1]
            val i2 = indices[i + 2]

            val i02 = i0 + i0
            val i03 = i0 + i02

            val i12 = i1 + i1
            val i13 = i1 + i12

            val i22 = i2 + i2
            val i23 = i2 + i22

            val v0x = positions[i03]
            val v0y = positions[i03 + 1]
            val v0z = positions[i03 + 2]

            val v1x = positions[i13]
            val v1y = positions[i13 + 1]
            val v1z = positions[i13 + 2]

            val v2x = positions[i23]
            val v2y = positions[i23 + 1]
            val v2z = positions[i23 + 2]

            val x1 = v1x - v0x
            val x2 = v2x - v0x
            val y1 = v1y - v0y
            val y2 = v2y - v0y
            val z1 = v1z - v0z
            val z2 = v2z - v0z

            val w0x = uvs[i02]
            val w0y = uvs[i02 + 1]

            val s1 = uvs[i12] - w0x
            val s2 = uvs[i22] - w0x
            val t1 = uvs[i12 + 1] - w0y
            val t2 = uvs[i22 + 1] - w0y

            val r = 1f / (s1 * t2 - s2 * t1)

            if (r.isFinite()) {

                val sx = (t2 * x1 - t1 * x2) * r
                val sy = (t2 * y1 - t1 * y2) * r
                val sz = (t2 * z1 - t1 * z2) * r
                add(tan1, i03, sx, sy, sz)
                add(tan1, i13, sx, sy, sz)
                add(tan1, i23, sx, sy, sz)

                val tx = (s1 * x2 - s2 * x1) * r
                val ty = (s1 * y2 - s2 * y1) * r
                val tz = (s1 * z2 - s2 * z1) * r
                add(tan2, i03, tx, ty, tz)
                add(tan2, i13, tx, ty, tz)
                add(tan2, i23, tx, ty, tz)

            }

        }

        val n = Vector3f()
        val t = Vector3f()

        // apply all the normals, smooth shading
        for (i in positions.indices step 3) {

            n.set(normals[i], normals[i + 1], normals[i + 2])
            t.set(tan1[i], tan1[i + 1], tan1[i + 2])

            // Gram-Schmidt orthogonalize
            val dot = n.dot(t)
            val tx = t.x - n.x * dot
            val ty = t.y - n.y * dot
            val tz = t.z - n.z * dot
            var r = 1f / sqrt(tx * tx + ty * ty + tz * tz)
            if (r.isFinite()) {

                // theoretically normalize t, however we don't need the amplitude in the handedness, so do it later, when the sign is known

                // n is no longer needed -> we can destroy it here
                val hv = n.cross(tx, ty, tz).dot(tan2[i], tan2[i + 1], tan2[i + 2])
                val handedness = if (hv < 0f) -1f else +1f
                r *= handedness

                tan1[i + 0] = tx * r
                tan1[i + 1] = ty * r
                tan1[i + 2] = tz * r

            } // else there was no data -> don't calculate NaNs

        }

    }

    private fun computeTangentsNonIndexed(
        positions: FloatArray,
        normals: FloatArray,
        tangents: FloatArray,
        uvs: FloatArray
    ) {
        // could be optimized, we don't really need to create the indices, and we would also not need the
        // tan2 float array
        computeTangentsIndexed(positions, normals, tangents, uvs, IntArray(positions.size / 3) { it })
    }

    fun checkTangents(
        positions: FloatArray,
        normals: FloatArray,
        tangents: FloatArray?,
        uvs: FloatArray?,
        indices: IntArray?
    ) {
        // first an allocation free check
        uvs ?: return
        tangents ?: return
        if (!NormalCalculator.needsNormalsComputation(tangents)) return
        if (indices == null) {
            computeTangentsNonIndexed(positions, normals, tangents, uvs)
        } else {
            computeTangentsIndexed(positions, normals, tangents, uvs, indices)
        }
    }


}