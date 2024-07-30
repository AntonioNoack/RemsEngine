package me.anno.gpu.buffer

import me.anno.gpu.GFXState
import me.anno.gpu.shader.Shader
import me.anno.utils.pooling.ByteBufferPool
import org.joml.AABBf
import org.lwjgl.opengl.GL46C
import java.nio.ByteOrder

open class StaticBuffer(
    name: String, attributes: List<Attribute>,
    var vertexCount: Int, usage: BufferUsage = BufferUsage.STATIC
) : Buffer(name, attributes, usage) {

    constructor(name: String, points: FloatArray, vertices: IntArray, attributes: List<Attribute>) :
            this(name, attributes, vertices.size) {
        val dimPerPoint = attributes.sumOf { it.components }
        for (v in vertices) {
            val baseIndex = v * dimPerPoint
            for (dOffset in 0 until dimPerPoint) {
                put(points[baseIndex + dOffset])
            }
        }
    }

    constructor(name: String, floats: FloatArray, attributes: List<Attribute>) :
            this(name, attributes, floats.size / attributes.sumOf { it.components }) {
        put(floats)
    }

    init {
        createNioBuffer()
    }

    // data for SVGs, might be removed in the future
    var bounds: AABBf? = null

    /**
     * copies all information over
     * */
    fun put(s: StaticBuffer) {
        val dst = nioBuffer!!
        val src = s.nioBuffer!!
        val length = src.position()
        for (i in 0 until length) {
            dst.put(src[i])
        }
    }

    open fun clear() {
        val buffer = nioBuffer!!
        buffer.position(0)
        buffer.limit(buffer.capacity())
        isUpToDate = false
        drawLength = 0
    }

    final override fun createNioBuffer() {
        val byteSize = vertexCount * attributes.sumOf { it.byteSize }
        nioBuffer = ByteBufferPool.allocateDirect(byteSize)
            .order(ByteOrder.nativeOrder())
    }

    companion object {

        private val nullBuffer = StaticBuffer(
            "null",
            listOf(Attribute("nothing0", AttributeType.UINT8_NORM, 4)),
            4
        ).apply {
            putInt(0)
            putInt(1)
            putInt(2)
            putInt(3)
        }

        fun drawArraysNull(shader: Shader, mode: DrawMode, length: Int) {
            // we need a null array, or bind bogus data, because drivers don't like this
            // https://stackoverflow.com/questions/8039929/opengl-drawarrays-without-binding-vbo
            nullBuffer.drawMode = mode
            nullBuffer.ensureBuffer()
            nullBuffer.apply {
                val baseLength = mode.minLength
                bind(shader)
                GFXState.bind()
                GL46C.glDrawArraysInstanced(mode.id, 0, baseLength, length)
                unbind(shader)
            }
        }

        fun join(
            buffers: List<StaticBuffer>,
            newName: String = buffers.joinToString("-") { it.name }
        ): StaticBuffer? {
            if (buffers.isEmpty()) return null
            val vertexCount = buffers.sumOf { it.vertexCount }
            val sample = buffers.first()
            val joint = StaticBuffer(newName, sample.attributes, vertexCount)
            for (buffer in buffers) joint.put(buffer)
            return joint
        }
    }
}