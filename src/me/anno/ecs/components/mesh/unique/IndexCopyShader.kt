package me.anno.ecs.components.mesh.unique

import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import org.joml.Vector3i
import org.lwjgl.opengl.GL42C.glMemoryBarrier
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BARRIER_BIT

object IndexCopyShader {
    private val shader = ComputeShader(
        "backward", 460, Vector3i(64, 1, 1), listOf(
            Variable(GLSLType.V1I, "inputOffset"),
            Variable(GLSLType.V1I, "outputOffset"),
            Variable(GLSLType.V1I, "valueDelta"),
            Variable(GLSLType.V1I, "copyLength"),
        ), "" +
                "layout(std430, binding=0) readonly  buffer inputBuffer1  { int inputBuffer[]; };\n" +
                "layout(std430, binding=1) writeonly buffer outputBuffer1 { int outputBuffer[]; };\n" +
                "void main() {\n" +
                "   int copyIndex = int(gl_GlobalInvocationID.x);\n" +
                "   if(copyIndex>=copyLength)return;\n" +
                "   outputBuffer[copyIndex + outputOffset] = inputBuffer[copyIndex + inputOffset] + valueDelta;\n" +
                "}\n"
    )

    fun copyIndices(
        srcOffset: Int, srcData: OpenGLBuffer, dstOffset: Int, dstData: OpenGLBuffer,
        numElementsToCopy: Int, valueDelta: Int
    ) {
        val shader = shader
        shader.use()
        shader.v1i("inputOffset", srcOffset)
        shader.v1i("outputOffset", dstOffset)
        shader.v1i("copyLength", numElementsToCopy)
        shader.v1i("valueDelta", valueDelta)
        shader.bindBuffer(0, srcData)
        shader.bindBuffer(1, dstData)
        shader.runBySize(numElementsToCopy)
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT)
    }
}