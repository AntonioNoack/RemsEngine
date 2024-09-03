package me.anno.ecs.components.mesh.shapes

import me.anno.mesh.Shapes
import org.joml.Vector3f

/**
 * Cubemap model with UVs for typical skybox layout:
 *    +y
 * -x -z +x +z
 *    -y
 * */
object CubemapModel {

    val model = createMesh()

    private fun createMesh(): Shapes.FBBMesh {
        val numVertices = 36
        val positions = FloatArray(numVertices * 3)
        val uvs = FloatArray(numVertices * 2)

        var i = 0
        var j = 0

        fun put(v0: Vector3f, dx: Vector3f, dy: Vector3f, x: Float, y: Float, u: Float, v: Float) {
            positions[i++] = v0.x + dx.x * x + dy.x * y
            positions[i++] = v0.y + dx.y * x + dy.y * y
            positions[i++] = v0.z + dx.z * x + dy.z * y
            uvs[j++] = u
            uvs[j++] = v
        }

        fun addFace(u: Int, v: Int, p: Vector3f, dx: Vector3f, dy: Vector3f) {

            val u0 = u / 4f
            val v0 = v / 3f

            val u1 = (u + 1) / 4f
            val v1 = (v + 1) / 3f

            put(p, dx, dy, -1f, -1f, u1, v0)
            put(p, dx, dy, +1f, +1f, u0, v1)
            put(p, dx, dy, -1f, +1f, u1, v1)

            put(p, dx, dy, -1f, -1f, u1, v0)
            put(p, dx, dy, +1f, -1f, u0, v0)
            put(p, dx, dy, +1f, +1f, u0, v1)
        }

        val pxAxis = Vector3f(1f, 0f, 0f)
        val pyAxis = Vector3f(0f, 1f, 0f)
        val pzAxis = Vector3f(0f, 0f, 1f)

        val mxAxis = Vector3f(-1f, 0f, 0f)
        val myAxis = Vector3f(0f, -1f, 0f)
        val mzAxis = Vector3f(0f, 0f, -1f)

        addFace(1, 1, mzAxis, mxAxis, pyAxis) // center, front
        addFace(0, 1, mxAxis, pzAxis, pyAxis) // left, left
        addFace(2, 1, pxAxis, mzAxis, pyAxis) // right, right
        addFace(3, 1, pzAxis, pxAxis, pyAxis) // 2x right, back
        addFace(1, 0, myAxis, mxAxis, mzAxis) // top
        addFace(1, 2, pyAxis, mxAxis, pzAxis) // bottom

        val mesh = Shapes.FBBMesh("Cubemap", positions, null)
        mesh.front.uvs = uvs
        mesh.back.uvs = uvs
        mesh.both.uvs = uvs
        return mesh
    }
}