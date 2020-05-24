package me.anno.gpu.buffer

import org.lwjgl.opengl.GL11

enum class AttributeType(val byteSize: Int, val glType: Int, val normalized: Boolean){
    FLOAT(4, GL11.GL_FLOAT, false),
}