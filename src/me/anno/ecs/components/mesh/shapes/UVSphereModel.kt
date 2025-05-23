package me.anno.ecs.components.mesh.shapes

import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.Maths
import me.anno.maths.Maths.PIf
import me.anno.utils.types.Arrays.resize
import kotlin.math.cos
import kotlin.math.sin

object UVSphereModel {

    val sphereMesh = createUVSphere(30, 30)

    /**
     * creates a UV-sphere with <us> vertical and <vs> horizontal sections
     * */
    fun createUVSphere(us: Int, vs: Int, mesh: Mesh = Mesh()): Mesh {

        val triangleCount = 2 * us * (vs - 1)
        val indexCount = triangleCount * 3

        val vertexCount = (us + 1) * (vs + 1)

        val positions = mesh.positions.resize(3 * vertexCount)
        mesh.positions = positions
        val uvs = mesh.uvs.resize(2 * vertexCount)
        mesh.uvs = uvs

        // precalculate the angles? mmh...
        val cu = FloatArray(us + 1)
        val su = FloatArray(us + 1)
        for (i in 0..us) {
            val angle = Maths.TAUf * i / us
            cu[i] = cos(angle)
            su[i] = sin(angle)
        }

        val cv = FloatArray(vs + 1)
        val sv = FloatArray(vs + 1)
        for (i in 0..vs) {// -pi/2 .. +pi/2
            val angle = (PIf * (i.toFloat() / vs - 0.5f))
            cv[i] = cos(angle)
            sv[i] = sin(angle)
        }

        var k = 0
        var l = 0
        for (v in 0..vs) {
            for (u in 0..us) {
                // calculate position
                positions[k++] = cv[v] * cu[u]
                positions[k++] = sv[v]
                positions[k++] = cv[v] * su[u]
                // good like this?
                uvs[l++] = 1f - u.toFloat() / us // mirrored as well...
                uvs[l++] = v.toFloat() / vs // mirrored
            }
        }

        // ok? to just copy the values
        mesh.normals = mesh.positions
        val indices = mesh.indices.resize(indexCount)
        mesh.indices = indices

        k = 0
        val ms = us + 1
        for (u in 0 until us) {
            // create triangle on north-pole
            indices[k++] = u + ms
            indices[k++] = u + 1 + ms
            indices[k++] = u + 1
        }
        for (v in 1 until vs - 1) {
            val w = v + 1
            for (u in 0 until us) {
                // create quad face
                val v0 = u + w * ms
                val v1 = u + 1 + w * ms
                val v2 = u + 1 + v * ms
                val v3 = u + v * ms
                indices[k++] = v0
                indices[k++] = v1
                indices[k++] = v2
                indices[k++] = v0
                indices[k++] = v2
                indices[k++] = v3
            }
        }
        val di = (vs - 1) * ms
        for (u in 0 until us) {
            // create triangle on south-pole
            indices[k++] = u + ms + di
            indices[k++] = u + 1 + di
            indices[k++] = u + di
        }
        return mesh
    }
}