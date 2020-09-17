package me.anno.gpu.buffer

import org.joml.Vector3f
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class StaticFloatBuffer(attributes: List<Attribute>, val vertexCount: Int): GPUFloatBuffer(attributes){

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

    fun put(v: Vector3f){
        put(v.x, v.y, v.z)
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
        floatBuffer.put(f)
        isUpToDate = false
    }

    final override fun createNioBuffer() {
        val byteSize = vertexCount * attributes.sumBy { it.components * it.byteSize }
        val nio = ByteBuffer.allocateDirect(byteSize).order(ByteOrder.nativeOrder())
        nioBuffer = nio
        floatBuffer = nio.asFloatBuffer()
        floatBuffer.position(0)
    }
}