package me.anno.gpu.buffer

import org.lwjgl.opengl.GL11

enum class AttributeType(val byteSize: Int, val glType: Int, val normalized: Boolean){
    FLOAT(4, GL11.GL_FLOAT, false),
    UINT8(1, GL11.GL_UNSIGNED_BYTE, false),
    UINT8_NORM(1, GL11.GL_UNSIGNED_BYTE, true)
}