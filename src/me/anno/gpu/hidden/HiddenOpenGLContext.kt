package me.anno.gpu.hidden

import me.anno.Engine.projectName
import me.anno.gpu.GFX
import me.anno.gpu.WindowX
import me.anno.utils.Clock
import org.apache.logging.log4j.LogManager
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.system.MemoryUtil

/**
 * a class, which allows us to use OpenGL without visible window
 * */
object HiddenOpenGLContext {

    private val window = WindowX(HiddenOpenGLContext::class.simpleName.toString())
    private val width get() = window.width
    private val height get() = window.height

    private var errorCallback: GLFWErrorCallback? = null

    private val LOGGER = LogManager.getLogger(HiddenOpenGLContext::class)

    var capabilities: GLCapabilities? = null

    fun setSize(w: Int, h: Int = w) {
        window.width = w
        window.height = h
    }

    fun createOpenGL(w: Int, h: Int = w) {
        setSize(w, h)
        createOpenGL()
    }

    fun createOpenGL() {

        LOGGER.info("Using LWJGL Version " + Version.getVersion())

        val tick = Clock()
        glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err).also { errorCallback = it })
        tick.stop("Error callback")

        check(glfwInit()) { "Unable to initialize GLFW" }

        tick.stop("GLFW initialization")

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_DECORATED, GLFW_FALSE)
        // should allow OpenGL to run without a window in the background
        // some people on Github said it only would use software rendering,
        // so that's of no use to us
        // glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_OSMESA_CONTEXT_API)

        // removes scaling options -> how could we replace them?
        window.pointer = glfwCreateWindow(width, height, projectName, 0L, 0L)
        if (window.pointer == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")
        GFX.windows.add(window)
        GFX.activeWindow = window

        tick.stop("Create window")

        glfwMakeContextCurrent(window.pointer)
        glfwSwapInterval(1)
        capabilities = GL.createCapabilities()

        GFX.check()

    }

}