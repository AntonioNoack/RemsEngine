package me.anno.gpu.buffer

import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class StaticBuffer(attributes: List<Attribute>, val vertexCount: Int): Buffer(attributes){

    constructor(points: List<List<Float>>, attributes: List<Attribute>, vertices: IntArray): this(attributes, vertices.size){
        vertices.forEach {
            points[it].forEach { v ->
                put(v)
            }
        }
    }

    init {
        createNioBuffer()
    }

    fun put(v: Vector2f){
        put(v.x, v.y)
    }

    fun put(v: Vector3f){
        put(v.x, v.y, v.z)
    }

    fun put(v: Vector4f){
        put(v.x, v.y, v.z, v.w)
    }

    fun put(x: Float, y: Float, z: Float, w: Float, a: Float){
        put(x)
        put(y)
        put(z)
        put(w)
        put(a)
    }

    fun put(x: Float, y: Float, z: Float, w: Float){
        put(x)
        put(y)
        put(z)
        put(w)
    }

    fun put(x: Float, y: Float, z: Float){
        put(x)
        put(y)
        put(z)
    }

    fun put(x: Float, y: Float){
        put(x)
        put(y)
    }

    fun put(f: Float){
        nioBuffer!!.putFloat(f)
        isUpToDate = false
    }

    fun putByte(b: Byte){
        nioBuffer!!.put(b)
        isUpToDate = false
    }

    fun putUByte(b: Int){
        nioBuffer!!.put(b.toByte())
        isUpToDate = false
    }

    fun putShort(b: Short){
        nioBuffer!!.putShort(b)
        isUpToDate = false
    }

    fun putUShort(b: Int){
        nioBuffer!!.putShort(b.toShort())
        isUpToDate = false
    }

    fun putInt(b: Int){
        nioBuffer!!.putInt(b)
        isUpToDate = false
    }

    fun putDouble(d: Double){
        nioBuffer!!.putDouble(d)
        isUpToDate = false
    }

    final override fun createNioBuffer() {
        val byteSize = vertexCount * attributes.sumBy { it.byteSize }
        val nio = ByteBuffer.allocateDirect(byteSize).order(ByteOrder.nativeOrder())
        nioBuffer = nio
    }
}