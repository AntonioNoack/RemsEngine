package me.anno.gpu.framebuffer

import org.lwjgl.opengl.GL30.*

data class TargetType(
    val internalFormat: Int,
    val uploadFormat: Int,
    val fillType: Int,
    val bytesPerPixel: Int,
    val isHDR: Boolean
) {

    companion object {
        // luminance counts as a compressed format, so it can't be used
        // luminance_alpha neither
        val UByteTarget1 = TargetType(GL_R8, GL_RED, GL_UNSIGNED_BYTE, 1, false)
        val FloatTarget1 = TargetType(GL_R32F, GL_RGBA, GL_FLOAT, 1 * 4, true)
        val UByteTarget2 = TargetType(GL_RG8, GL_RG, GL_UNSIGNED_BYTE, 2, false)
        val FloatTarget2 = TargetType(GL_RG32F, GL_RGBA, GL_FLOAT, 2 * 4, true)
        val UByteTarget3 = TargetType(GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE, 3, false)
        val FloatTarget3 = TargetType(GL_RGB32F, GL_RGBA, GL_FLOAT, 3 * 4, true)
        val UByteTarget4 = TargetType(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, 4, false)
        val HalfFloatTarget4 = TargetType(GL_RGBA16F, GL_RGBA, GL_FLOAT, 2 * 4, true)
        val Normal12Target4 = // not working, because compressed formats are not color-renderable :/, why ever...
            HalfFloatTarget4 // TargetType(GL_UNSIGNED_INT_10_10_10_2, GL_RGBA, GL_UNSIGNED_BYTE, 4, true)
        val FloatTarget4 = TargetType(GL_RGBA32F, GL_RGBA, GL_FLOAT, 4 * 4, true)
    }

}