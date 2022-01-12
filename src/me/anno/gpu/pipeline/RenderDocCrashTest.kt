package me.anno.gpu.pipeline

import me.anno.config.DefaultConfig
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL30C.glBindFramebuffer
import org.lwjgl.system.MemoryUtil

fun main() {

    // the reason for the crash was that I was inspecting OpenGLs classes before RenderDoc was loaded
    // this caused the driver to load incorrectly

    val title = "RenderDoc Crash Test"

    val width = 800
    val height = 700

    // still crashes, the easiest case I found

    // if the default config is not accessed, then renderdoc does not crash!!! WTF!!!
    DefaultConfig["debug.renderdoc.path", "C:/Program Files/RenderDoc/renderdoc.dll"]
    System.load("C:/Program Files/RenderDoc/renderdoc.dll")

    println("Using LWJGL Version " + Version.getVersion())

    GLFW.glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err))

    check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

    GLFW.glfwDefaultWindowHints()
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE)

    val window = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL)

    GLFW.glfwSetWindowTitle(window, title)
    GLFW.glfwShowWindow(window)

    GLFW.glfwMakeContextCurrent(window)
    GL.createCapabilities()

    while (true) {

        Thread.sleep(500)

        GLFW.glfwSwapBuffers(window)

        val w = 4
        val h = 4

        val buffer = Framebuffer(
            "x", w, h,
            1, 0,
            false, // doesn't matter
            DepthBufferType.INTERNAL // doesn't matter (texture/none/internal)
        )

        val pointer = GL30C.glGenFramebuffers()
        Framebuffer.bindFramebuffer(GL30C.GL_FRAMEBUFFER, pointer)
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
        buffer.check(pointer)

        glBindFramebuffer(GL30C.GL_FRAMEBUFFER, pointer)
        glClear(GL_DEPTH_BUFFER_BIT)

    }

    /*class TestStudio :
        StudioBase(false, "RenderDoc Crash Test", 1) {

        override fun createUI() {}

        override fun onGameLoop(w: Int, h: Int) {

            val w = 4
            val h = 4

            val buffer = Framebuffer(
                "x", w, h,
                1, 0,
                false, // doesn't matter
                DepthBufferType.INTERNAL // doesn't matter (texture/none/internal)
            )

            val pointer = GL30C.glGenFramebuffers()
            Framebuffer.bindFramebuffer(GL30C.GL_FRAMEBUFFER, pointer)
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
            buffer.check(pointer)

            glBindFramebuffer(GL30C.GL_FRAMEBUFFER, pointer)
            glClear(GL_DEPTH_BUFFER_BIT)
            glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0)

            Thread.sleep(500)

        }
    }

    TestStudio().run()*/

}