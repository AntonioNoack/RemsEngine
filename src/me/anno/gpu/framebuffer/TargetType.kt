package me.anno.gpu.framebuffer

import org.lwjgl.opengl.GL30.*

data class TargetType(val type0: Int, val type1: Int, val fillType: Int, val bytesPerPixel: Int) {

    companion object {
        val UByteTarget1 = TargetType(GL_R8, GL_RED, GL_UNSIGNED_BYTE, 1)
        val FloatTarget1 = TargetType(GL_R32F, GL_RGBA, GL_FLOAT, 1 * 4)
        val UByteTarget2 = TargetType(GL_RG8, GL_RG, GL_UNSIGNED_BYTE, 2)
        val FloatTarget2 = TargetType(GL_RG32F, GL_RGBA, GL_FLOAT, 2 * 4)
        val UByteTarget3 = TargetType(GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE, 3)
        val FloatTarget3 = TargetType(GL_RGB32F, GL_RGBA, GL_FLOAT, 3 * 4)
        val UByteTarget4 = TargetType(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, 4)
        val FloatTarget4 = TargetType(GL_RGBA32F, GL_RGBA, GL_FLOAT, 4 * 4)
    }

}