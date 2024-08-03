package me.anno.gpu.buffer

import org.lwjgl.opengl.GL46C.GL_BYTE
import org.lwjgl.opengl.GL46C.GL_DOUBLE
import org.lwjgl.opengl.GL46C.GL_FLOAT
import org.lwjgl.opengl.GL46C.GL_HALF_FLOAT
import org.lwjgl.opengl.GL46C.GL_INT
import org.lwjgl.opengl.GL46C.GL_SHORT
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_SHORT

@Suppress("unused")
enum class AttributeType(val byteSize: Int, val normalized: Boolean, val signed: Boolean, val id: Int) {

    HALF(2, false, true, GL_HALF_FLOAT),
    FLOAT(4, false, true, GL_FLOAT),
    DOUBLE(8, false, true, GL_DOUBLE),

    UINT8(1, false, false, GL_UNSIGNED_BYTE),
    UINT16(2, false, false, GL_UNSIGNED_SHORT),
    UINT32(4, false, false, GL_UNSIGNED_INT),
    SINT8(1, false, true, GL_BYTE),
    SINT16(2, false, true, GL_SHORT),
    SINT32(4, false, true, GL_INT),

    UINT8_NORM(1, true, false, GL_UNSIGNED_BYTE),
    UINT16_NORM(2, true, false, GL_UNSIGNED_SHORT),
    UINT32_NORM(4, true, false, GL_UNSIGNED_INT),
    SINT8_NORM(1, true, true, GL_BYTE),
    SINT16_NORM(2, true, true, GL_SHORT),
    SINT32_NORM(4, true, true, GL_INT),
}