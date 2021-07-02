package me.anno.mesh.jmonkey

import com.jme3.scene.VertexBuffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

object BufferConverter {

    fun convertInt32(buffer: VertexBuffer): IntArray {
        val data = buffer.data
        return when (buffer.format) {
            VertexBuffer.Format.Int, VertexBuffer.Format.UnsignedInt -> {
                data as IntBuffer
                IntArray(data.capacity()) { data[it] }
            }
            VertexBuffer.Format.Short, VertexBuffer.Format.UnsignedShort -> {
                data as ShortBuffer
                IntArray(data.capacity()) { data[it].toUInt().toInt() }
            }
            VertexBuffer.Format.Byte, VertexBuffer.Format.UnsignedByte -> {
                data as ByteBuffer
                IntArray(data.capacity()) { data[it].toUInt().toInt() }
            }
            VertexBuffer.Format.Float -> {
                data as FloatBuffer
                IntArray(data.capacity()) { data[it].toInt() }
            }
            else -> throw NotImplementedError("${buffer.format} for ints")
        }
    }

    fun convertUByte(buffer: VertexBuffer): ByteArray {
        val data = buffer.data
        return when (buffer.format) {
            VertexBuffer.Format.Int, VertexBuffer.Format.UnsignedInt -> {
                data as IntBuffer
                ByteArray(data.capacity()) { data[it].toByte() }
            }
            VertexBuffer.Format.Short, VertexBuffer.Format.UnsignedShort -> {
                data as ShortBuffer
                ByteArray(data.capacity()) { data[it].toByte() }
            }
            VertexBuffer.Format.Byte, VertexBuffer.Format.UnsignedByte -> {
                data as ByteBuffer
                ByteArray(data.capacity()) { data[it] }
            }
            else -> throw NotImplementedError("${buffer.format} for bytes")
        }
    }

    fun convertFloats(buffer: VertexBuffer): FloatArray {
        val data = buffer.data
        return when (buffer.format) {
            VertexBuffer.Format.Float -> {
                data as FloatBuffer
                FloatArray(data.capacity()) { data[it] }
            }
            else -> throw NotImplementedError("${buffer.format} for floats")
        }
    }

}