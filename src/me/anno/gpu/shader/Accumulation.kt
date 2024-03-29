package me.anno.gpu.shader

import me.anno.gpu.GFX
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.shader.builder.Variable
import org.joml.Vector3i
import org.lwjgl.opengl.GL46C.GL_UNIFORM_BARRIER_BIT
import org.lwjgl.opengl.GL46C.glMemoryBarrier
import kotlin.math.min

/**
 * calculates all partial sums of integer buffers
 * */
object Accumulation {

    // for the first ~10 rounds, we use shared memory for fewer invocations and synchronizations
    @JvmField
    val smallStepShader = ComputeShader(
        "accumulate", Vector3i(1024, 1, 1), listOf(
            Variable(GLSLType.V1I, "totalSize"),
        ), "" +
                "layout(std430, binding = 0) buffer dataLayout { uint data[]; };\n" +
                "shared uint[1024] tmp;\n" + // 32 kB are guaranteed to exist -> we're fine with using 4 kB :)
                "void main() {\n" +
                "   int index = int(gl_GlobalInvocationID.x);\n" +
                "   if(index < totalSize){\n" +
                "       int localIndex = index & 1023;\n" +
                "       tmp[localIndex] = data[index];\n" +
                "       for(int step = 1;step < totalSize;step = step << 1){\n" +
                "           barrier();\n" +
                "           tmp[localIndex] += (localIndex & step) != 0 ? tmp[(localIndex - step) | (step - 1)] : 0;\n" +
                "       }\n" +
                "       data[index] = tmp[localIndex];\n" +
                "   }\n" +
                "}\n"
    )

    @JvmField
    val generalShader = ComputeShader(
        "accumulate", Vector3i(1024, 1, 1), listOf(
            Variable(GLSLType.V1I, "totalSize"),
            Variable(GLSLType.V1I, "step")
        ), "" +
                "layout(std430, binding = 0) buffer srcLayout { uint src[]; };\n" +
                "layout(std430, binding = 1) buffer dstLayout { uint dst[]; };\n" +
                "void main() {\n" +
                "   int index = int(gl_GlobalInvocationID.x);\n" +
                "   if(index < totalSize){\n" +
                "       dst[index] = (index & step) != 0 ?\n" +
                "           src[index] + src[(index - step) | (step - 1)] :\n" +
                "           src[index];\n" +
                "   }\n" +
                "}\n"
    )

    @JvmStatic
    fun accumulateI32(buffer: ComputeBuffer, tmp: ComputeBuffer): ComputeBuffer {
        GFX.check()
        val size = min(buffer.elementCount, tmp.elementCount)
        var step = 1
        var src = buffer
        var dst = tmp
        if (step < size) {
            val shader = smallStepShader
            shader.use()
            shader.bindBuffer(0, src)
            shader.v1i("totalSize", size)
            shader.runBySize(buffer.elementCount, 1, 1)
            step = 1024
        }
        if (step < size) {
            val shader = generalShader
            shader.use()
            shader.v1i("totalSize", size)
            while (step < size) {
                shader.v1i("step", step)
                shader.bindBuffer(0, src)
                shader.bindBuffer(1, dst)
                glMemoryBarrier(GL_UNIFORM_BARRIER_BIT)
                shader.runBySize(buffer.elementCount, 1, 1)
                val tmp1 = src
                src = dst
                dst = tmp1
                step = step shl 1
            }
        }
        GFX.check()
        return src
    }
    // todo graphics-only implementation for web
}