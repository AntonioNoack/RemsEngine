package me.anno.gpu.buffer

import me.anno.gpu.buffer.Buffer.Companion.bindBuffer
import me.anno.gpu.texture.Texture2D
import me.anno.utils.pooling.ByteBufferPool
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL15C.glGenBuffers
import org.lwjgl.opengl.GL31C.glDrawElementsInstanced
import kotlin.math.min

object DrawLinesBuffer {

    fun drawLines(triangleCount: Int) {
        ensureLineBuffer()
        val count = min(triangleCount * 2, lineBufferLength)
        glDrawElements(GL_LINES, count, GL_UNSIGNED_INT, 0)
    }

    fun drawLinesInstanced(triangleCount: Int, instanceCount: Int) {
        ensureLineBuffer()
        val count = min(triangleCount * 2, lineBufferLength)
        glDrawElementsInstanced(GL_LINES, count, GL_UNSIGNED_INT, 0, instanceCount)
    }

    private const val lineBufferLength = 1 shl 20
    private var lineBuffer = -1
    private fun ensureLineBuffer() {
        if (lineBuffer < 0) {
            // GFX.check()
            lineBuffer = glGenBuffers()
            if (lineBuffer < 0) throw RuntimeException("Failed to create buffer")
            bindBuffer(GL_ELEMENT_ARRAY_BUFFER, lineBuffer)
            val nioBytes = Texture2D.bufferPool[4 * lineBufferLength, false, false]
            val nioBuffer = nioBytes
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
            ByteBufferPool.free(nioBytes)
            // GFX.check()
        } else bindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, lineBuffer)
    }

}