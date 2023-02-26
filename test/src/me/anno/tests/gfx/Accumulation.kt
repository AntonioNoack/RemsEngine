package me.anno.tests.gfx

import me.anno.Engine
import me.anno.gpu.GFXBase
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.Accumulation

fun main() {

    val src = IntArray(100) { it + 1 }
    val dst = IntArray(src.size)

    val buffer = ComputeBuffer(listOf(Attribute("v", AttributeType.UINT32, 1, true)), src.size)
    val tmp = ComputeBuffer(buffer.attributes, buffer.elementCount)

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
    val dstBuffer = Accumulation.accumulateI32(buffer, tmp)

    // read data from buffer
    val data = dstBuffer.readDataI(0L, dst)
    println("dst: ${data.joinToString()}")

    Engine.requestShutdown()

}