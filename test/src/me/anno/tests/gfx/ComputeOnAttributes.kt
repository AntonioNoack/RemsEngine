package me.anno.tests.gfx

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.WindowRenderFlags
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeLayout
import me.anno.gpu.buffer.AttributeReadWrite.createAccessors
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix3f
import org.joml.Vector3i
import org.lwjgl.opengl.GL46C.GL_SHADER_STORAGE_BARRIER_BIT
import org.lwjgl.opengl.GL46C.glMemoryBarrier

fun main() {
    rotatingCube()
}

fun rotatingCube() {

    // modify an attribute / index buffer using Compute Shaders
    val scene = Entity()
    val mesh = flatCube.front.shallowClone()

    val shader = LazyMap { layout: AttributeLayout ->
        ComputeShader(
            "geometry", Vector3i(64, 1, 1), listOf(
                Variable(GLSLType.M3x3, "rotation"),
                Variable(GLSLType.V1I, "size")
            ), "" +
                    createAccessors(
                        layout, listOf(
                            Attribute("positions", 3),
                            Attribute("normals", 3)
                        ), "Vertex", 0, true
                    ) +
                    "void main() {\n" +
                    "   uint index = gl_GlobalInvocationID.x;\n" +
                    "   if(index < uint(size)){\n" +
                    "       setVertexPositions(index, matMul(rotation, getVertexPositions(index)));\n" +
                    "       setVertexNormals(index, normalize(matMul(rotation, getVertexNormals(index))));\n" +
                    "   }\n" +
                    "}\n"
        )
    }

    val comp = object : Component(), OnUpdate {
        val rotation = Matrix3f().rotateY((5f).toRadians())
        override fun onUpdate() {
            val buffer = mesh.buffer ?: return
            val size = mesh.positions!!.size / 3
            val shader = shader[buffer.attributes]
            shader.use()
            shader.m3x3("rotation", rotation)
            shader.v1i("size", size)
            shader.bindBuffer(0, buffer)
            shader.runBySize(size)
            glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT)
        }
    }
    scene.add(comp)
    scene.add(MeshComponent(mesh))
    testSceneWithUI("Procedural GPU Mesh", scene) {
        WindowRenderFlags.enableVSync = true
    }
}