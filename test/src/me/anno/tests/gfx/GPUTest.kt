package me.anno.tests.gfx

import me.anno.gpu.hidden.HiddenOpenGLContext
import org.lwjgl.opengl.GL11C.GL_EXTENSIONS
import org.lwjgl.opengl.GL11C.glGetString

// todo test different things of GPU implementations
// todo test enough that if everything works, UI rendering will work, too

fun main() {
    HiddenOpenGLContext.createOpenGL()
    for (line in glGetString(GL_EXTENSIONS)!!.split(' ')) {
        println(line)
    }
}
