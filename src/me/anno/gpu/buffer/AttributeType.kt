package me.anno.gpu.buffer

import org.lwjgl.opengl.GL11C.*

@Suppress("unused")
enum class AttributeType(val byteSize: Int, val glType: Int, val normalized: Boolean) {

    FLOAT(4, GL_FLOAT, false),
    DOUBLE(8, GL_DOUBLE, false),

    UINT8(1, GL_UNSIGNED_BYTE, false),
    UINT16(2, GL_UNSIGNED_SHORT, false),
    UINT32(4, GL_UNSIGNED_INT, false),
    SINT8(1, GL_BYTE, false),
    SINT16(2, GL_SHORT, false),
    SINT32(4, GL_INT, false),

    UINT8_NORM(1, GL_UNSIGNED_BYTE, true),
    UINT16_NORM(2, GL_UNSIGNED_SHORT, true),
    UINT32_NORM(4, GL_UNSIGNED_INT, true),
    SINT8_NORM(1, GL_BYTE, true),
    SINT16_NORM(2, GL_SHORT, true),
    SINT32_NORM(4, GL_INT, true),

}