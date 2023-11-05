package me.anno.gpu.buffer

import org.lwjgl.opengl.GL11C

enum class DrawMode(val id: Int, val minLength: Int) {
    POINTS(GL11C.GL_POINTS, 1),
    LINES(GL11C.GL_LINES, 2),
    LINE_STRIP(GL11C.GL_LINE_STRIP, 2),
    TRIANGLES(GL11C.GL_TRIANGLES, 3),
    TRIANGLE_STRIP(GL11C.GL_TRIANGLE_STRIP, 3);
}