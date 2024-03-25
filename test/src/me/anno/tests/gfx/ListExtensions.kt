package me.anno.tests.gfx

import me.anno.jvm.HiddenOpenGLContext
import org.lwjgl.opengl.GL46C.GL_EXTENSIONS
import org.lwjgl.opengl.GL46C.glGetString

fun main() {
    HiddenOpenGLContext.createOpenGL()
    for (line in glGetString(GL_EXTENSIONS)!!.split(' ')) {
        println(line)
    }
}
