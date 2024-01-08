package me.anno.gpu

import org.lwjgl.opengl.GL11C

@Suppress("unused")
enum class DepthMode(val id: Int, val reversedDepth: Boolean) {
    // default depth mode is reversed in the engine, because it is superior
    ALWAYS(GL11C.GL_ALWAYS, true),
    DIFFERENT(GL11C.GL_NOTEQUAL, true),
    FARTHER(GL11C.GL_LESS, true),
    FAR(GL11C.GL_LEQUAL, true),
    EQUALS(GL11C.GL_EQUAL, true),
    CLOSE(GL11C.GL_GEQUAL, true),
    CLOSER(GL11C.GL_GREATER, true),
    FORWARD_ALWAYS(GL11C.GL_ALWAYS, false),
    FORWARD_DIFFERENT(GL11C.GL_NOTEQUAL, false),
    FORWARD_CLOSER(GL11C.GL_LESS, false),
    FORWARD_CLOSE(GL11C.GL_LEQUAL, false),
    FORWARD_EQUALS(GL11C.GL_EQUAL, false),
    FORWARD_FAR(GL11C.GL_GEQUAL, false),
    FORWARD_FARTHER(GL11C.GL_GREATER, false)
}