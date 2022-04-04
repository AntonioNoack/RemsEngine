package me.anno.ecs.components.mesh.shapes

import me.anno.ecs.components.mesh.Mesh
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object Icosahedron {

    fun createMesh(us: Int = 10, vs: Int = 10, mesh: Mesh = Mesh()): Mesh {

        val faceCount = us * vs * 2
        val triangleCount = faceCount * 2
        val indexCount = triangleCount * 3

        val vertexCount = us * (vs + 1)

        mesh.positions = FloatArray(3 * vertexCount)
        mesh.uvs = FloatArray(2 * vertexCount)

        // precalculate the angles? mmh...
        val cu = FloatArray(us)
        val su = FloatArray(us)
        for (i in 0 until us) {
            val angle = (2.0 * PI * i / us).toFloat()
            cu[i] = cos(angle)
            su[i] = sin(angle)
        }

        val cv = FloatArray(vs + 1)
        val sv = FloatArray(vs + 1)
        for (i in 0..vs) {// -pi/2 .. +pi/2
            val angle = (PI * (i.toDouble() / vs - 0.5)).toFloat()
            cv[i] = cos(angle)
            sv[i] = sin(angle)
        }

        var k = 0
        var l = 0
        val positions = mesh.positions!!
        val uvs = mesh.uvs!!
        for (v in 0..vs) {
            for (u in 0 until us) {
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
        val indices = IntArray(indexCount)
        mesh.indices = indices

        k = 0
        for (v in 0 until vs) {
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

        return mesh
    }

    fun getIndex(u: Int, v: Int, us: Int): Int {
        val u2 = if (u >= us) 0 else u
        return u2 + v * us
    }

}