package me.anno.ecs.components.mesh.shapes

import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.Maths.PHIf
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.utils.types.Arrays.resize
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object Icosahedron {

    // not sorted, because those numbers are from a Blender file
    val positions = floatArrayOf(
        -1f, -PHIf, 0f,
        +1f, -PHIf, 0f,
        +0f, -1f, PHIf,
        -PHIf, 0f, +1f,
        -PHIf, 0f, -1f,
        0f, -1f, -PHIf,
        PHIf, 0f, +1f,
        0f, +1f, +PHIf,
        -1f, +PHIf, 0f,
        +0f, +1f, -PHIf,
        +PHIf, 0f, -1f,
        +1f, +PHIf, 0f,
    )

    val indices = byteArrayOf(
        0, 1, 2, 1, 0, 5, 0, 2, 3,
        0, 3, 4, 0, 4, 5, 1, 5, 10,
        2, 1, 6, 3, 2, 7, 4, 3, 8,
        5, 4, 9, 1, 10, 6, 2, 6, 7,
        3, 7, 8, 4, 8, 9, 5, 9, 10,
        6, 10, 11, 7, 6, 11, 8, 7, 11,
        9, 8, 11, 10, 9, 11
    )

    init {
        // normalize all positions
        for (i in positions.indices step 3) {
            val x = positions[i]
            val y = positions[i + 1]
            val z = positions[i + 2]
            val f = 1f / length(x, y, z)
            positions[i] = x * f
            positions[i + 1] = y * f
            positions[i + 2] = z * f
        }
    }

    /**
     * creates a subdivided icosahedron like Blenders "icosphere"
     * */
    fun createIcosphere(subDivisions: Int, mesh: Mesh = Mesh()): Mesh {

        // create base shape, and then subdivide the triangles
        val numVertices = indices.size shl (subDivisions * 2)
        val numCoords = numVertices * 3
        val positions = mesh.positions.resize(numCoords)
        val uvs = mesh.uvs.resize(numVertices * 2)
        var k2 = 0
        var k3 = 0

        fun u(x: Float, z: Float, x2: Float, z2: Float): Float {
            if (x * x + z * z <= 1e-7f) return atan2(x2, z2) / PIf
            return atan2(z, x) / PIf
        }

        // todo equators are weird
        fun v(x: Float, y: Float, z: Float): Float {
            return 1f - atan2(length(x, z), y) / PIf
        }

        fun add(
            s: Int,
            x0: Float, y0: Float, z0: Float, u0: Float, v0: Float,
            x1: Float, y1: Float, z1: Float, u1: Float, v1: Float,
            x2: Float, y2: Float, z2: Float, u2: Float, v2: Float,
        ) {
            if (s < subDivisions) {
                var x01 = (x0 + x1)
                var y01 = (y0 + y1)
                var z01 = (z0 + z1)
                val u01 = u(x01, z01, x2, z2)
                val v01 = v(x01, y01, z01)
                val a = 1f / length(x01, y01, z01)
                x01 *= a
                y01 *= a
                z01 *= a
                var x12 = (x1 + x2)
                var y12 = (y1 + y2)
                var z12 = (z1 + z2)
                val u12 = u(x12, z12, x0, z0)
                val v12 = v(x12, y12, z12)
                val b = 1f / length(x12, y12, z12)
                x12 *= b
                y12 *= b
                z12 *= b
                var x20 = (x2 + x0)
                var y20 = (y2 + y0)
                var z20 = (z2 + z0)
                val u20 = u(x20, z20, x1, z1)
                val v20 = v(x20, y20, z20)
                val c = 1f / length(x20, y20, z20)
                x20 *= c
                y20 *= c
                z20 *= c
                val t = s + 1
                add(
                    t, x0, y0, z0, u0, v0,
                    x01, y01, z01, u01, v01,
                    x20, y20, z20, u20, v20,
                )
                add(
                    t, x01, y01, z01, u01, v01,
                    x12, y12, z12, u12, v12,
                    x20, y20, z20, u20, v20,
                )
                add(
                    t, x1, y1, z1, u1, v1,
                    x12, y12, z12, u12, v12,
                    x01, y01, z01, u01, v01,
                )
                add(
                    t, x2, y2, z2, u2, v2,
                    x20, y20, z20, u20, v20,
                    x12, y12, z12, u12, v12,
                )
            } else {
                var u0x = u0
                var u1x = u1
                var u2x = u2
                // more than PI difference? fix it
                if (max(u0x, max(u1x, u2x)) > min(u0x, min(u1x, u2x)) + 1f) {
                    if (u0x > 0f) u0x -= 2f
                    if (u1x > 0f) u1x -= 2f
                    if (u2x > 0f) u2x -= 2f
                }
                // add triangle
                positions[k3++] = x0
                positions[k3++] = y0
                positions[k3++] = z0
                uvs[k2++] = u0x
                uvs[k2++] = v0
                positions[k3++] = x1
                positions[k3++] = y1
                positions[k3++] = z1
                uvs[k2++] = u1x
                uvs[k2++] = v1
                positions[k3++] = x2
                positions[k3++] = y2
                positions[k3++] = z2
                uvs[k2++] = u2x
                uvs[k2++] = v2
            }
        }

        val pos = this.positions
        for (i in indices.indices step 3) {
            val a = indices[i] * 3
            val b = indices[i + 1] * 3
            val c = indices[i + 2] * 3
            val x0 = pos[a]
            val y0 = pos[a + 1]
            val z0 = pos[a + 2]
            val x1 = pos[b]
            val y1 = pos[b + 1]
            val z1 = pos[b + 2]
            val x2 = pos[c]
            val y2 = pos[c + 1]
            val z2 = pos[c + 2]
            add(
                0,
                x0, y0, z0, u(x0, z0, (x1 + x2), (z1 + z2)), v(x0, y0, z0),
                x1, y1, z1, u(x1, z1, (x0 + x2), (z0 + z2)), v(x1, y1, z1),
                x2, y2, z2, u(x2, z2, (x0 + x1), (z0 + z1)), v(x2, y2, z2)
            )
        }
        mesh.positions = positions
        mesh.normals = positions
        mesh.uvs = uvs
        mesh.indices = null
        return mesh
    }

    /**
     * creates a UV-sphere
     * */
    fun createUVSphere(us: Int = 10, vs: Int = 10, mesh: Mesh = Mesh()): Mesh {

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

    private fun getIndex(u: Int, v: Int, us: Int): Int {
        val u2 = if (u >= us) 0 else u
        return u2 + v * us
    }

}