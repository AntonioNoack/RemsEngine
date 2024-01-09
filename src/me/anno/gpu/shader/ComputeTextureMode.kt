package me.anno.gpu.shader

import org.lwjgl.opengl.GL46C

enum class ComputeTextureMode(val code: Int) {
    READ(GL46C.GL_READ_ONLY),
    WRITE(GL46C.GL_WRITE_ONLY),
    READ_WRITE(GL46C.GL_READ_WRITE)
}