package me.anno.tests.gfx

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.shader.BufferCompute.createAccessors
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import me.anno.mesh.Shapes.flatCube
import me.anno.studio.StudioBase
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix3f
import org.joml.Vector2i
import org.lwjgl.opengl.GL42C.glMemoryBarrier
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BARRIER_BIT

fun main() {
    rotatingCube()
}

fun rotatingCube() {
    // modify an attribute / index buffer using Compute Shaders
    val scene = Entity()
    val mesh = flatCube.front.clone() as Mesh
    mesh.ensureBuffer()
    val shader = ComputeShader(
        "geometry", Vector2i(64, 1), listOf(
            Variable(GLSLType.M3x3, "rotation"),
            Variable(GLSLType.V1I, "size")
        ), "" +
                createAccessors(
                    mesh.buffer!!, listOf(
                        Attribute("coords", 3),
                        Attribute("normals", 3)
                    ), "Vertex", 0, true
                ) +
                "void main() {\n" +
                "   uint index = gl_GlobalInvocationID.x;\n" +
                "   if(index < uint(size)){\n" +
                "       setVertexCoords(index, matMul(rotation, getVertexCoords(index)));\n" +
                "       setVertexNormals(index, normalize(matMul(rotation, getVertexNormals(index))));\n" +
                "   }\n" +
                "}\n"
    )
    val comp = object : Component() {
        val rotation = Matrix3f().rotateY((5f).toRadians())
        override fun onUpdate(): Int {
            val buffer = mesh.buffer ?: return 1
            shader.use()
            shader.m3x3("rotation", rotation)
            shader.v1i("size", buffer.elementCount)
            shader.bindBuffer(0, buffer)
            shader.runBySize(buffer.elementCount)
            glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT)
            return 1
        }
    }
    scene.add(comp)
    scene.add(MeshComponent(mesh))
    testSceneWithUI("Procedural GPU Mesh", scene) {
        StudioBase.instance?.enableVSync = true
    }
}