package me.anno.gpu

import org.lwjgl.opengl.GL11C.*

@Suppress("unused")
enum class DepthMode(val id: Int, val reversedDepth: Boolean) {
    // default depth mode is reversed in the engine, because it is superior
    ALWAYS(GL_ALWAYS, true),
    DIFFERENT(GL_NOTEQUAL, true),
    FARTHER(GL_LESS, true),
    FAR(GL_LEQUAL, true),
    EQUALS(GL_EQUAL, true),
    CLOSE(GL_GEQUAL, true),
    CLOSER(GL_GREATER, true),
    FORWARD_ALWAYS(GL_ALWAYS, false),
    FORWARD_DIFFERENT(GL_NOTEQUAL, false),
    FORWARD_CLOSER(GL_LESS, false),
    FORWARD_CLOSE(GL_LEQUAL, false),
    FORWARD_EQUALS(GL_EQUAL, false),
    FORWARD_FAR(GL_GEQUAL, false),
    FORWARD_FARTHER(GL_GREATER, false)
}