package me.anno.gpu.buffer

import me.anno.gpu.GFX
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL31
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

object DrawLinesBuffer {
    
    fun drawLines(triangleCount: Int) {
        ensureLineBuffer()
        val count = min(triangleCount * 2, lineBufferLength)
        GL11.glDrawElements(GL11.GL_LINES, count, GL11.GL_UNSIGNED_INT, 0)
    }

    fun drawLinesInstanced(triangleCount: Int, instanceCount: Int) {
        ensureLineBuffer()
        val count = min(triangleCount * 2, lineBufferLength)
        GL31.glDrawElementsInstanced(GL11.GL_LINES, count, GL11.GL_UNSIGNED_INT, 0, instanceCount)
    }

    private const val lineBufferLength = 1 shl 20
    private var lineBuffer = -1
    private fun ensureLineBuffer() {
        if (lineBuffer < 0) {
            GFX.check()
            lineBuffer = GL20.glGenBuffers()
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, lineBuffer)
            val nioBuffer = ByteBuffer.allocateDirect(4 * lineBufferLength)
                    .order(ByteOrder.nativeOrder())
                    .asIntBuffer()
            nioBuffer.position(0)
            for (i in 0 until lineBufferLength / 6) {
                // 01 12 20
                val j = i * 3
                nioBuffer.put(j)
                nioBuffer.put(j + 1)
                nioBuffer.put(j + 1)
                nioBuffer.put(j + 2)
                nioBuffer.put(j + 2)
                nioBuffer.put(j)
            }
            nioBuffer.position(0)
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, nioBuffer, GL15.GL_STATIC_DRAW)
            GFX.check()
        } else GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, lineBuffer)
    }
    
}