package me.anno.gpu.buffer

import java.nio.ByteBuffer
import java.nio.ByteOrder

open class StaticFloatBuffer(attributes: List<Attribute>, val floatCount: Int): GPUFloatBuffer(attributes){

    init {
        createNioBuffer()
    }

    fun put(f: Float){
        floatBuffer.put(f)
        isUpToDate = false
    }

    final override fun createNioBuffer() {
        val nio = ByteBuffer.allocateDirect(floatCount * 4).order(ByteOrder.nativeOrder())
        nioBuffer = nio
        floatBuffer = nio.asFloatBuffer()
        floatBuffer.position(0)
    }
}