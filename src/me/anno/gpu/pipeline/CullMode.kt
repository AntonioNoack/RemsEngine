package me.anno.gpu.pipeline

import org.lwjgl.opengl.GL11C.GL_BACK
import org.lwjgl.opengl.GL11C.GL_FRONT

enum class CullMode(val id: Int, val opengl: Int) {
    BOTH(0, 0),
    FRONT(1, GL_FRONT),
    BACK(2, GL_BACK)
}