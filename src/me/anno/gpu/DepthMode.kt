package me.anno.gpu

import org.lwjgl.opengl.GL11.*

enum class DepthMode(val func: Int, val reversedDepth: Boolean) {
    ALWAYS(0, true),
    EQUALS(GL_EQUAL, true),
    NOT_EQUAL(GL_NOTEQUAL, true),
    LESS_EQUAL(GL_LEQUAL, true),
    LESS(GL_LESS, true),
    GREATER_EQUAL(GL_GEQUAL, true),
    GREATER(GL_GREATER, true),
    FORWARD_ALWAYS(0, false),
    FORWARD_EQUALS(GL_EQUAL, false),
    FORWARD_NOT_EQUAL(GL_NOTEQUAL, false),
    FORWARD_LESS_EQUAL(GL_LEQUAL, false),
    FORWARD_LESS(GL_LESS, false),
    FORWARD_GREATER_EQUAL(GL_GEQUAL, false),
    FORWARD_GREATER(GL_GREATER, false)
}