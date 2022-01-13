package me.anno.gpu.pipeline

import org.lwjgl.Version
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL30C.glBindFramebuffer
import org.lwjgl.opengl.GL45C
import org.lwjgl.opengl.GL46
import org.lwjgl.system.MemoryUtil
import kotlin.reflect.full.staticProperties

fun main() {

    // the reason for the crash was that I was inspecting OpenGLs classes before RenderDoc was loaded
    // this caused the driver to load incorrectly

    val title = "RenderDoc Crash Test"

    val width = 800
    val height = 700

    // still crashes, the easiest case I found
    for (p in GL46::class.staticProperties) {
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

    val window = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL)

    GLFW.glfwSetWindowTitle(window, title)
    GLFW.glfwShowWindow(window)

    GLFW.glfwMakeContextCurrent(window)
    GL.createCapabilities()

    val w = 4
    val h = 4

    val pointer = GL30C.glGenFramebuffers()
    glBindFramebuffer(GL30C.GL_FRAMEBUFFER, pointer)
    GL30C.glDrawBuffer(GL30C.GL_NONE)
    val renderBuffer = GL30C.glGenRenderbuffers()
    GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, renderBuffer)
    val format = GL30C.GL_DEPTH_COMPONENT // application chooses bytes/pixel
    GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, format, w, h)
    GL30C.glFramebufferRenderbuffer(
        GL30C.GL_FRAMEBUFFER,
        GL30C.GL_DEPTH_ATTACHMENT,
        GL30C.GL_RENDERBUFFER,
        renderBuffer
    )

    val state = GL45C.glCheckNamedFramebufferStatus(pointer, GL30C.GL_FRAMEBUFFER)
    if (state != GL30C.GL_FRAMEBUFFER_COMPLETE)
        throw RuntimeException("Framebuffer is incomplete: $state")

    while (true) {

        // press the print key to crash the program

        GLFW.glfwWaitEventsTimeout(1.0 / 240.0)

        GLFW.glfwSwapBuffers(window)

        glBindFramebuffer(GL30C.GL_FRAMEBUFFER, pointer)
        glClear(GL_DEPTH_BUFFER_BIT)

        Thread.sleep(30)

    }

}