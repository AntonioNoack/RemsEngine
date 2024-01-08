package me.anno.gpu.shader

import org.lwjgl.opengl.GL15C

enum class ComputeTextureMode(val code: Int) {
    READ(GL15C.GL_READ_ONLY),
    WRITE(GL15C.GL_WRITE_ONLY),
    READ_WRITE(GL15C.GL_READ_WRITE)
}