package me.anno.tests.gfx

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import me.anno.mesh.Shapes.flatCube
import me.anno.studio.StudioBase
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix3f
import org.joml.Vector2i
import org.lwjgl.opengl.GL30C.glBindBufferBase
import org.lwjgl.opengl.GL42C.glMemoryBarrier
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BARRIER_BIT
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER

fun main() {
    // modify an attribute / index buffer using Compute Shaders
    val scene = Entity()
    val mesh = flatCube.front.clone() as Mesh
    val shader = ComputeShader(
        "geometry", Vector2i(64, 1), listOf(
            Variable(GLSLType.M3x3, "rotation"),
            Variable(GLSLType.V1I, "size")
        ), "" +
                "struct Vertex {\n" +
                "   vec3 position;\n" +
                "   int normal;\n" +
                "};\n" +
                "layout(std140, set = 0, binding = 0) buffer AttributeBuffer {\n" +
                "    Vertex data[];\n" +
                "} attributes;\n" +
                "void main() {\n" +
                "   uint globalID = gl_GlobalInvocationID.x;\n" +
                "   if(globalID < uint(size)){\n" +
                // No need to explicitly write to the output buffer, modifications are done in place
                "       attributes.data[globalID].position *= rotation;\n" +
                "       int norm = attributes.data[globalID].normal;\n" +
                "       vec3 normal = normalize(vec3((norm<<24)>>24,(norm<<16)>>24,(norm<<8)>>24));\n" +
                "       normal *= rotation;\n" +
                "       ivec3 normal1 = ivec3(round(normal * 127.49));\n" +
                "       attributes.data[globalID].normal = ((normal1.x & 255)) | ((normal1.y & 255)<<8) | ((normal1.z & 255)<<16);\n" +
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
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, buffer.pointer)
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