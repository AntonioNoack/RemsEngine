package me.anno.games.roadcraft

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.interfaces.CustomEditMode
import me.anno.engine.raycast.RaycastMesh
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.AttributeReadWrite.createAccessors
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import me.anno.input.Input
import me.anno.maths.Maths.sq
import org.joml.Vector3d
import org.joml.Vector3i
import org.lwjgl.opengl.GL42C.glMemoryBarrier
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BARRIER_BIT

/**
 * Implements our first terrain-painting as a compute shader.
 * */
fun main() {

    val mesh = createSampleTerrain(50, 50)
    mesh.ensureBuffer()

    val shader = ComputeShader(
        "geometry", Vector3i(64, 1, 1), listOf(
            Variable(GLSLType.V3F, "position"),
            Variable(GLSLType.V1F, "dt"),
            Variable(GLSLType.V1F, "radiusSq"),
            Variable(GLSLType.V1I, "size")
        ), "" +
                createAccessors(
                    mesh.buffer!!, listOf(
                        Attribute("positions", 3),
                        Attribute("colors0", AttributeType.UINT8_NORM, 4)
                    ), "Vertex", 0, true
                ) +
                "void main() {\n" +
                "   uint index = gl_GlobalInvocationID.x;\n" +
                "   if(index < uint(size)) {\n" +
                "       vec3 pos = getVertexPositions(index);\n" +
                "       vec2 delta = position.xz - pos.xz;\n" +
                "       float distanceSq = dot(delta,delta);\n" +
                "       float force = 3.0 * dt * (1.0 / (1.0 + 2e3 * distanceSq / radiusSq) - 0.1);\n" +
                "       if (force > 0.0) {\n" +
                "           vec4 drawnColor = vec4(0.0);\n" +
                "           vec4 color = getVertexColors0(index);\n" +
                "           color = mix(drawnColor, color, exp(-force));\n" +
                "           pos.y += force;\n" +
                "           setVertexColors0(index, color);\n" +
                "           setVertexPositions(index, pos);\n" +
                "       }\n" +
                "   }\n" +
                "}\n"
    )

    val comp = object : Component(), CustomEditMode {
        private var lastTime = 0L
        override fun onEditMove(x: Float, y: Float, dx: Float, dy: Float): Boolean {
            if (!Input.isLeftDown) return false

            // prevent multiple executions per frame
            val time = Time.frameTimeNanos
            if (time == lastTime) return true
            lastTime = time

            val rv = RenderView.currentInstance ?: return false
            val ray = rv.rayQuery()
            if (RaycastMesh.raycastGlobalMesh(ray, null, mesh)) {
                val hit = ray.result.positionWS
                applyShader(hit, ray.result.distance)
            }

            return true
        }

        fun applyShader(pos: Vector3d, distance: Double) {
            val buffer = mesh.buffer ?: return
            shader.use()
            shader.v3f("position", pos)
            shader.v1i("size", buffer.elementCount)
            shader.v1f("radiusSq", sq(distance.toFloat() + 1f))
            shader.v1f("dt", Time.deltaTime.toFloat())
            shader.bindBuffer(0, buffer)
            shader.runBySize(buffer.elementCount)
            glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT)
        }
    }

    val scene = Entity()
        .add(MeshComponent(mesh))
        .add(comp)

    testSceneWithUI("TerrainCompute", scene)
}