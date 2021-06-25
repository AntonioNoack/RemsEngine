package me.anno.gpu

import me.anno.input.Input.invalidateLayout
import me.anno.studio.StudioBase.Companion.addEvent
import org.lwjgl.glfw.*
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.system.Callback

class WindowX(title: String, val w: Int, val h: Int) {

    val pointer: Long = glfwCreateWindow(w, h, title, 0, 0)

    var width = w
    var height = h

    var isInFocus = false

    var errorCallback: GLFWErrorCallback? = null
    var keyCallback: GLFWKeyCallback? = null
    var fsCallback: GLFWFramebufferSizeCallback? = null
    var debugProc: Callback? = null

    init {
        if (pointer == 0L) throw IllegalStateException("Window could not be created")
    }

    var title: String = title
        set(value) {
            field = value
            // todo update the window
        }

    fun setListeners() {
        GLFW.glfwSetKeyCallback(pointer, object : GLFWKeyCallback() {
            override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) GLFW.glfwSetWindowShouldClose(
                    window,
                    true
                )
            }
        }.also { keyCallback = it })
        GLFW.glfwSetFramebufferSizeCallback(pointer, object : GLFWFramebufferSizeCallback() {
            override fun invoke(window: Long, w: Int, h: Int) {
                if (w > 0 && h > 0 && (w != width || h != height)) {
                    addEvent {
                        width = w
                        height = h
                        invalidateLayout()
                    }
                }
            }
        }.also { fsCallback = it })
        GLFW.glfwSetWindowFocusCallback(pointer, object : GLFWWindowFocusCallback() {
            override fun invoke(window: Long, isInFocus0: Boolean) {
                isInFocus = isInFocus0
            }
        })
    }

}