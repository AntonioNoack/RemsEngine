package me.anno.jvm

import me.anno.gpu.GFX
import me.anno.gpu.GFXBase
import me.anno.gpu.OSWindow
import me.anno.utils.Clock
import me.anno.utils.assertions.assertTrue
import org.apache.logging.log4j.LogManager
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwSetErrorCallback
import org.lwjgl.glfw.GLFWErrorCallback

/**
 * a class, which allows us to use OpenGL without visible window;
 *
 * mainly used for command line tools, so typically on Desktop only -> moved to JVMExtension
 * */
object HiddenOpenGLContext {

    private val window = OSWindow(HiddenOpenGLContext::class.simpleName.toString())
    private val width get() = window.width
    private val height get() = window.height

    private var errorCallback: GLFWErrorCallback? = null

    private val LOGGER = LogManager.getLogger(HiddenOpenGLContext::class)

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

        val tick = Clock(LOGGER)
        glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err).also { errorCallback = it })
        tick.stop("Error callback")

        assertTrue(glfwInit(), "Unable to initialize GLFW")

        tick.stop("GLFW initialization")

        GFXBase.setWindowFlags()
        // GFXBase.setDebugFlag()

        // should allow OpenGL to run without a window in the background
        // some people on GitHub said it only would use software rendering,
        // so that's of no use to us
        // glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_OSMESA_CONTEXT_API)

        // removes scaling options -> how could we replace them?
        window.pointer = glfwCreateWindow(width, height, "Hidden", 0L, 0L)
        if (window.pointer == 0L) throw RuntimeException("Failed to create the GLFW window")
        GFX.windows.add(window)
        GFX.activeWindow = window

        tick.stop("Create window")

        window.makeCurrent()
        window.forceUpdateVsync()
        GFXBase.prepareForRendering(null)

        GFX.check()

        GFX.setupBasics(tick)
    }
}