package me.anno.gpu.framebuffer

import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.GL_RGBA32F

data class TargetType(val type0: Int, val type1: Int, val fillType: Int, val bytesPerPixel: Int) {

    companion object {
        val UByteTarget4 = TargetType(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, 4)
        val FloatTarget4 = TargetType(GL_RGBA32F, GL_RGBA, GL_FLOAT, 4 * 4)
    }

}