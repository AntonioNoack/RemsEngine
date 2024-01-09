package me.anno.gpu.buffer

import org.lwjgl.opengl.GL46C.*

@Suppress("unused")
enum class AttributeType(val byteSize: Int, val normalized: Boolean, val id: Int) {

    HALF(2, false, GL_HALF_FLOAT),
    FLOAT(4, false, GL_FLOAT),
    DOUBLE(8, false, GL_DOUBLE),

    UINT8(1, false, GL_UNSIGNED_BYTE),
    UINT16(2, false, GL_UNSIGNED_SHORT),
    UINT32(4, false, GL_UNSIGNED_INT),
    SINT8(1, false, GL_BYTE),
    SINT16(2, false, GL_SHORT),
    SINT32(4, false, GL_INT),

    UINT8_NORM(1, true, GL_UNSIGNED_BYTE),
    UINT16_NORM(2, true, GL_UNSIGNED_SHORT),
    UINT32_NORM(4, true, GL_UNSIGNED_INT),
    SINT8_NORM(1, true, GL_BYTE),
    SINT16_NORM(2, true, GL_SHORT),
    SINT32_NORM(4, true, GL_INT),

}