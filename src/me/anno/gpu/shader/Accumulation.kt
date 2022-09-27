package me.anno.gpu.shader

import me.anno.Engine
import me.anno.gpu.GFX
import me.anno.gpu.GFXBase
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.hidden.HiddenOpenGLContext
import org.joml.Vector3i
import org.lwjgl.opengl.GL42C
import kotlin.math.min

/**
 * calculates all partial sums of integer buffers
 * */
object Accumulation {

    // for the first ~10 rounds, we use shared memory for fewer invocations and synchronizations
    val smallStepShader = ComputeShader(
        "accumulate", Vector3i(1024, 1, 1), "" +
                "layout(std430, binding = 0) buffer dataLayout { uint data[]; };\n" +
                "uniform int totalSize;\n" +
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

    val generalShader = ComputeShader(
        "accumulate", Vector3i(1024, 1, 1), "" +
                "layout(std430, binding = 0) buffer srcLayout { uint src[]; };\n" +
                "layout(std430, binding = 1) buffer dstLayout { uint dst[]; };\n" +
                "uniform int totalSize;\n" +
                "uniform int step;\n" +
                "void main() {\n" +
                "   int index = int(gl_GlobalInvocationID.x);\n" +
                "   if(index < totalSize){\n" +
                "       dst[index] = (index & step) != 0 ?\n" +
                "           src[index] + src[(index - step) | (step - 1)] :\n" +
                "           src[index];\n" +
                "   }\n" +
                "}\n"
    )

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
                GL42C.glMemoryBarrier(GL42C.GL_UNIFORM_BARRIER_BIT)
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

    @JvmStatic
    fun main(args: Array<String>) {

        val src = IntArray(100) { it + 1 }
        val dst = IntArray(src.size)

        val buffer = ComputeBuffer(src.size, listOf(Attribute("v", AttributeType.UINT32, 1, true)))
        val tmp = ComputeBuffer(buffer.elementCount, buffer.attributes)

        GFXBase.forceLoadRenderDoc()

        HiddenOpenGLContext.createOpenGL()

        // store it inside a shader buffer
        val nio = buffer.nioBuffer!!
        nio.position(0)
        nio.asIntBuffer().put(src)
        nio.position(src.size * 4)
        buffer.ensureBuffer()
        tmp.ensureBuffer()

        // accumulate the values
        val dstBuffer = accumulateI32(buffer, tmp)

        // read data from buffer
        val data = dstBuffer.readDataI(0L, dst)
        println("dst: ${data.joinToString()}")

        Engine.requestShutdown()

    }

    // todo graphics-only implementation for web

    // I just re-invented geometry shaders... stupid!
    // todo how well do/would they work compared to normal geometry buffers?
    //class ConditionalDynamicShader {

        // to do components:
        // to do (compute-)generation shader

        // to do update() -> ensures buffers, generates geometry on gpu side
        // to do -> accumulate number of triangles
        // to do -> then write the triangles at their correct place

        // to do then later, use the mesh for rendering :)

        // sample: max(sin(x+y*0.1), 0), only generate triangle, if at least one entry is > 0

        // to do invoke drawing based on last element of accumulation table

   // }

}