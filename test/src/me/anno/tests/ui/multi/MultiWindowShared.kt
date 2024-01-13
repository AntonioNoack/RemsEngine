package me.anno.tests.ui.multi

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.OSWindow
import org.joml.Vector4f
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryUtil
import kotlin.math.abs
import kotlin.math.sin

fun main() {

    val titles = arrayOf("Red", "Green", "Blue")

    var frameIndex = 0
    val color = Vector4f()

    GFX.activeWindow = OSWindow(titles[0])

    GLFWErrorCallback.createPrint().set()
    check(GLFW.glfwInit()) { "Failed to initialize GLFW." }
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
    var w0 = 0L
    val windows = LongArray(titles.size) {
        val window = GLFW.glfwCreateWindow(200, 200, titles[it], 0L, w0)
        if (it == 0) w0 = window
        check(window != MemoryUtil.NULL) { "Failed to create GLFW window." }
        GLFW.glfwSetKeyCallback(window) { windowHnd: Long, key: Int, _: Int, action: Int, _: Int ->
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
                GLFW.glfwSetWindowShouldClose(windowHnd, true)
            }
        }
        GLFW.glfwSetWindowPos(window, 200 + 250 * it, 200)
        GLFW.glfwShowWindow(window)
        window
    }

    GLFW.glfwMakeContextCurrent(windows[0])
    GL.createCapabilities()
    GLFW.glfwSwapInterval(1) // per context, so it's fine here

    out@ while (true) {
        GLFW.glfwWaitEventsTimeout(0.0)
        for (i in windows.indices) {
            val window = windows[i]
            if (GLFW.glfwWindowShouldClose(window)) break@out
            else {
                color.set(0f)
                GLFW.glfwMakeContextCurrent(window)
                val v = abs(sin(frameIndex / 30f))
                color[i] = v
                GFXState.currentBuffer.clearColor(color)
                GLFW.glfwSwapBuffers(window)
            }
        }
        frameIndex++
    }

    for (window in windows) {
        Callbacks.glfwFreeCallbacks(window)
        GLFW.glfwDestroyWindow(window)
    }

    GLFW.glfwTerminate()
    GLFW.glfwSetErrorCallback(null)!!.free()

}
