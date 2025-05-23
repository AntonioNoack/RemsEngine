package me.anno.ecs.components.mesh.shapes

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.shapes.MaxAreaCircle.createCircleIndices
import me.anno.ecs.components.mesh.shapes.MaxAreaCircle.numCircleIndices
import me.anno.io.files.FileReference
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.mix
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Booleans.toInt
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generates cylinder meshes with optionally different materials for top, sides and bottom.
 * */
object CylinderModel {

    fun createCylinder(
        us: Int, vs: Int,
        top: Boolean, bottom: Boolean,
        // option to use different materials for top, middle and bottom
        middleTopBottom: List<FileReference>?, uScale: Float, mesh: Mesh,
    ): Mesh = createCylinder(us, vs, top, bottom, middleTopBottom, uScale, mesh, -1f, +1f, 1f)

    fun createCylinder(
        us: Int, vs: Int, top: Boolean, bottom: Boolean,
        // option to use different materials for top, middle and bottom
        middleTopBottom: List<FileReference>?, uScale: Float, mesh: Mesh,
        y0: Float, y1: Float, radius: Float,
    ): Mesh {

        assertTrue(us >= 3)
        assertTrue(vs >= 1)

        val quadCount = us * (vs - 1)
        val triangleCount = quadCount * 2 + (us - 2) * (top.toInt() + bottom.toInt())
        val indexCount = triangleCount * 3

        val vertexCount = (us + 1) * vs + top.toInt(us) + bottom.toInt(us)

        mesh.positions = mesh.positions.resize(3 * vertexCount)
        mesh.normals = mesh.normals.resize(3 * vertexCount)
        mesh.uvs = mesh.uvs.resize(2 * vertexCount)
        if (middleTopBottom != null) mesh.materials = middleTopBottom
        val materialIds = if (middleTopBottom != null) mesh.materialIds.resize(triangleCount) else null
        mesh.materialIds = materialIds

        // precalculate the angles
        val cu = FloatArray(us + 1)
        val su = FloatArray(us + 1)
        for (i in 0..us) {
            val angle = i * TAUf / us
            cu[i] = cos(angle)
            su[i] = sin(angle)
        }

        var k = 0
        var l = 0
        val positions = mesh.positions!!
        val normals = mesh.normals!!
        val uvs = mesh.uvs!!
        for (v in 0 until vs) {
            val y = mix(y0, y1, v / (vs - 1f))
            for (u in 0..us) {
                // calculate position
                normals[k] = cu[u]
                positions[k++] = radius * cu[u]
                positions[k++] = y
                normals[k] = su[u]
                positions[k++] = radius * su[u]
                uvs[l++] = uScale * (1f - u.toFloat() / us)
                uvs[l++] = v.toFloat() / (vs - 1f)
            }
        }

        if (top) {
            for (u in 0 until us) {
                // calculate position
                positions[k++] = radius * cu[u]
                normals[k] = 1f
                positions[k++] = y1
                positions[k++] = radius * su[u]
                uvs[l++] = .5f + cu[u] * .5f
                uvs[l++] = .5f - su[u] * .5f
            }
        }

        if (bottom) {
            for (u in 0 until us) {
                // calculate position
                positions[k++] = radius * cu[u]
                normals[k] = -1f
                positions[k++] = y0
                positions[k++] = radius * su[u]
                uvs[l++] = .5f + cu[u] * .5f
                uvs[l++] = .5f + su[u] * .5f
            }
        }

        val indices = mesh.indices.resize(indexCount)
        mesh.indices = indices

        k = 0
        for (v in 0 until vs - 1) {
            for (u in 0 until us) {
                // create quad face
                val v0 = getIndex(u, v + 1, us)
                val v1 = getIndex(u + 1, v + 1, us)
                val v2 = getIndex(u + 1, v, us)
                val v3 = getIndex(u, v, us)
                indices[k++] = v0
                indices[k++] = v1
                indices[k++] = v2
                indices[k++] = v0
                indices[k++] = v2
                indices[k++] = v3
            }
        }

        val k0 = k / 3
        materialIds?.fill(0, 0, k0)

        var v0 = (us + 1) * vs
        if (top) {
            // add ring
            // 0 1 2 0 2 3 0 3 4
            createCircleIndices(us, v0, indices, k, false)
            k += numCircleIndices(us)
            v0 += us
        }

        val k1 = k / 3
        materialIds?.fill(1, k0, k1)

        if (bottom) {
            createCircleIndices(us, v0, indices, k, true)
            k += numCircleIndices(us)
            // v0 += us // unused
        }

        val k2 = k / 3
        materialIds?.fill(2, k1, k2)

        return mesh
    }

    private fun getIndex(u: Int, v: Int, us: Int): Int {
        return u + v * (us + 1)
    }
}