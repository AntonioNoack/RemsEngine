package me.anno.gpu.texture

import org.lwjgl.opengl.GL11C.*

enum class GPUFiltering(val mag: Int, val min: Int, val needsMipmap: Boolean) {
    NEAREST(GL_NEAREST, GL_LINEAR_MIPMAP_LINEAR, true),
    TRULY_NEAREST(GL_NEAREST, GL_NEAREST, false),
    LINEAR(GL_LINEAR, GL_LINEAR_MIPMAP_LINEAR, true),
    TRULY_LINEAR(GL_LINEAR, GL_LINEAR, false)
}