package me.anno.gpu.buffer

import org.lwjgl.opengl.GL46C

enum class DrawMode(val id: Int, val minLength: Int, val primitiveSize: Int) {
    POINTS(GL46C.GL_POINTS, 1, 1),
    LINES(GL46C.GL_LINES, 2, 2),
    LINE_STRIP(GL46C.GL_LINE_STRIP, 2, 2),
    TRIANGLES(GL46C.GL_TRIANGLES, 3, 3),
    TRIANGLE_STRIP(GL46C.GL_TRIANGLE_STRIP, 3, 3);
}