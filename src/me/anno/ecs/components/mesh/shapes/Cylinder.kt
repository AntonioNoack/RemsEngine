package me.anno.ecs.components.mesh.shapes

import me.anno.ecs.components.mesh.Mesh
import me.anno.io.files.FileReference
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Booleans.toInt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object Cylinder {

    fun createMesh(
        us: Int = 10,
        vs: Int = 2,
        top: Boolean,
        bottom: Boolean,
        mesh: Mesh = Mesh(),
        // option to use different materials for top, middle and bottom
        topMiddleBottom: List<FileReference>? = null
    ): Mesh {

        val quadCount = us * vs
        val triangleCount = quadCount * 2 + (us - 2) * (top.toInt() + bottom.toInt())
        val indexCount = triangleCount * 3

        val vertexCount = us * vs + us * (top.toInt() + bottom.toInt())

        mesh.positions = mesh.positions.resize(3 * vertexCount)
        mesh.normals = mesh.normals.resize(3 * vertexCount)
        mesh.uvs = mesh.uvs.resize(2 * vertexCount)
        if (topMiddleBottom != null) mesh.materials = topMiddleBottom
        val materialIds = if (topMiddleBottom != null) mesh.materialIds.resize(vertexCount) else null
        mesh.materialIds = materialIds

        // precalculate the angles
        val cu = FloatArray(us)
        val su = FloatArray(us)
        for (i in 0 until us) {
            val angle = (2.0 * PI * i / us).toFloat()
            cu[i] = cos(angle)
            su[i] = sin(angle)
        }

        var k = 0
        var l = 0
        val positions = mesh.positions!!
        val normals = mesh.normals!!
        val uvs = mesh.uvs!!
        for (v in 0 until vs) {
            val y = v * 2f / (vs - 1) - 1f
            for (u in 0 until us) {
                // calculate position
                normals[k] = cu[u]
                positions[k++] = cu[u]
                // normals[k] = 0f
                positions[k++] = y
                normals[k] = su[u]
                positions[k++] = su[u]
                // good like this?
                uvs[l++] = 1f - u.toFloat() / us
                uvs[l++] = v.toFloat() / vs
            }
        }

        val k0 = k / 3
        materialIds?.fill(1, 0, k0)

        if (top) {
            for (u in 0 until us) {
                // calculate position
                normals[k] = 0f
                positions[k++] = cu[u]
                normals[k] = 1f
                positions[k++] = +1f
                normals[k] = 0f
                positions[k++] = su[u]
                // good like this?
                uvs[l++] = 0f
                uvs[l++] = 0f
            }
        }

        val k1 = k / 3
        materialIds?.fill(1, k0, k1)

        if (bottom) {
            for (u in 0 until us) {
                // calculate position
                normals[k] = 0f
                positions[k++] = cu[u]
                normals[k] = -1f
                positions[k++] = -1f
                normals[k] = 0f
                positions[k++] = su[u]
                // good like this?
                uvs[l++] = 0f
                uvs[l++] = 0f
            }
        }

        val k2 = k / 3
        materialIds?.fill(1, k1, k2)

        val indices = IntArray(indexCount)
        mesh.indices = indices

        k = 0
        for (v in 0 until vs) {
            for (u in 0 until us) {
                // create quad face
                val v0 = getIndex(u, v + 1, us, vs)
                val v1 = getIndex(u + 1, v + 1, us, vs)
                val v2 = getIndex(u + 1, v, us, vs)
                val v3 = getIndex(u, v, us, vs)
                indices[k++] = v0
                indices[k++] = v1
                indices[k++] = v2
                indices[k++] = v0
                indices[k++] = v2
                indices[k++] = v3
            }
        }

        var v0 = us * vs
        if (top) {
            // add ring
            // 0 1 2 0 2 3 0 3 4
            for (u in 2 until us) {
                indices[k++] = v0
                indices[k++] = v0 + u
                indices[k++] = v0 + u - 1
            }
            v0 += us
        }

        if (bottom) {
            for (u in 2 until us) {
                indices[k++] = v0
                indices[k++] = v0 + u - 1
                indices[k++] = v0 + u
            }
        }

        return mesh
    }

    private fun getIndex(u: Int, v: Int, us: Int, vs: Int): Int {
        val u2 = if (u >= us) 0 else u
        val v2 = if (v >= vs) 0 else v
        return u2 + v2 * us
    }

}