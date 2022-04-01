package me.anno.gpu.debug

import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFW
import org.lwjgl.system.MemoryUtil
import org.lwjgl.glfw.Callbacks
import org.lwjgl.opengl.GL
import java.lang.Thread
import org.lwjgl.opengl.GL11C
import kotlin.math.abs
import kotlin.math.sin

/**
 * this is a sample on how multiple windows can use separate OpenGL contexts concurrently
 * Rem's Engine currently doesn't support such things
 * https://github.com/LWJGL/lwjgl3/blob/0db3963c882378faadaa9065a56ba85a40c8f1cb/modules/samples/src/test/java/org/lwjgl/demo/glfw/Threads.java
 */
object ThreadTest {

    private var run = false
    private val titles = arrayOf("Red", "Green", "Blue")
    private val rgb = arrayOf(
        floatArrayOf(1f, 0f, 0f, 0f),
        floatArrayOf(0f, 1f, 0f, 0f),
        floatArrayOf(0f, 0f, 1f, 0f)
    )

    @JvmStatic
    fun main(args: Array<String>) {
        GLFWErrorCallback.createPrint().set()
        check(GLFW.glfwInit()) { "Failed to initialize GLFW." }
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        run = true
        val threads = Array(titles.size){
            val window = GLFW.glfwCreateWindow(200, 200, titles[it], MemoryUtil.NULL, MemoryUtil.NULL)
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

    private class GLFWThread(val window: Long, index: Int) : Thread() {
        val r = rgb[index][0]
        val g = rgb[index][1]
        val b = rgb[index][2]
        override fun run() {
            GLFW.glfwMakeContextCurrent(window)
            GL.createCapabilities()
            GLFW.glfwSwapInterval(1)
            var ctr = 0
            while (run) {
                val v = abs(sin(ctr++ * 3.1416f / 100f))
                GL11C.glClearColor(r * v, g * v, b * v, 0f)
                GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT)
                GLFW.glfwSwapBuffers(window)
            }
        }
    }
}