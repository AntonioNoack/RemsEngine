package me.anno.ui.canvas

import me.anno.cache.ICacheData
import me.anno.gpu.GFX.INVALID_POINTER
import me.anno.gpu.GFX.INVALID_SESSION
import me.anno.gpu.GFX.isPointerValid
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.BufferState
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.shader.GPUShader
import org.lwjgl.opengl.GL46C.glBindVertexArray
import org.lwjgl.opengl.GL46C.glCreateVertexArrays
import org.lwjgl.opengl.GL46C.glDeleteVertexArrays
import org.lwjgl.opengl.GL46C.glDrawArraysInstanced

/**
 * VAO / Fixed binding for buffers.
 * Can only be used, if all buffers are always bound the same way.
 * The default part of the engine is very dynamic, so we avoid it there.
 *
 * On my 7900 XTX + 7950X3D, this has no performance impact, but there are much fewer calls in RenderDoc (4.2k -> 2.5k).
 * */
class FixedAttributeBinding(
    val shapeBuffer: Buffer,
    val instancedBuffer: Buffer?,
    val shader: GPUShader,
) : ICacheData {

    var pointer = INVALID_POINTER
    var session = INVALID_SESSION

    private fun create() {
        pointer = glCreateVertexArrays()
        session = GFXState.session
        bindImpl()
        recordBindings()
    }

    private fun recordBindings() {
        val shader = CanvasShader
        shapeBuffer.ensureBuffer()
        instancedBuffer?.ensureBuffer()

        BufferState.invalidateBinding()
        shapeBuffer.bindAttributes(shader, false)
        instancedBuffer?.bindAttributes(shader, true)
        BufferState.bindSetState(shader)
        BufferState.invalidateBinding()
    }

    fun isValid() = isPointerValid(pointer) && GFXState.session == session

    fun bind() {
        if (isValid()) bindImpl()
        else create()
    }

    private fun bindImpl() {
        glBindVertexArray(pointer)
    }

    fun drawInstanced(shapeData: Buffer, instanceData: Buffer, drawMode: DrawMode) {
        GFXState.bind()
        shapeData.ensureBuffer()
        instanceData.ensureBuffer()
        glDrawArraysInstanced(drawMode.id, 0, shapeData.drawLength, instanceData.drawLength)
    }

    override fun destroy() {
        if (session != GFXState.session) {
            pointer = INVALID_POINTER
        }
        if (isPointerValid(pointer)) {
            glDeleteVertexArrays(pointer)
            pointer = INVALID_POINTER
        }
    }

    companion object {
        fun unbind() {
            GFXState.bindVAO()
        }
    }

}