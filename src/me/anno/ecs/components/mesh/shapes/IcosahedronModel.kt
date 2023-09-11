package me.anno.ecs.components.mesh.shapes

import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.Maths.PHI
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.utils.types.Arrays.resize
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object IcosahedronModel {

    val positions = doubleArrayOf(
        -PHI, 0.0, -1.0,
        -PHI, 0.0, +1.0,
        +PHI, 0.0, -1.0,
        +PHI, 0.0, +1.0,
        -1.0, -PHI, 0.0,
        +1.0, -PHI, 0.0,
        -1.0, +PHI, 0.0,
        +1.0, +PHI, 0.0,
        0.0, -1.0, -PHI,
        0.0, +1.0, -PHI,
        0.0, -1.0, +PHI,
        0.0, +1.0, +PHI,
    )

    val indices = byteArrayOf(
        0, 4, 1, 2, 3, 5, 0, 1, 6, 2, 7, 3, 2, 5, 8, 0, 8, 4, 4, 8, 5, 6, 7, 9, 2, 9, 7, 0, 6, 9, 2,
        8, 9, 0, 9, 8, 3, 10, 5, 1, 4, 10, 4, 5, 10, 3, 7, 11, 6, 11, 7, 3, 11, 10, 1, 11, 6, 1, 10, 11
    )

    init {
        // normalize all positions, and rotate everything by https://oeis.org/A195693,
        // so it is even
        val s = 1.0 / length(1.0, PHI)
        val c = PHI * s
        for (i in positions.indices step 3) {
            val x = positions[i]
            val y = positions[i + 1]
            val z = positions[i + 2]
            positions[i] = (x * c - y * s) * s
            positions[i + 1] = (x * s + y * c) * s
            positions[i + 2] = z * s
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
            return if (x * x + z * z <= 1e-7f) 1f - atan2(z2, x2) / PIf
            else 1f - atan2(z, x) / PIf
        }

        fun v(x: Float, y: Float, z: Float): Float {
            return 1f - atan2(length(x, z), y) / PIf
        }

        fun addTriangle(
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
                addTriangle(
                    t, x0, y0, z0, u0, v0,
                    x01, y01, z01, u01, v01,
                    x20, y20, z20, u20, v20,
                )
                addTriangle(
                    t, x01, y01, z01, u01, v01,
                    x12, y12, z12, u12, v12,
                    x20, y20, z20, u20, v20,
                )
                addTriangle(
                    t, x1, y1, z1, u1, v1,
                    x12, y12, z12, u12, v12,
                    x01, y01, z01, u01, v01,
                )
                addTriangle(
                    t, x2, y2, z2, u2, v2,
                    x20, y20, z20, u20, v20,
                    x12, y12, z12, u12, v12,
                )
            } else {
                var u0x = u0
                var u1x = u1
                var u2x = u2
                // more than PI difference? fix it
                val mx = max(u0x, max(u1x, u2x))
                val mn = min(u0x, min(u1x, u2x))
                if (mx > mn + 1f) {
                    val avg = (mx + mn) * 0.5f
                    if (u0x > avg) u0x -= 2f
                    if (u1x > avg) u1x -= 2f
                    if (u2x > avg) u2x -= 2f
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
            val x0 = pos[a].toFloat()
            val y0 = pos[a + 1].toFloat()
            val z0 = pos[a + 2].toFloat()
            val x1 = pos[b].toFloat()
            val y1 = pos[b + 1].toFloat()
            val z1 = pos[b + 2].toFloat()
            val x2 = pos[c].toFloat()
            val y2 = pos[c + 1].toFloat()
            val z2 = pos[c + 2].toFloat()
            addTriangle(
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

        val vertexCount = (us + 1) * (vs + 1)

        val positions = mesh.positions.resize(3 * vertexCount)
        mesh.positions = positions
        val uvs = mesh.uvs.resize(2 * vertexCount)
        mesh.uvs = uvs

        // precalculate the angles? mmh...
        val cu = FloatArray(us + 1)
        val su = FloatArray(us + 1)
        for (i in 0..us) {
            val angle = (TAU * i / us).toFloat()
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
        for (v in 0 until vs) {
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

        return mesh
    }
}