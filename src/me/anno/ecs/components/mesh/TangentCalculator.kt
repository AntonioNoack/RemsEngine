package me.anno.ecs.components.mesh

import me.anno.utils.pooling.JomlPools
import org.lwjgl.opengl.GL11C.GL_TRIANGLES
import kotlin.math.abs
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
            val i04 = i02 + i02

            val i12 = i1 + i1
            val i13 = i1 + i12
            val i14 = i12 + i12

            val i22 = i2 + i2
            val i23 = i2 + i22
            val i24 = i22 + i22

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
                add(tan1, i04, sx, sy, sz)
                add(tan1, i14, sx, sy, sz)
                add(tan1, i24, sx, sy, sz)

                val tx = (s1 * x2 - s2 * x1) * r
                val ty = (s1 * y2 - s2 * y1) * r
                val tz = (s1 * z2 - s2 * z1) * r
                add(tan2, i04, tx, ty, tz)
                add(tan2, i14, tx, ty, tz)
                add(tan2, i24, tx, ty, tz)

            }
        }

        val n = JomlPools.vec3f.create()
        val t = JomlPools.vec3f.create()

        // apply all the normals, smooth shading
        var j = 0
        for (i in normals.indices step 3) {

            n.set(normals[i], normals[i + 1], normals[i + 2])
            t.set(tan1[j], tan1[j + 1], tan1[j + 2])

            // Gram-Schmidt orthogonalize
            val dot = n.dot(t)
            val tx = t.x - n.x * dot
            val ty = t.y - n.y * dot
            val tz = t.z - n.z * dot
            val r = 1f / sqrt(tx * tx + ty * ty + tz * tz)
            if (r.isFinite()) {

                // theoretically normalize t, however we don't need the amplitude in the handedness, so do it later, when the sign is known

                // n is no longer needed -> we can reuse it here
                val hv = n.cross(tx, ty, tz).dot(tan2[j], tan2[j + 1], tan2[j + 2])

                tan1[j++] = tx * r
                tan1[j++] = ty * r
                tan1[j++] = tz * r
                tan1[j++] = sign(hv)

            } else j += 4// else there was no data -> don't calculate NaNs
        }
        JomlPools.vec3f.sub(2)
    }

    @JvmStatic
    private fun computeTangentsNonIndexed(
        positions: FloatArray,
        normals: FloatArray,
        tan1: FloatArray,
        uvs: FloatArray
    ) {

        tan1.fill(0f) // in the future we could keep old values, probably not worth the effort
        val tan2 = FloatArray(tan1.size)
        for (i0 in 0 until (positions.size - 8) / 3 step 3) {

            // https://gamedev.stackexchange.com/questions/68612/how-to-compute-tangent-and-bitangent-vectors

            val i1 = i0 + 1
            val i2 = i0 + 2

            val i02 = i0 + i0
            val i03 = i0 + i02
            val i04 = i02 + i02

            val i12 = i1 + i1
            val i13 = i1 + i12
            val i14 = i12 + i12

            val i22 = i2 + i2
            val i23 = i2 + i22
            val i24 = i22 + i22

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

            val area = s1 * t2 - s2 * t1
            if (abs(area) > 1e-16f) {

                val r = 1f / area

                val sx = (t2 * x1 - t1 * x2) * r
                val sy = (t2 * y1 - t1 * y2) * r
                val sz = (t2 * z1 - t1 * z2) * r
                add(tan1, i04, sx, sy, sz)
                add(tan1, i14, sx, sy, sz)
                add(tan1, i24, sx, sy, sz)

                val tx = (s1 * x2 - s2 * x1) * r
                val ty = (s1 * y2 - s2 * y1) * r
                val tz = (s1 * z2 - s2 * z1) * r
                add(tan2, i04, tx, ty, tz)
                add(tan2, i14, tx, ty, tz)
                add(tan2, i24, tx, ty, tz)

            }

        }

        val n = JomlPools.vec3f.create()
        val t = JomlPools.vec3f.create()

        // apply all the normals, smooth shading
        var j = 0
        for (i in normals.indices step 3) {

            n.set(normals[i], normals[i + 1], normals[i + 2])
            t.set(tan1[j], tan1[j + 1], tan1[j + 2])

            // Gram-Schmidt orthogonalize
            val dot = n.dot(t)
            val tx = t.x - n.x * dot
            val ty = t.y - n.y * dot
            val tz = t.z - n.z * dot
            val area2 = tx * tx + ty * ty + tz * tz
            if (area2 > 0f) {

                val r = 1f / sqrt(area2)

                // theoretically normalize t, however we don't need the amplitude in the handedness, so do it later, when the sign is known

                // n is no longer needed -> we can destroy it here
                val hv = n.cross(tx, ty, tz)
                    .dot(tan2[j], tan2[j + 1], tan2[j + 2])

                tan1[j++] = tx * r
                tan1[j++] = ty * r
                tan1[j++] = tz * r
                tan1[j++] = sign(hv)

            } else j += 4// else there was no data -> don't calculate NaNs
        }

        JomlPools.vec3f.sub(2)
    }

    @JvmStatic
    fun checkTangents(
        positions: FloatArray,
        normals: FloatArray,
        tangents: FloatArray?,
        uvs: FloatArray?,
        indices: IntArray?,
        drawMode: Int
    ) {
        // todo support GL_TRIANGLE_STRIP
        // first an allocation free check
        if (drawMode != GL_TRIANGLES) return
        if (uvs == null || tangents == null) return
        if (NormalCalculator.needsNormalsComputation(tangents, 4)) {
            if (indices == null) {
                computeTangentsNonIndexed(positions, normals, tangents, uvs)
            } else {
                computeTangentsIndexed(positions, normals, tangents, uvs, indices)
            }
        }
    }

}