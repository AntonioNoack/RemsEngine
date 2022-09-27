/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package me.anno.gpu

import me.anno.Build.isDebug
import me.anno.Engine
import me.anno.Engine.projectName
import me.anno.Engine.shutdown
import me.anno.config.DefaultConfig
import me.anno.gpu.GFX.addGPUTask
import me.anno.gpu.GFX.checkIsGFXThread
import me.anno.gpu.GFX.focusedWindow
import me.anno.gpu.GFX.getErrorTypeName
import me.anno.gpu.debug.LWJGLDebugCallback
import me.anno.gpu.debug.OpenGLDebug.getDebugSeverityName
import me.anno.gpu.debug.OpenGLDebug.getDebugSourceName
import me.anno.gpu.debug.OpenGLDebug.getDebugTypeName
import me.anno.image.Image
import me.anno.image.ImageCPUCache
import me.anno.input.Input
import me.anno.input.Input.isMouseTrapped
import me.anno.input.Input.trapMouseWindow
import me.anno.io.files.BundledRef
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.language.translation.NameDesc
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.input.InputPanel
import me.anno.utils.Clock
import me.anno.utils.structures.lists.Lists.all2
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.Lists.none2
import org.apache.logging.log4j.LogManager.getLogger
import org.lwjgl.BufferUtils
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.glfwGetError
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL11C.glEnable
import org.lwjgl.opengl.GL11C.glGetError
import org.lwjgl.opengl.GL43C.glDebugMessageCallback
import org.lwjgl.system.Callback
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import sun.plugin.dom.exception.InvalidStateException
import java.awt.AWTException
import java.awt.Robot
import kotlin.math.abs
import kotlin.math.max

/**
 * Showcases how you can use multithreading in a GLFW application
 * to separate the (blocking) winproc handling from the render loop.
 *
 * @author Kai Burjack
 *
 * modified by Antonio Noack
 * including all os natives has luckily only very few overhead :) (&lt; 1 MiB)
 *
 * todo rewrite this such that we can have multiple windows, which may be nice for the color picker, and maybe other stuff,
 * todo e.g. having multiple editor windows
 *
 * todo rebuild and recompile the glfw driver, which handles the touch input, so the input can be assigned to the window
 * (e.g. add 1 to the pointer)
 */
object GFXBase {

    private val LOGGER = getLogger(GFXBase::class)
    private val windows get() = GFX.windows

    private var debugMsgCallback: Callback? = null
    private var errorCallback: GLFWErrorCallback? = null

    val glfwLock = Any()
    val openglLock = Any()
    var destroyed = false

    var capabilities: GLCapabilities? = null
    var robot: Robot? = null

    /** must be executed before OpenGL-init;
     * must be disabled for Nvidia Nsight */
    private var disableRenderDoc = false
    fun disableRenderDoc() {
        disableRenderDoc = true
    }

    fun loadRenderDoc() {
        val enabled = DefaultConfig["debug.renderdoc.enabled", isDebug]
        if (enabled && !disableRenderDoc) {
            forceLoadRenderDoc(null)
        }
    }

    fun forceLoadRenderDoc(renderDocPath: String? = null) {
        val path = renderDocPath ?: DefaultConfig["debug.renderdoc.path", "C:/Program Files/RenderDoc/renderdoc.dll"]
        try {
            // if renderdoc is installed on linux, or given in the path, we could use it as well with loadLibrary()
            // at least this is the default location for RenderDoc
            if (getReference(path).exists) {
                LOGGER.info("Loading RenderDoc")
                System.load(path)
            } else LOGGER.warn("Did not find RenderDoc, searched '$path'")
        } catch (e: Exception) {
            LOGGER.warn("Could not initialize RenderDoc")
            e.printStackTrace()
        }
    }

    fun run() {
        try {

            loadRenderDoc()
            val clock = init()

            val window0 = createWindow(WindowX(projectName), clock)

            try {
                robot = Robot()
            } catch (e: AWTException) {
                e.printStackTrace()
            }

            windowLoop(window0)

            // wait for the last frame to be finished,
            // before we actually destroy the window and its framebuffer
            synchronized(glfwLock) {
                synchronized(openglLock) {
                    destroyed = true
                    when (windows.size) {
                        0 -> {}
                        1 -> LOGGER.info("Closing one remaining window")
                        else -> LOGGER.info("Closing ${windows.size} remaining windows")
                    }
                    for (index in 0 until windows.size) {
                        close(windows.getOrNull(index) ?: break)
                    }
                    windows.clear()
                }
            }
            if (debugMsgCallback != null) debugMsgCallback!!.free()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            GLFW.glfwTerminate()
            errorCallback!!.free()
        }
    }

