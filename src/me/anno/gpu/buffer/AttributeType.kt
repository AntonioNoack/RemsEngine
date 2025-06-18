package me.anno.gpu.buffer

import me.anno.gpu.shader.GLSLType
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
enum class AttributeType(
    val id: Int,
    val byteSize: Int,
    val normalized: Boolean,
    val signed: Boolean,
    val glslId: Int
) {

    HALF(10, 2, false, true, GL_HALF_FLOAT),
    FLOAT(11, 4, false, true, GL_FLOAT),
    DOUBLE(12, 8, false, true, GL_DOUBLE),

    UINT8(20, 1, false, false, GL_UNSIGNED_BYTE),
    UINT16(21, 2, false, false, GL_UNSIGNED_SHORT),
    UINT32(22, 4, false, false, GL_UNSIGNED_INT),
    SINT8(23, 1, false, true, GL_BYTE),
    SINT16(24, 2, false, true, GL_SHORT),
    SINT32(25, 4, false, true, GL_INT),

    UINT8_NORM(30, 1, true, false, GL_UNSIGNED_BYTE),
    UINT16_NORM(31, 2, true, false, GL_UNSIGNED_SHORT),
    UINT32_NORM(32, 4, true, false, GL_UNSIGNED_INT),
    SINT8_NORM(33, 1, true, true, GL_BYTE),
    SINT16_NORM(34, 2, true, true, GL_SHORT),
    SINT32_NORM(35, 4, true, true, GL_INT),

    ;

    fun toGLSLType(numComponents: Int): GLSLType {
        return when (this) {
            UINT8, UINT16, UINT32,
            SINT8, SINT16, SINT32 -> GLSLType.floats
            else -> GLSLType.integers
        }[numComponents]
    }

    fun size(numComponents: Int): Int {
        return numComponents * byteSize
    }

    fun alignment(numComponents: Int): Int {
        val alignC = if (numComponents == 3) 4 else numComponents
        return alignC * byteSize
    }
}