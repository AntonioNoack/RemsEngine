package me.anno.tests.gfx

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.shader.Accumulation
import me.anno.jvm.HiddenOpenGLContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class AccumulationTest {
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testAccumulate() {
        val src = IntArray(100) { it + 1 }
        val dst = IntArray(src.size)

        val buffer = ComputeBuffer("values", bind(Attribute("v", AttributeType.UINT32, 1)), src.size)
        val tmp = ComputeBuffer("tmp", buffer.attributes, buffer.elementCount)

        HiddenOpenGLContext.createOpenGL()

        // store it inside a shader buffer
        val nio = buffer.getOrCreateNioBuffer()
        nio.position(0)
        nio.asIntBuffer().put(src)
        nio.position(src.size * 4)
        buffer.ensureBuffer()

        tmp.getOrCreateNioBuffer().position(src.size * 4)
        tmp.ensureBuffer()

        // accumulate the values
        val dstBuffer = Accumulation.accumulateI32(buffer, tmp)

        // read data from buffer
        val data = dstBuffer.readDataI(0L, dst)
        for (i in src.indices) {
            Assertions.assertEquals(((i + 1) * (i + 2)) / 2, data[i])
        }
    }
}