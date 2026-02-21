package me.anno.gpu.buffer

import me.anno.gpu.GFX
import me.anno.gpu.GFX.INVALID_POINTER
import me.anno.gpu.shader.Shader
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.withFlag
import me.anno.utils.types.Booleans.withoutFlag
import org.lwjgl.opengl.GL46C
import org.lwjgl.opengl.GL46C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL46C.glVertexAttribDivisor
import org.lwjgl.opengl.GL46C.glVertexAttribIPointer
import org.lwjgl.opengl.GL46C.glVertexAttribPointer

object BufferState {

    private const val MAX_SIZE = 64

    private val boundBuffers = IntArray(MAX_SIZE)
    private val boundDivisors = IntArray(MAX_SIZE)
    private val boundLayouts = arrayOfNulls<AttributeLayout>(MAX_SIZE)
    private val boundLayoutIds = IntArray(MAX_SIZE)

    private val prevBuffers = IntArray(MAX_SIZE)
    private val prevDivisors = IntArray(MAX_SIZE)
    private val prevLayouts = arrayOfNulls<AttributeLayout>(MAX_SIZE)
    private val prevLayoutIds = IntArray(MAX_SIZE)

    fun unbindAll() {
        boundLayouts.fill(null)
    }

    fun invalidateBinding() {
        // good???
        boundLayouts.fill(null)
        prevLayouts.fill(null)
        enabledAttributes = 0 // good???
    }

    fun bind(
        buffer: GPUBuffer, glIndex: Int, divisor: Int,
        layout: AttributeLayout, layoutId: Int
    ) {
        check(buffer.pointer != INVALID_POINTER)
        boundBuffers[glIndex] = buffer.pointer
        boundDivisors[glIndex] = divisor
        boundLayouts[glIndex] = layout
        boundLayoutIds[glIndex] = layoutId
    }

    fun bindSetState(shader: Shader) {
        check(shader.attributes.size <= GFX.maxAttributes)
        for (glIndex in shader.attributes.indices) {
            val layout = boundLayouts[glIndex]
            if (layout != null) {
                val buffer = boundBuffers[glIndex]
                val layoutId = boundLayoutIds[glIndex]
                val divisor = boundDivisors[glIndex]
                if (buffer != prevBuffers[glIndex] ||
                    layout != prevLayouts[glIndex] ||
                    layoutId != prevLayoutIds[glIndex] ||
                    divisor != prevDivisors[glIndex]
                ) {
                    val type = layout.type(layoutId)
                    val numComponents = layout.components(layoutId)
                    val stride = layout.stride(layoutId)
                    val offset = layout.offset(layoutId).toLong() and 0xffffffffL
                    val typeId = type.glslId

                    GPUBuffer.bindBuffer(GL_ARRAY_BUFFER, buffer)
                    if (shader.isAttributeNative(glIndex)) {
                        // defined as integer and to be used as integer
                        glVertexAttribIPointer(
                            glIndex, numComponents, typeId,
                            stride, offset
                        )
                    } else {
                        glVertexAttribPointer(
                            glIndex, numComponents, typeId,
                            type.normalized, stride, offset
                        )
                    }
                    glVertexAttribDivisor(glIndex, divisor)

                    prevBuffers[glIndex] = buffer
                    prevDivisors[glIndex] = divisor
                    prevLayouts[glIndex] = layout
                    prevLayoutIds[glIndex] = layoutId
                }
                enable(glIndex)
            } else {
                // todo is this automatically set to 0?
                disable(glIndex)
            }
        }
    }

    private var enabledAttributes = 0

    private fun enable(index: Int) {
        val flag = 1 shl index
        if (!enabledAttributes.hasFlag(flag)) {
            GL46C.glEnableVertexAttribArray(index)
            enabledAttributes = enabledAttributes.withFlag(flag)
        }
    }

    private fun disable(index: Int) {
        val flag = 1 shl index
        if (enabledAttributes.hasFlag(flag)) {
            GL46C.glDisableVertexAttribArray(index)
            enabledAttributes = enabledAttributes.withoutFlag(flag)
        }
    }

}