package me.anno.tests.gfx

import org.lwjgl.Version
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL11C.glClear
import org.lwjgl.opengl.GL30C.*
import org.lwjgl.opengl.GL45C
import org.lwjgl.opengl.GL46C
import kotlin.reflect.full.staticProperties

fun main() {

    // the reason for the crash was that I was inspecting OpenGLs classes before RenderDoc was loaded
    // this caused the driver to load incorrectly

    val title = "RenderDoc Crash Test"

    val width = 800
    val height = 700

    // still crashes, the easiest case I found
    for (p in GL46C::class.staticProperties) {
        if (p.name.length > 3 &&
            p.name.startsWith("GL_")
        ) {
            val value = p.get()
            if (value is Int) {
                println("${p.name} = $value")
            }
        }
    }

    System.load("C:/Program Files/RenderDoc/renderdoc.dll")

    println("Using LWJGL Version " + Version.getVersion())

    GLFW.glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err))

    check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

    GLFW.glfwDefaultWindowHints()

    val window = GLFW.glfwCreateWindow(width, height, title, 0L, 0L)

    GLFW.glfwSetWindowTitle(window, title)
    GLFW.glfwShowWindow(window)

    GLFW.glfwMakeContextCurrent(window)
    GL.createCapabilities()

    val w = 4
    val h = 4

    val pointer = glGenFramebuffers()
    glBindFramebuffer(GL_FRAMEBUFFER, pointer)
    glDrawBuffer(GL_NONE)
    val renderBuffer = glGenRenderbuffers()
    glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer)
    val format = GL_DEPTH_COMPONENT // application chooses bytes/pixel
    glRenderbufferStorage(GL_RENDERBUFFER, format, w, h)
    glFramebufferRenderbuffer(
        GL_FRAMEBUFFER,
        GL_DEPTH_ATTACHMENT,
        GL_RENDERBUFFER,
        renderBuffer
    )

    val state = GL45C.glCheckNamedFramebufferStatus(pointer, GL_FRAMEBUFFER)
    if (state != GL_FRAMEBUFFER_COMPLETE)
        throw RuntimeException("Framebuffer is incomplete: $state")

    while (true) {

        // press the print key to crash the program

        GLFW.glfwWaitEventsTimeout(1.0 / 240.0)

        GLFW.glfwSwapBuffers(window)

        glBindFramebuffer(GL_FRAMEBUFFER, pointer)
        glClear(GL_DEPTH_BUFFER_BIT)

        Thread.sleep(30)

    }

}