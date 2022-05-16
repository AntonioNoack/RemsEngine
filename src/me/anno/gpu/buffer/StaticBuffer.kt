package me.anno.gpu.buffer

import me.anno.gpu.GFX
import me.anno.gpu.shader.Shader
import me.anno.image.svg.SVGMesh
import me.anno.maths.Maths.clamp
import me.anno.utils.pooling.ByteBufferPool
import org.joml.Vector2fc
import org.joml.Vector3fc
import org.joml.Vector4fc
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL33
import kotlin.math.roundToInt

open class StaticBuffer(attributes: List<Attribute>, var vertexCount: Int, usage: Int = GL_STATIC_DRAW) :
    Buffer(attributes, usage) {

    constructor(points: List<List<Float>>, attributes: List<Attribute>, vertices: IntArray) :
            this(attributes, vertices.size) {
        for (v in vertices) {
            for (p in points[v]) {
                put(p)
            }
        }
    }

    constructor(points: FloatArray, vertices: IntArray, attributes: List<Attribute>) :
            this(attributes, vertices.size) {
        val dimPerPoint = attributes.sumOf { it.components }
        for (v in vertices) {
            val baseIndex = v * dimPerPoint
            for (dOffset in 0 until dimPerPoint) {
                put(points[baseIndex + dOffset])
            }
        }
    }

    constructor(floats: FloatArray, attributes: List<Attribute>) :
            this(attributes, floats.size / attributes.sumOf { it.components }) {
        put(floats)
    }

    init {
        createNioBuffer()
    }

    var minX = 0.0
    var maxX = 0.0
    var minY = 0.0
    var maxY = 0.0

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

    fun put(v: Vector2fc) {
        put(v.x(), v.y())
    }

    fun put(v: FloatArray) {
        for (vi in v) {
            put(vi)
        }
    }

    fun put(v: FloatArray, index: Int, length: Int) {
        for (i in index until index + length) {
            put(v[i])
        }
    }

    fun put(v: Vector3fc) {
        put(v.x(), v.y(), v.z())
    }

    fun put(v: Vector4fc) {
        put(v.x(), v.y(), v.z(), v.w())
    }

    fun put(x: Float, y: Float, z: Float, w: Float, a: Float) {
        put(x)
        put(y)
        put(z)
        put(w)
        put(a)
    }

    fun put(x: Float, y: Float, z: Float, w: Float) {
        put(x)
        put(y)
        put(z)
        put(w)
    }

    fun put(x: Float, y: Float, z: Float) {
        put(x)
        put(y)
        put(z)
    }

    fun put(x: Float, y: Float) {
        put(x)
        put(y)
    }

    fun put(f: Float) {
        nioBuffer!!.putFloat(f)
        isUpToDate = false
    }

    fun putByte(b: Byte) {
        nioBuffer!!.put(b)
        isUpToDate = false
    }

    fun putByte(f: Float) {
        val asInt = clamp(f * 127f, -127f, +127f).roundToInt()
        putByte(asInt.toByte())
    }

    fun putUByte(b: Int) {
        nioBuffer!!.put(b.toByte())
        isUpToDate = false
    }

    fun putShort(b: Short) {
        nioBuffer!!.putShort(b)
        isUpToDate = false
    }

    fun putUShort(b: Int) {
        nioBuffer!!.putShort(b.toShort())
        isUpToDate = false
    }

    fun putInt(b: Int) {
        nioBuffer!!.putInt(b)
        isUpToDate = false
    }

    fun putDouble(d: Double) {
        nioBuffer!!.putDouble(d)
        isUpToDate = false
    }

    fun setBounds(svg: SVGMesh) {
        minX = svg.minX
        maxX = svg.maxX
        minY = svg.minY
        maxY = svg.maxY
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
        val nio = ByteBufferPool.allocateDirect(byteSize)
        nioBuffer = nio
    }

    companion object {

        private val nullBuffer = StaticBuffer(
            listOf(Attribute("nothing0", AttributeType.UINT8_NORM, 4)),
            4
        ).apply {
            putInt(0)
            putInt(1)
            putInt(2)
            putInt(3)
        }

        fun drawArraysNull(shader: Shader, mode: Int, length: Int) {
            // we need a null array, or bind bogus data, because drivers don't like this
            // https://stackoverflow.com/questions/8039929/opengl-drawarrays-without-binding-vbo
            nullBuffer.drawMode = mode
            nullBuffer.ensureBuffer()
            nullBuffer.apply {
                val baseLength = when (mode) {
                    GL_LINE, GL_LINES -> 2
                    GL_TRIANGLES -> 3
                    GL_QUADS -> 4
                    else -> throw RuntimeException("DrawMode ${GFX.getName(mode)} is not supported")
                }
                bind(shader)
                GL33.glDrawArraysInstanced(mode, 0, baseLength, length)
                unbind(shader)
            }
        }

        fun join(buffers: List<StaticBuffer>): StaticBuffer? {
            if (buffers.isEmpty()) return null
            val vertexCount = buffers.sumOf { it.vertexCount }
            val sample = buffers.first()
            val joint = StaticBuffer(sample.attributes, vertexCount)
            for (buffer in buffers) joint.put(buffer)
            return joint
        }
    }

}