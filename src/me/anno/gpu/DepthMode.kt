package me.anno.gpu

import org.lwjgl.opengl.GL11.*

enum class DepthMode(val func: Int, val withEqual: Int, val withoutEqual: Int) {
    ALWAYS(0, 0, 0),
    EQUALS(GL_EQUAL, GL_EQUAL, GL_NEVER),
    NOT_EQUAL(GL_NOTEQUAL, GL_ALWAYS, GL_NOTEQUAL),
    LESS_EQUAL(GL_LEQUAL, GL_LEQUAL, GL_LESS),
    LESS(GL_LESS, GL_LEQUAL, GL_LESS),
    GREATER_EQUAL(GL_GEQUAL, GL_GEQUAL, GL_GREATER),
    GREATER(GL_GREATER, GL_GEQUAL, GL_GREATER)
}