package me.anno.gpu.framebuffer

import me.anno.gpu.GFX.getName
import org.lwjgl.opengl.GL30.*

class TargetType(
    val name: String,
    val internalFormat: Int,
    val uploadFormat: Int,
    val fillType: Int,
    val bytesPerPixel: Int,
    val channels: Int,
    val isHDR: Boolean
) {

    override fun toString(): String {
        return "TargetType($name, internalFormat=${getName(internalFormat)}, uploadFormat=${getName(uploadFormat)}, " +
                "fillType=${getName(fillType)}, bytesPerPixel=$bytesPerPixel, channels=$channels, isHDR=$isHDR)"
    }

    companion object {
        // luminance counts as a compressed format, so it can't be used
        // luminance_alpha neither
        val UByteTarget1 = TargetType("u1", GL_R8, GL_RED, GL_UNSIGNED_BYTE, 1, 1, false)
        val FP16Target1 = TargetType("h1", GL_R16F, GL_RED, GL_HALF_FLOAT, 2, 1, true)
        val FloatTarget1 = TargetType("f1", GL_R32F, GL_RGBA, GL_FLOAT, 1 * 4, 1, true)
        val UByteTarget2 = TargetType("u2", GL_RG8, GL_RG, GL_UNSIGNED_BYTE, 2, 2, false)
        val FP16Target2 = TargetType("h2", GL_RG16F, GL_RG, GL_HALF_FLOAT, 2 * 2, 2, true)
        val FloatTarget2 = TargetType("f2", GL_RG32F, GL_RGBA, GL_FLOAT, 2 * 4, 2, true)
        val UByteTarget3 = TargetType("u3", GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE, 3, 3, false)
        val FP16Target3 = TargetType("h3", GL_RGB16F, GL_RGB, GL_HALF_FLOAT, 2 * 3, 3, true)
        val FloatTarget3 = TargetType("f3", GL_RGB32F, GL_RGBA, GL_FLOAT, 3 * 4, 3, true)
        val UByteTarget4 = TargetType("u4", GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, 4, 4, false)
        val FP16Target4 = TargetType("h4", GL_RGBA16F, GL_RGBA, GL_HALF_FLOAT, 2 * 4, 4, true)
        val Normal12Target4 = // not working, because compressed formats are not color-renderable :/, why ever...
            FP16Target4 // TargetType(GL_UNSIGNED_INT_10_10_10_2, GL_RGBA, GL_UNSIGNED_BYTE, 4, true)
        val FloatTarget4 = TargetType("f4", GL_RGBA32F, GL_RGBA, GL_FLOAT, 4 * 4, 4, true)
    }

}