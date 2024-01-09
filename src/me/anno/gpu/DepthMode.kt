package me.anno.gpu

import org.lwjgl.opengl.GL46C

@Suppress("unused")
enum class DepthMode(val id: Int, val reversedDepth: Boolean) {
    // default depth mode is reversed in the engine, because it is superior
    ALWAYS(GL46C.GL_ALWAYS, true),
    DIFFERENT(GL46C.GL_NOTEQUAL, true),
    FARTHER(GL46C.GL_LESS, true),
    FAR(GL46C.GL_LEQUAL, true),
    EQUALS(GL46C.GL_EQUAL, true),
    CLOSE(GL46C.GL_GEQUAL, true),
    CLOSER(GL46C.GL_GREATER, true),
    FORWARD_ALWAYS(GL46C.GL_ALWAYS, false),
    FORWARD_DIFFERENT(GL46C.GL_NOTEQUAL, false),
    FORWARD_CLOSER(GL46C.GL_LESS, false),
    FORWARD_CLOSE(GL46C.GL_LEQUAL, false),
    FORWARD_EQUALS(GL46C.GL_EQUAL, false),
    FORWARD_FAR(GL46C.GL_GEQUAL, false),
    FORWARD_FARTHER(GL46C.GL_GREATER, false)
}