    fun init(): Clock {
        LOGGER.info("Using LWJGL Version " + Version.getVersion())
        val tick = Clock()
        GLFW.glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err).also { errorCallback = it })
        tick.stop("Error callback")
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }
        tick.stop("GLFW initialization")
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)
        if (isDebug) {
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE)
        }
        // removes scaling options -> how could we replace them?
        // glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
        // tick.stop("window hints");// 0s
        return tick
    }

    fun addCallbacks(window: WindowX) {
        window.addCallbacks()
        Input.initForGLFW(window)
    }

    @Suppress("unused")
    fun createWindow(title: String, panel: Panel): WindowX {
        val window = WindowX(title)
        createWindow(window, null)
        window.windowStack.push(panel)
        return window
    }

    fun createWindow(instance: WindowX, tick: Clock?): WindowX {
        synchronized(glfwLock) {
            val width = instance.width
            val height = instance.height
            val sharedWindow = windows.firstOrNull { it.pointer != 0L }?.pointer ?: 0L
            val window = GLFW.glfwCreateWindow(width, height, projectName, 0L, sharedWindow)
            instance.pointer = window
            if (window == 0L) throw RuntimeException("Failed to create the GLFW window")
            windows.add(instance)
            tick?.stop("Create window")
            addCallbacks(instance)
            tick?.stop("Adding callbacks")
            val videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
            if (videoMode != null) GLFW.glfwSetWindowPos(
                window,
                (videoMode.width() - width) / 2,
                (videoMode.height() - height) / 2
            )
            MemoryStack.stackPush().use { frame ->
                val framebufferSize = frame.mallocInt(2)
                GLFW.nglfwGetFramebufferSize(
                    window,
                    MemoryUtil.memAddress(framebufferSize),
                    MemoryUtil.memAddress(framebufferSize) + 4
                )
                instance.width = framebufferSize[0]
                instance.height = framebufferSize[1]
            }
            tick?.stop("Window position")
            GLFW.glfwSetWindowTitle(window, instance.title)

            // tick.stop("window title"); // 0s
            GLFW.glfwShowWindow(window)
            tick?.stop("Show window")
            setIcon(window)
            tick?.stop("Setting icon")
            val x = DoubleArray(1)
            val y = DoubleArray(1)
            GLFW.glfwGetCursorPos(window, x, y)
            instance.mouseX = x[0].toFloat()
            instance.mouseY = y[0].toFloat()
        }
        return instance
    }

    private fun makeCurrent(window: WindowX): Boolean {
        GFX.activeWindow = window
        if (window.pointer != lastCurrent && window.pointer != 0L) {
            lastCurrent = window.pointer
            GLFW.glfwMakeContextCurrent(window.pointer)
        }
        return window.pointer != 0L
    }

    private var neverStarveWindows = DefaultConfig["ux.neverStarveWindows", false]

    private var lastCurrent = 0L
    fun runRenderLoop(window0: WindowX) {
        LOGGER.info("Running RenderLoop")
        val tick = Clock()
        makeCurrent(window0)
        window0.forceUpdateVsync()
        tick.stop("Make context current + vsync")
        capabilities = GL.createCapabilities()
        tick.stop("OpenGL initialization")
        debugMsgCallback = GLUtil.setupDebugMessageCallback(LWJGLDebugCallback)
        tick.stop("Debugging Setup")
        // render first frames = render logo
        // the engine will still be loading,
        // so it has to be a still image
        // alternatively we could play a small animation
        GFX.maxSamples = max(1, GL30C.glGetInteger(GL30C.GL_MAX_SAMPLES))
        GFX.check()
        val zeroFrames = 2
        for (i in 0 until zeroFrames) {
            drawLogo(window0, i == zeroFrames - 1)
            GFX.check()
            GLFW.glfwSwapBuffers(window0.pointer)
            val err = glGetError()
            if (err != 0)
                LOGGER.warn("Got awkward OpenGL error from calling glfwSwapBuffers: ${getErrorTypeName(err)}")
            // GFX.check()
        }
        tick.stop("Render frame zero")
        renderStep0()
        GFX.check()
        tick.stop("Render step zero")
        StudioBase.instance?.gameInit()
        tick.stop("Game Init")
        var lastTime = System.nanoTime()
        while (!destroyed && !shutdown) {

            Engine.updateTime()

            val time = Engine.nanoTime

            val firstWindow = windows.firstOrNull()
            if (firstWindow != null) Input.pollControllers(firstWindow)

            for (index in 0 until windows.size) {
                val window = windows.getOrNull(index) ?: break
                if (window.isInFocus ||
                    window.hasActiveMouseTargets() ||
                    neverStarveWindows ||
                    abs(window.lastUpdate - time) * GFX.idleFPS > 1e9
                ) {
                    window.lastUpdate = time
                    // this is hopefully ok (calling it async to other glfw stuff)
                    if (makeCurrent(window)) {
                        synchronized(openglLock) {
                            GFX.activeWindow = window
                            renderStep(window)
                        }
                        synchronized(glfwLock) {
                            if (!destroyed) {
                                GLFW.glfwWaitEventsTimeout(0.0)
                                GLFW.glfwSwapBuffers(window.pointer)
                                window.updateVsync()
                            }
                        }
                    }
                }
            }

            for (index in 0 until windows.size) {
                val window = windows.getOrNull(index) ?: break
                if (window.shouldClose) close(window)
            }

            if (windows.isNotEmpty() &&
                windows.none2 { (it.isInFocus && !it.isMinimized) || it.hasActiveMouseTargets() }
            ) {
                // enforce 30 fps, because we don't need more
                // and don't want to waste energy
                val currentTime = System.nanoTime()
                val waitingTime = GFX.idleFPS - (currentTime - lastTime) / 1000000
                lastTime = currentTime
                if (waitingTime > 0) try {
                    // wait does not work, causes IllegalMonitorState exception
                    Thread.sleep(waitingTime)
                } catch (ignored: InterruptedException) {
                }
            }
        }
        StudioBase.instance?.onShutdown()
    }

    fun renderStep0() {
        if (isDebug) {
            // System.loadLibrary("renderdoc");
            glDebugMessageCallback({ source: Int, type: Int, id: Int, severity: Int, _: Int, message: Long, _: Long ->
                val message2 = if (message != 0L) MemoryUtil.memUTF8(message) else null
                LOGGER.warn(
                    message2 +
                            ", source: " + getDebugSourceName(source) +
                            ", type: " + getDebugTypeName(type) + // mmh, not correct, at least for my simple sample I got a non-mapped code
                            ", id: " + getErrorTypeName(id) +
                            ", severity: " + getDebugSeverityName(severity)
                )
            }, 0)
            glEnable(KHRDebug.GL_DEBUG_OUTPUT)
        }
        checkIsGFXThread()
        GFX.renderStep0()
    }

    fun renderStep(window: WindowX) {
        GFX.renderStep(window)
    }

    fun close(window: WindowX) {
        synchronized(glfwLock) {
            if (window.pointer != 0L) {
                GLFW.glfwDestroyWindow(window.pointer)
                window.keyCallback?.free()
                window.keyCallback = null
                window.fsCallback?.free()
                window.fsCallback = null
                window.pointer = 0L
            }
        }
    }

    fun windowLoop(window0: WindowX) {

        Thread.currentThread().name = "GLFW"

        // Start new thread to have the OpenGL context current in and that does the rendering.
        Thread {
            runRenderLoop(window0)
            cleanUp()
        }.start()

        var lastTrapWindow: WindowX? = null

        while (!windows.all2 { it.shouldClose } && !shutdown) {
            for (index in 0 until windows.size) {
                val window = windows[index]
                if (!window.shouldClose) {
                    if (GLFW.glfwWindowShouldClose(window.pointer)) {
                        val ws = window.windowStack
                        if (ws.isEmpty() ||
                            DefaultConfig["window.close.directly", false] ||
                            ws.peek().isClosingQuestion
                        ) {
                            window.shouldClose = true
                            GLFW.glfwSetWindowShouldClose(window.pointer, true)
                        } else {
                            GLFW.glfwSetWindowShouldClose(window.pointer, false)
                            addGPUTask("close-request", 1) {
                                ask(
                                    ws, NameDesc("Close %1?", "", "ui.closeProgram")
                                        .with("%1", projectName)
                                ) {
                                    window.shouldClose = true
                                    GLFW.glfwSetWindowShouldClose(window.pointer, true)
                                }?.isClosingQuestion = true
                                window.framesSinceLastInteraction = 0
                                ws.peek().setAcceptsClickAway(false)
                            }
                        }
                    } else {
                        // update small stuff, that may need to be updated;
                        // currently only the title
                        window.updateTitle()
                    }
                }
            }

            val trapWindow = trapMouseWindow
            if (isMouseTrapped && trapWindow != null && !trapWindow.shouldClose) {
                if (lastTrapWindow == null) {
                    GLFW.glfwSetInputMode(trapWindow.pointer, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)
                    // GLFW.glfwSetInputMode(trapWindow.pointer, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN)
                    lastTrapWindow = trapWindow
                }
                /*val x = trapWindow.mouseX
                val y = trapWindow.mouseY
                val centerX = trapWindow.width * 0.5
                val centerY = trapWindow.height * 0.5
                val dx = x - centerX
                val dy = y - centerY
                if (dx * dx + dy * dy > trapMouseRadius * trapMouseRadius) {
                    GLFW.glfwSetCursorPos(trapWindow.pointer, centerX, centerY)
                }*/
            } else if (lastTrapWindow != null && !lastTrapWindow.shouldClose) {
                GLFW.glfwSetInputMode(lastTrapWindow.pointer, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL)
                lastTrapWindow = null
            } else if (Input.mouseMovementSinceMouseDown > 5f && Input.mouseKeysDown.isNotEmpty() && DefaultConfig["ui.enableMouseJumping", true]) {
                // when dragging a value (dragging + selected.isInput), and cursor is on the border, respawn it in the middle of the screen
                // for that, the cursor should be within 2 frames of reaching the border...
                // for that, we need the last mouse movement :)
                val window = focusedWindow
                if (window != null) {
                    val margin = 10f
                    if (window.mouseX !in margin..window.width - margin || window.mouseY !in margin..window.height - margin) {
                        val inFocus = window.windowStack.inFocus
                        if (inFocus.any2 { p -> p.anyInHierarchy { h -> h is InputPanel<*> } }) {
                            val centerX = window.width * 0.5
                            val centerY = window.height * 0.5
                            GLFW.glfwSetCursorPos(window.pointer, centerX, centerY)
                            window.mouseX = centerX.toFloat()
                            window.mouseY = centerY.toFloat()
                        }
                    }
                }
            }

            for (index in windows.indices) {
                val window = windows[index]
                if (!window.shouldClose && window.updateMouseTarget()) break
            }

            // glfwWaitEventsTimeout() without args only terminates, if keyboard or mouse state is changed
            GLFW.glfwWaitEventsTimeout(0.0)

        }

    }

    fun cleanUp() {}

    fun setIcon(window: Long) {
        val src = getReference(BundledRef.prefix + "icon.png")
        val srcImage = ImageCPUCache.getImage(src, false)
        if (srcImage != null) {
            setIcon(window, srcImage)
        }
    }

    fun setIcon(window: Long, srcImage: Image) {

        val image = GLFWImage.malloc()
        val buffer = GLFWImage.malloc(1)

        val w = srcImage.width
        val h = srcImage.height
        val pixels = BufferUtils.createByteBuffer(w * h * 4)
        for (y in 0 until h) {
            for (x in 0 until w) {
                // argb -> rgba
                val color = srcImage.getRGB(x, y)
                pixels.put(color.shr(16).toByte())
                pixels.put(color.shr(8).toByte())
                pixels.put(color.toByte())
                pixels.put(color.shr(24).toByte())
            }
        }
        pixels.flip()
        image.set(w, h, pixels)
        buffer.put(0, image)
        GLFW.glfwSetWindowIcon(window, buffer)
    }


}