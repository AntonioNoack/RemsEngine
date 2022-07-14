package me.anno.tests

import me.anno.maths.Maths.PIf
import org.joml.Vector4f
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C
import org.lwjgl.system.MemoryUtil
import kotlin.math.abs
import kotlin.math.sin

/**
 * this is a sample on how multiple windows can use separate OpenGL contexts concurrently
 * Rem's Engine currently doesn't support such things
 * https://github.com/LWJGL/lwjgl3/blob/0db3963c882378faadaa9065a56ba85a40c8f1cb/modules/samples/src/test/java/org/lwjgl/demo/glfw/Threads.java
 */
fun main() {

    var run = false
    val titles = arrayOf("Red", "Green", "Blue")
    class GLFWThread(val window: Long, val index: Int) : Thread() {
        override fun run() {
            GLFW.glfwMakeContextCurrent(window)
            GL.createCapabilities()
            GLFW.glfwSwapInterval(1)
            var ctr = 0
            val color = Vector4f()
            while (run) {
                val v = abs(sin(ctr++ * PIf / 100f))
                color.setComponent(index, v)
                GL11C.glClearColor(color.x, color.y, color.z, color.w)
                GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT)
                GLFW.glfwSwapBuffers(window)
            }
        }
    }

    GLFWErrorCallback.createPrint().set()
    check(GLFW.glfwInit()) { "Failed to initialize GLFW." }
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
    run = true
    val threads = Array(titles.size) {
        val window = GLFW.glfwCreateWindow(200, 200, titles[it], 0L, 0L)
        check(window != MemoryUtil.NULL) { "Failed to create GLFW window." }
        GLFW.glfwSetKeyCallback(window) { windowHnd: Long, key: Int, _: Int, action: Int, _: Int ->
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
                GLFW.glfwSetWindowShouldClose(windowHnd, true)
            }
        }
        GLFW.glfwSetWindowPos(window, 200 + 250 * it, 200)
        GLFW.glfwShowWindow(window)
        val thread = GLFWThread(window, it)
        thread.start()
        thread
    }

    out@ while (true) {
        GLFW.glfwWaitEvents()
        for (i in titles.indices) {
            if (GLFW.glfwWindowShouldClose(threads[i].window)) {
                run = false
                break@out
            }
        }
    }

    for (glfwThread in threads) {
        glfwThread.join()
    }

    for (thread in threads) {
        Callbacks.glfwFreeCallbacks(thread.window)
        GLFW.glfwDestroyWindow(thread.window)
    }

    GLFW.glfwTerminate()
    GLFW.glfwSetErrorCallback(null)!!.free()

}
