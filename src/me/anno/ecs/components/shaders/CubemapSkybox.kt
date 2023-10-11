package me.anno.ecs.components.shaders

import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import org.joml.Vector3f

/**
 * Typical skybox layout in a texture file:
 *    +y
 * -x -z +x +z
 *    -y
 * */
class CubemapSkybox : TextureSkybox() {

    init {
        material.shader = defaultShader
    }

    override fun getMesh(): Mesh = cubemapMesh

    override val className: String
        get() = "CubemapSkybox"

    companion object {

        @JvmStatic
        val cubemapMesh = Mesh().apply {
            name = "Cubemap"

            val numVertices = 36
            val positions = FloatArray(numVertices * 3)
            val uvs = FloatArray(numVertices * 2)

            var i = 0
            var j = 0

            fun put(v0: Vector3f, dx: Vector3f, dy: Vector3f, x: Float, y: Float, u: Int, v: Int) {
                positions[i++] = v0.x + dx.x * x + dy.x * y
                positions[i++] = v0.y + dx.y * x + dy.y * y
                positions[i++] = v0.z + dx.z * x + dy.z * y
                uvs[j++] = u / 4f
                uvs[j++] = v / 3f
            }

            fun addFace(u0: Int, v0: Int, p: Vector3f, dx: Vector3f, dy: Vector3f) {

                val u1 = u0 + 1
                val v1 = v0 + 1

                put(p, dx, dy, -1f, -1f, u1, v0)
                put(p, dx, dy, -1f, +1f, u1, v1)
                put(p, dx, dy, +1f, +1f, u0, v1)

                put(p, dx, dy, -1f, -1f, u1, v0)
                put(p, dx, dy, +1f, +1f, u0, v1)
                put(p, dx, dy, +1f, -1f, u0, v0)
            }

            val mxAxis = Vector3f(-1f, 0f, 0f)
            val myAxis = Vector3f(0f, -1f, 0f)
            val mzAxis = Vector3f(0f, 0f, -1f)

            val xAxis = Vector3f(1f, 0f, 0f)
            val yAxis = Vector3f(0f, 1f, 0f)
            val zAxis = Vector3f(0f, 0f, 1f)

            addFace(1, 1, mzAxis, mxAxis, yAxis) // center, front
            addFace(0, 1, mxAxis, zAxis, yAxis) // left, left
            addFace(2, 1, xAxis, mzAxis, yAxis) // right, right
            addFace(3, 1, zAxis, xAxis, yAxis) // 2x right, back
            addFace(1, 0, myAxis, mxAxis, mzAxis) // top
            addFace(1, 2, yAxis, mxAxis, zAxis) // bottom

            this.positions = positions
            this.uvs = uvs

        }

        val defaultShader = CubemapSkyboxShader("cubemap-skybox")
    }
}