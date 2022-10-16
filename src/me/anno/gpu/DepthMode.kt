package me.anno.gpu

import org.lwjgl.opengl.GL11C.*

@Suppress("unused")
enum class DepthMode(val func: Int, val reversedDepth: Boolean) {
    // default depth mode is reversed in the engine, because it is superior
    ALWAYS(0, true),
    ALWAYS11(0, false),
    DIFFERENT(GL_NOTEQUAL, true),
    FARTHER(GL_LESS, true),
    FAR(GL_LEQUAL, true),
    EQUALS(GL_EQUAL, true),
    CLOSE(GL_GEQUAL, true),
    CLOSER(GL_GREATER, true),
    FORWARD_ALWAYS(0, false),
    FORWARD_DIFFERENT(GL_NOTEQUAL, false),
    FORWARD_CLOSER(GL_LESS, false),
    FORWARD_CLOSE(GL_LEQUAL, false),
    FORWARD_EQUALS(GL_EQUAL, false),
    FORWARD_FAR(GL_GEQUAL, false),
    FORWARD_FARTHER(GL_GREATER, false)
}